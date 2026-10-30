package com.bbvideoeditor.app.playback

import kotlin.math.abs

data class PreviewRequest(val id: Long, val revision: Long, val positionUs: Long)

/** Main-confined admission state. At most one native request plus one replaceable pending target.
 * Transport intent is owned independently by PreviewEngine, never stored in a scrub request. */
internal class PreviewRequests {
    private var serial = 0L
    var pending: PreviewRequest? = null; private set
    var inFlight: PreviewRequest? = null; private set
    var coalesced = 0L; private set
    fun offer(positionUs: Long, revision: Long): PreviewRequest {
        if (pending != null) coalesced++
        return PreviewRequest(++serial, revision, positionUs).also { pending = it }
    }
    fun take(revision: Long): PreviewRequest? {
        if (inFlight != null) return null
        val next = pending ?: return null
        pending = null
        if (next.revision != revision) return null
        inFlight = next
        return next
    }
    fun submitted(revision: Long, timeUs: Long, toleranceUs: Long): PreviewRequest? {
        val request = inFlight ?: return null
        if (request.revision != revision || abs(request.positionUs - timeUs) > toleranceUs) return null
        inFlight = null
        return request
    }
    fun abandon(): PreviewRequest? = inFlight.also { inFlight = null }
    fun clear() { pending = null; inFlight = null }
    val busy get() = pending != null || inFlight != null
}

data class PreviewMetrics(
    val playerCreates: Long, val playerReleases: Long, val compositionInstalls: Long, val prepares: Long,
    val nativeSeeks: Long, val graphRebuilds: Long, val decoderInitializations: Long,
    val requests: Long, val coalesced: Long, val submittedFrames: Long, val bufferingEvents: Long,
    val parameterUpdates: Long, val recoveries: Long, val maximumSeekOfferNs: Long,
    val nativeDecoderReleases:Long, val nativeSeekP95Ns:Long, val maximumNativeSeekNs:Long,
    val nativeRedraws:Long, val maximumInstallNs:Long,
)
data class EngineEvent(val kind: String, val revision: Long, val requestId: Long = 0, val timeUs: Long = 0, val sourceMediaId:String? = null)

/** Bounded in-memory engineering telemetry. No paths, media names or production log spam. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PreviewDiagnostics {
    // CompositionPlayer forwards onVideoEnabled counters for all sequence renderers, but does
    // not forward every decoder-initialized callback. Keep the actual live counters instead.
    private val counters=java.util.ArrayDeque<androidx.media3.exoplayer.DecoderCounters>()
    private var retiredInitializations=0L
    private var retiredReleases=0L
    private val nativeTimes=ArrayDeque<Long>()
    internal var redraws=0L; internal var maxInstallNs=0L
    internal fun observeCounters(value:androidx.media3.exoplayer.DecoderCounters) {
        if(counters.any{it===value})return
        if(counters.size==64) {
            val old=counters.removeFirst();old.ensureUpdated()
            retiredInitializations+=old.decoderInitCount;retiredReleases+=old.decoderReleaseCount
        }
        counters.addLast(value)
    }
    internal fun nativeCall(durationNs:Long) {
        if(nativeTimes.size==256)nativeTimes.removeFirst()
        nativeTimes.addLast(durationNs)
    }
    internal var creates=0L; internal var releases=0L; internal var installs=0L; internal var prepares=0L
    internal var seeks=0L; internal var rebuilds=0L; internal var decoders=0L; internal var requests=0L
    internal var coalesced=0L; internal var frames=0L; internal var buffering=0L; internal var parameters=0L
    internal var recoveries=0L; internal var maxOfferNs=0L
    private val ring=ArrayDeque<EngineEvent>()
    internal fun record(event:EngineEvent) { if(ring.size==256)ring.removeFirst();ring.addLast(event) }
    fun events():List<EngineEvent> = ring.toList()
    fun snapshot():PreviewMetrics {
        counters.forEach{it.ensureUpdated()}
        val initializations=retiredInitializations+counters.sumOf{it.decoderInitCount.toLong()}
        val decoderReleases=retiredReleases+counters.sumOf{it.decoderReleaseCount.toLong()}
        val times=nativeTimes.sorted()
        return PreviewMetrics(creates,releases,installs,prepares,seeks,rebuilds,initializations,requests,
            coalesced,frames,buffering,parameters,recoveries,maxOfferNs,decoderReleases,
            if(times.isEmpty())0 else times[((times.size-1)*.95).toInt()],times.lastOrNull() ?: 0,redraws,maxInstallNs)
    }
}
