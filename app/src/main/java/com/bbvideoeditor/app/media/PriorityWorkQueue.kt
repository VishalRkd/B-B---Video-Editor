package com.bbvideoeditor.app.media

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

enum class AssetPriority { VISIBLE, NEAR_VIEWPORT, PREFETCH, BACKGROUND }
data class AssetQueueMetrics(val queued: Int, val running: Int, val completed: Long, val discarded: Long)

/** Bounded, deduplicated worker queue. Cancelled callers relinquish queued work; a blocking
 * platform call keeps its worker until it actually returns. New visible work displaces old work. */
internal class PriorityWorkQueue<T>(workers: Int, private val capacity: Int = 96) : AutoCloseable {
    private class Entry<T>(val key: String, var priority: AssetPriority, val order: Long,
        val work: suspend () -> T?) {
        val result = CompletableDeferred<T?>()
        var waiters = 0
        var started = false
    }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val signal=Channel<Unit>(Channel.CONFLATED)
    private val lock=Any()
    private val entries=mutableMapOf<String,Entry<T>>()
    private var serial=0L; private var running=0; private var completed=0L; private var discarded=0L
    private var closed=false
    init {
        require(workers>0 && capacity>0)
        repeat(workers) { scope.launch {
            for(ignored in signal) while(isActive) {
                val next=synchronized(lock) {
                    entries.values.filter { !it.started }.minWithOrNull(compareBy<Entry<T>>{it.priority.ordinal}.thenByDescending{it.order})
                        ?.also { it.started=true;running++;signal.trySend(Unit) }
                } ?: break
                try { next.result.complete(next.work()) }
                catch(error:CancellationException){
                    next.result.cancel(error)
                    // An operation may carry a cancelled request context. Only cancellation of
                    // this worker/queue ends the worker; one cancelled entry must not drain the
                    // shared pool permanently and strand all subsequent visible requests.
                    if(!currentCoroutineContext().isActive)throw error
                }
                catch(error:Exception){next.result.completeExceptionally(error)}
                finally{synchronized(lock){entries.remove(next.key);running--;completed++}}
            }
        } }
    }
    suspend fun submit(key:String,priority:AssetPriority,operation:suspend()->T?):T? {
        val entry=synchronized(lock) {
            check(!closed) { "Asset queue is closed" }
            entries[key]?.also { if(priority.ordinal<it.priority.ordinal)it.priority=priority } ?: run {
                if(entries.values.count{!it.started}>=capacity) {
                    val stale=entries.values.filter{!it.started}.maxWithOrNull(compareBy<Entry<T>>{it.priority.ordinal}.thenBy{ -it.order })!!
                    if(stale.priority.ordinal<priority.ordinal)return null
                    entries.remove(stale.key);stale.result.complete(null);discarded++
                }
                Entry(key,priority,++serial,operation).also{entries[key]=it}
            }.also{it.waiters++}
        }
        signal.trySend(Unit)
        try{return entry.result.await()}
        finally{synchronized(lock){
            entry.waiters--
            if(entry.waiters==0 && !entry.started && entries[entry.key]===entry){entries.remove(entry.key);entry.result.complete(null);discarded++}
        }}
    }
    fun metrics()=synchronized(lock){AssetQueueMetrics(entries.values.count{!it.started},running,completed,discarded)}
    override fun close(){synchronized(lock){closed=true;entries.values.forEach{it.result.cancel()};entries.clear()};scope.cancel();signal.close()}
}
