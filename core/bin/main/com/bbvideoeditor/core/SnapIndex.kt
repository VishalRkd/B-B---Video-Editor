package com.bbvideoeditor.core

import kotlin.math.abs

/** Immutable revision-derived endpoints. Exclusion is fixed at gesture start; the playhead is
 * queried separately so moving transport never sorts document endpoints again. */
class SnapIndex(project: Project, excludeClipId: String? = null) {
    private val points = TimelineSnapper.targets(project, excludeClipId).toLongArray()
    private fun nearest(time: Long): Long? {
        val found = points.binarySearch(time)
        if (found >= 0) return points[found]
        val insertion = -found - 1
        val left = points.getOrNull(insertion - 1)
        val right = points.getOrNull(insertion)
        return when { left == null -> right; right == null -> left
            abs(time - left) <= abs(right - time) -> left; else -> right }
    }
    fun snap(timeUs: Long, toleranceUs: Long, playheadUs: Long? = null): SnapResult {
        require(toleranceUs >= 0)
        val fixed = nearest(timeUs)
        val dynamic = playheadUs?.takeIf { it >= 0 }
        val best = when { fixed == null -> dynamic; dynamic == null -> fixed
            abs(timeUs - fixed) < abs(timeUs - dynamic) -> fixed
            abs(timeUs - fixed) == abs(timeUs - dynamic) -> minOf(fixed, dynamic)
            else -> dynamic }
        return if (best != null && abs(timeUs - best) <= toleranceUs) SnapResult(best, best)
            else SnapResult(timeUs.coerceAtLeast(0))
    }
    fun snapMove(startUs: Long, durationUs: Long, toleranceUs: Long, playheadUs: Long? = null): SnapResult {
        require(durationUs > 0 && toleranceUs >= 0)
        val start = startUs.coerceAtLeast(0)
        require(start <= Long.MAX_VALUE - durationUs)
        val a = snap(start, toleranceUs, playheadUs).takeIf { it.snapped }
        val end = snap(start + durationUs, toleranceUs, playheadUs).takeIf { it.snapped && it.timeUs >= durationUs }
        val b = end?.let { SnapResult(it.timeUs - durationUs, it.targetUs) }
        if (a == null) return b ?: SnapResult(start)
        if (b == null) return a
        val da = abs(a.timeUs - start); val db = abs(b.timeUs - start)
        return if (da < db || (da == db && a.targetUs!! <= b.targetUs!!)) a else b
    }
}
