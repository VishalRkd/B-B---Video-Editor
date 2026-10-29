package com.bbvideoeditor.app.playback

import com.bbvideoeditor.core.*
import kotlin.math.abs
import kotlin.math.max

/** Main-confined rate feedback; intent still changes on every input event. Final requests bypass
 * this policy. Leave a display interval between expensive calls, rather than spending every UI
 * frame inside Player. Fast motion can skip intermediate source frames, never the final target. */
internal class PreviewCadence(refreshHz: Double = 60.0) {
    var displayPeriodNs = period(refreshHz); private set
    private var lastAdmissionNs = Long.MIN_VALUE
    private var nativeCostNs = 0L
    private var previousOfferNs = Long.MIN_VALUE
    private var previousPositionUs = 0L
    private var sourcePeriodNs = 33_333_333L
    var fast = false; private set

    fun refreshRate(hz: Double) { displayPeriodNs = period(hz) }
    fun offer(positionUs: Long, nowNs: Long, sourceFrameUs: Long) {
        sourcePeriodNs = sourceFrameUs.coerceAtLeast(1) * 1_000
        if (previousOfferNs != Long.MIN_VALUE && nowNs > previousOfferNs) {
            val outputMotionPerDisplay = abs(positionUs - previousPositionUs).toDouble() * displayPeriodNs / (nowNs - previousOfferNs)
            fast = outputMotionPerDisplay > sourceFrameUs
        }
        previousOfferNs = nowNs; previousPositionUs = positionUs
    }
    fun waitNs(nowNs: Long, interactive: Boolean): Long {
        if (!interactive || lastAdmissionNs == Long.MIN_VALUE) return 0
        val interval = max(displayPeriodNs + nativeCostNs, if (fast) sourcePeriodNs * 2 else sourcePeriodNs)
        return (interval - (nowNs - lastAdmissionNs)).coerceAtLeast(0)
    }
    fun applied(startNs: Long, costNs: Long) {
        lastAdmissionNs = startNs
        nativeCostNs = if (nativeCostNs == 0L) costNs else (nativeCostNs * 3 + costNs) / 4
    }
    fun endGesture() { previousOfferNs = Long.MIN_VALUE; fast = false }
    fun reset() { endGesture(); lastAdmissionNs = Long.MIN_VALUE; nativeCostNs = 0 }
    private fun period(hz: Double): Long {
        val refresh = hz.takeIf { it.isFinite() && it in 24.0..240.0 } ?: 60.0
        return (1_000_000_000.0 / refresh).toLong()
    }
}

internal fun samePausedContent(plan: RenderPlan, fromUs: Long, toUs: Long, renderTextInPlayer: Boolean): Boolean {
    for (track in plan.visuals) {
        val before = track.clipAt(fromUs); val after = track.clipAt(toUs)
        if (before?.id != after?.id) return false
        if (after != null) {
            if (after.properties.isNotEmpty()) return false
            val media = plan.project.media[after.mediaId] ?: return false
            // Average FPS does not prove VFR sample identity. Only identical public seek
            // milliseconds are safe to suppress for video; stills/gaps are time-invariant.
            if (media.kind == MediaKind.VIDEO && fromUs / 1_000 != toUs / 1_000) return false
        }
    }
    if (renderTextInPlayer) for (track in plan.text) if (track.clipAt(fromUs)?.id != track.clipAt(toUs)?.id) return false
    return true
}
