package com.bbvideoeditor.app.media

import com.bbvideoeditor.core.MediaReference
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/** Worker-only evidence preparation. No Player/project semantics and no work on transport ticks. */
class MediaCompatibilityService(private val backend: CompatibilityBackend,
    private val now: () -> Long = { System.nanoTime()/1_000_000 }, private val probeTimeoutMs:Long = 5_000) : AutoCloseable {
    private data class Cached(val at: Long, val value: ProbeEvidence)
    private val results = object : LinkedHashMap<String, Cached>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>?) = size > 64
    }
    private val failures = object : LinkedHashMap<String, Long>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 128
    }
    private val observations = object : LinkedHashMap<String, ProbeEvidence>(64,.75f,true) {
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<String,ProbeEvidence>?)=size>64
    }
    fun observations(evidence:SourceEvidence):ProbeEvidence?=synchronized(lock){observations[key(evidence)]}
    private val pages = mutableMapOf<String, Int>()
    private val lock = Any()
    private val activeOperations = mutableSetOf<CompletableJob>()
    private var closed = false
    private val serial = AtomicLong()
    private val probes = PriorityWorkQueue<ProbeEvidence>(1, 24)
    val inspections = AtomicLong()
    val probeRuns = AtomicLong()
    suspend fun inspect(media: MediaReference): SourceEvidence = withContext(Dispatchers.IO) {
        inspections.incrementAndGet(); backend.inspect(media)
    }
    fun key(evidence: SourceEvidence) = evidence.format.fingerprint(backend.deviceBackend)
    fun failed(evidence: SourceEvidence): Boolean = synchronized(lock) {
        failures[key(evidence)]?.let { now() - it < 60_000 } == true
    }
    fun recordFailure(evidence: SourceEvidence) { synchronized(lock) { failures[key(evidence)] = now(); results.remove(key(evidence)) } }
    fun invalidate(evidence: SourceEvidence) { synchronized(lock) { results.remove(key(evidence)); failures.remove(key(evidence)) } }
    fun queueMetrics() = probes.metrics()
    suspend fun verify(evidence: SourceEvidence, priority: AssetPriority = AssetPriority.VISIBLE): ProbeEvidence {
        val key = key(evidence)
        synchronized(lock) { results[key]?.takeIf { now() - it.at < 5 * 60_000 }?.let { return it.value } }
        val caller = currentCoroutineContext()
        // Each generation owns its operation. A cancelled old waiter cannot cancel a new generation's probe.
        val result = probes.submit("$key:${serial.incrementAndGet()}", priority) {
            val operation=Job(caller[Job])
            synchronized(lock){
                if(closed){operation.cancel();throw CancellationException("Compatibility service is closed")}
                activeOperations+=operation
            }
            try { withContext(caller + operation + Dispatchers.IO) {
                probeRuns.incrementAndGet()
                val start = synchronized(lock) { pages[key] ?: 0 }
                val candidates = evidence.candidates.drop(start).take(3)
                val observations = mutableListOf<DecoderObservation>()
                for (candidate in candidates) {
                    ensureActive()
                    val observed = try { withTimeout(probeTimeoutMs) { backend.probe(evidence.format, candidate) } }
                    catch (timeout: TimeoutCancellationException) { DecoderObservation(candidate.name, ProbeStatus.TIMEOUT, cause = timeout) }
                    observations += observed
                    if (observed.status == ProbeStatus.OUTPUT || observed.status == ProbeStatus.RESOURCE_LIMIT) break
                }
                val value = ProbeEvidence(observations, start + observations.size >= evidence.candidates.size)
                synchronized(lock) {
                    this@MediaCompatibilityService.observations[key]=value
                    if (value.success != null) results[key] = Cached(now(), value)
                    else {
                        if (pages.size >= 64) pages.clear()
                        // A timeout/resource/access result is not evidence that these candidates
                        // are impossible. Revisit them instead of skipping to a later page and
                        // incorrectly reporting that every candidate has deterministically failed.
                        pages[key] = if (value.exhausted || value.transient) 0 else start + observations.size
                    }
                }
                value
            } } finally { operation.complete();synchronized(lock){activeOperations-=operation} }
        }
        return result ?: ProbeEvidence(listOf(DecoderObservation("queue", ProbeStatus.RESOURCE_LIMIT, detail = "Compatibility queue is busy")), false)
    }
    override fun close() { synchronized(lock){closed=true;activeOperations.forEach{it.cancel()}};probes.close() }
}
