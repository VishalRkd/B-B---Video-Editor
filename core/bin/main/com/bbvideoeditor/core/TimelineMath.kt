package com.bbvideoeditor.core

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/** A viewport stores time, never an enormous pixel scroll offset. Safe for multi-hour timelines. */
data class TimelineViewport(val centerUs: Long = 0, val pixelsPerSecond: Double = 80.0) {
    init {
        require(centerUs >= 0)
        require(pixelsPerSecond.isFinite() && pixelsPerSecond in MIN_ZOOM..MAX_ZOOM)
    }

    fun timeToX(timeUs: Long, widthPx: Double): Double =
        widthPx / 2.0 + (timeUs - centerUs).toDouble() / MICROS_PER_SECOND * pixelsPerSecond

    fun xToTime(xPx: Double, widthPx: Double): Long =
        (centerUs.toDouble() + (xPx - widthPx / 2.0) / pixelsPerSecond * MICROS_PER_SECOND).roundToLong().coerceAtLeast(0)

    /** Positive deltas move content right under the finger, seeking toward earlier time. */
    fun pan(deltaPx: Double): TimelineViewport = copy(
        centerUs = (centerUs.toDouble() - deltaPx / pixelsPerSecond * MICROS_PER_SECOND).roundToLong().coerceAtLeast(0),
    )

    /** Keep the time underneath the pinch focal point stable while changing zoom. */
    fun zoom(factor: Double, focalXPx: Double, widthPx: Double): TimelineViewport {
        require(factor.isFinite() && factor > 0)
        val newScale = (pixelsPerSecond * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val focalDelta = focalXPx - widthPx / 2.0
        val newCenter = centerUs.toDouble() + focalDelta * MICROS_PER_SECOND * (1 / pixelsPerSecond - 1 / newScale)
        return copy(centerUs = newCenter.roundToLong().coerceAtLeast(0), pixelsPerSecond = newScale)
    }

    fun visibleRange(widthPx: Double): LongRange = xToTime(0.0, widthPx)..xToTime(widthPx, widthPx)

    companion object {
        const val MIN_ZOOM = 0.25
        const val MAX_ZOOM = 2400.0
    }
}

data class SnapResult(val timeUs: Long, val targetUs: Long? = null) {
    val snapped: Boolean get() = targetUs != null
}

/** Stateless snapping; zoom-dependent tolerance should be supplied by the view in microseconds. */
object TimelineSnapper {
    fun targets(project: Project, excludeClipId: String? = null, playheadUs: Long? = null): List<Long> = buildList {
        add(0L)
        if (playheadUs != null && playheadUs >= 0) add(playheadUs)
        project.timeline.tracks.forEach { track -> track.clips.forEach { clip ->
            if (clip.id != excludeClipId) { add(clip.startUs); add(clip.endUs) }
        } }
    }.distinct().sorted()

    fun snap(timeUs: Long, targets: List<Long>, toleranceUs: Long): SnapResult {
        require(toleranceUs >= 0)
        val candidate = targets.asSequence().filter { it >= 0 }.minByOrNull { abs(it - timeUs) }
        return if (candidate != null && abs(candidate - timeUs) <= toleranceUs) SnapResult(candidate, candidate)
        else SnapResult(timeUs.coerceAtLeast(0))
    }

    fun snap(project: Project, timeUs: Long, toleranceUs: Long, excludeClipId: String? = null, playheadUs: Long? = null): SnapResult =
        snap(timeUs, targets(project, excludeClipId, playheadUs), toleranceUs)

    /** Snap either edge of a dragged clip, using the smallest movement and stable start-edge tie breaking. */
    fun snapMove(startUs: Long, durationUs: Long, targets: List<Long>, toleranceUs: Long): SnapResult {
        require(durationUs > 0 && toleranceUs >= 0)
        val start = startUs.coerceAtLeast(0)
        val candidates = targets.flatMap { target ->
            buildList {
                if (target >= 0) add(SnapResult(target, target))
                if (target >= durationUs) add(SnapResult(target - durationUs, target))
            }
        }
        return candidates.minByOrNull { abs(it.timeUs - start) }
            ?.takeIf { abs(it.timeUs - start) <= toleranceUs }
            ?: SnapResult(start)
    }
}

/** Build once per track revision, then binary search to compose only the visible clips. */
class TrackIndex(track: Track) {
    private val ordered = track.clips.sortedBy { it.startUs }

    fun visibleClips(startUs: Long, endUs: Long): List<Clip> {
        if (endUs <= startUs || ordered.isEmpty()) return emptyList()
        var low = 0
        var high = ordered.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (ordered[mid].endUs <= startUs) low = mid + 1 else high = mid
        }
        val first = low
        while (low < ordered.size && ordered[low].startUs < endUs) low++
        return ordered.subList(first, low)
    }

    fun clipAt(timeUs: Long): Clip? {
        if (timeUs < 0) return null
        var low = 0
        var high = ordered.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (ordered[mid].startUs <= timeUs) low = mid + 1 else high = mid
        }
        return ordered.getOrNull(low - 1)?.takeIf { timeUs < it.endUs }
    }
}

/** Choose readable 1/2/5 ruler spacing, with no iteration proportional to project length. */
fun rulerStepUs(pixelsPerSecond: Double, minimumTickPixels: Double = 70.0): Long {
    require(pixelsPerSecond > 0 && pixelsPerSecond.isFinite() && minimumTickPixels > 0)
    val seconds = minimumTickPixels / pixelsPerSecond
    val magnitude = 10.0.pow(floor(log10(seconds)))
    val factor = seconds / magnitude
    val nice = when { factor <= 1 -> 1.0; factor <= 2 -> 2.0; factor <= 5 -> 5.0; else -> 10.0 }
    return (nice * magnitude * MICROS_PER_SECOND).roundToLong().coerceAtLeast(1)
}

fun rulerTicks(startUs: Long, endUs: Long, stepUs: Long): Sequence<Long> {
    require(stepUs > 0)
    if (endUs < startUs) return emptySequence()
    val first = ceil(startUs.coerceAtLeast(0).toDouble() / stepUs).toLong() * stepUs
    return generateSequence(first) { previous ->
        if (previous > Long.MAX_VALUE - stepUs) null else previous + stepUs
    }.takeWhile { it <= endUs }
}

fun formatTimecode(timeUs: Long, frameRate: Int = 30, showFrames: Boolean = false): String {
    require(frameRate > 0)
    val safe = timeUs.coerceAtLeast(0)
    val totalSeconds = safe / MICROS_PER_SECOND
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    val base = if (hours > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT, hours, minutes, seconds)
    else "%02d:%02d".format(java.util.Locale.ROOT, minutes, seconds)
    return if (showFrames) base + ":%02d".format(java.util.Locale.ROOT, safe % MICROS_PER_SECOND * frameRate / MICROS_PER_SECOND) else base
}

/** Finds the first fitting gap at or after the requested position; it never overwrites another clip. */
fun Track.availableStart(requestedUs: Long, durationUs: Long, excludingClipId: String? = null): Long {
    require(durationUs > 0)
    var position = requestedUs.coerceAtLeast(0)
    clips.asSequence().filterNot { it.id == excludingClipId }.sortedBy { it.startUs }.forEach { clip ->
        require(position <= Long.MAX_VALUE - durationUs)
        if (position < clip.endUs && position + durationUs > clip.startUs) position = clip.endUs
    }
    return position
}
