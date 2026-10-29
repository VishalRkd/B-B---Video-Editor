package com.bbvideoeditor.app.editor

import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.MICROS_PER_SECOND
import com.bbvideoeditor.core.TimelineViewport

internal data class TimelineClipWindow(val startUs: Long, val endUs: Long)

/** Keep normal clip dimensions stable during pan. Very long clips use a bounded three-screen
 * segment that changes only at viewport buckets, avoiding enormous Compose constraints. */
internal fun timelineClipWindow(clip: Clip, viewport: TimelineViewport, widthPx: Double): TimelineClipWindow {
    val screenUs = (widthPx.coerceAtLeast(1.0) / viewport.pixelsPerSecond * MICROS_PER_SECOND).toLong().coerceAtLeast(1)
    if (clip.durationUs <= screenUs * 3) return TimelineClipWindow(clip.startUs, clip.endUs)
    val bucket = viewport.centerUs / screenUs
    val start = maxOf(clip.startUs, (bucket - 1).coerceAtLeast(0) * screenUs).coerceAtMost(clip.endUs - 1)
    val end = start + minOf(clip.endUs - start, screenUs * 3)
    return TimelineClipWindow(start, end)
}
