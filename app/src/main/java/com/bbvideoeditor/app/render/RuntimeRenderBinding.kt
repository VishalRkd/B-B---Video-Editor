package com.bbvideoeditor.app.render

import com.bbvideoeditor.core.*
import java.util.concurrent.atomic.AtomicReference

/** Derived immutable plans can change compositor parameters without reinstalling media sources.
 * Timing, audio, effects, text and source structure still require a composition update. */
@androidx.media3.common.util.UnstableApi
class RuntimeRenderBinding(plan: RenderPlan) {
    private data class Snapshot(val plan:RenderPlan,val revision:Long)
    private data class Evaluation(val snapshot:Snapshot,val timeUs:Long,val frame:EvaluatedFrameState)
    private val current = AtomicReference(Snapshot(plan,0))
    @Volatile private var evaluated:Evaluation?=null
    var revision:Long
        get()=current.get().revision
        set(value){current.updateAndGet{it.copy(revision=value)}}
    val plan: RenderPlan get() = current.get().plan
    fun updateParameters(plan: RenderPlan, revision: Long) { current.set(Snapshot(plan,revision)) }
    fun evaluateFrame(timeUs:Long):EvaluatedFrameState {
        val snapshot=current.get()
        evaluated?.takeIf{it.snapshot===snapshot && it.timeUs==timeUs}?.let{return it.frame}
        return snapshot.plan.evaluateFrame(timeUs).also{evaluated=Evaluation(snapshot,timeUs,it)}
    }
    fun acceptsParameters(candidate: RenderPlan): Boolean {
        val before = plan.project; val after = candidate.project
        if (before.canvas != after.canvas || before.media != after.media || before.timeline.transitions != after.timeline.transitions) return false
        if (before.timeline.tracks.size != after.timeline.tracks.size) return false
        return before.timeline.tracks.zip(after.timeline.tracks).all { (a,b) ->
            a.id == b.id && a.type == b.type && a.muted == b.muted && a.clips.size == b.clips.size && a.clips.zip(b.clips).all { (x,y) ->
                if(a.type in listOf(TrackType.VIDEO,TrackType.OVERLAY))
                    x.copy(transform=Transform(),properties=emptyList()) == y.copy(transform=Transform(),properties=emptyList())
                else x == y
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
internal interface FrameGraphIdentity { val binding: RuntimeRenderBinding }
