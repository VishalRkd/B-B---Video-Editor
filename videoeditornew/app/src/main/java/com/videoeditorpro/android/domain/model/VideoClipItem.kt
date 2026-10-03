package com.videoeditorpro.android.domain.model

import java.util.UUID

/**
 * Represents a single video clip segment on the project timeline.
 * Supports non-destructive trimming, speed scaling, rotation, volume, and mute.
 */
data class VideoClipItem(
    val id: String = UUID.randomUUID().toString(),
    val sourcePath: String,
    val sourceDurationMs: Int,
    val trimStartMs: Int = 0,
    val trimEndMs: Int = sourceDurationMs,
    val speed: Float = 1.0f,
    val volume: Int = 100, // 0 to 200
    val rotation: Int = 0, // 0, 90, 180, 270
    val isMuted: Boolean = false,
    val filter: FilterItem = FilterItem.NONE
) {
    /**
     * The raw trimmed span before speed scaling.
     */
    val rawTrimmedDurationMs: Int
        get() = (trimEndMs - trimStartMs).coerceAtLeast(100)

    /**
     * The final effective duration on the timeline after speed scaling.
     * E.g. a 10s clip played at 2.0x takes 5s of timeline time.
     */
    val effectiveDurationMs: Int
        get() = (rawTrimmedDurationMs / speed.coerceAtLeast(0.1f)).toInt().coerceAtLeast(100)

    /**
     * Converts a timeline-relative time (0 .. effectiveDurationMs)
     * back to the absolute timestamp in the original media file.
     */
    fun timelineTimeToSourceTimeMs(timelineTimeMs: Int): Int {
        val scaled = (timelineTimeMs * speed).toInt()
        return (trimStartMs + scaled).coerceIn(trimStartMs, trimEndMs)
    }

    /**
     * Converts an absolute source timestamp back to timeline-relative time.
     */
    fun sourceTimeToTimelineTimeMs(sourceTimeMs: Int): Int {
        val offset = (sourceTimeMs - trimStartMs).coerceAtLeast(0)
        return (offset / speed.coerceAtLeast(0.1f)).toInt()
    }
}
