package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents a segment of an [Asset] placed at a specific position on a [Track].
 *
 * A Clip does NOT hold media data — it references an [Asset] by ID and defines
 * how a portion of that asset maps onto the timeline.
 *
 * @param id                 Unique identifier for this clip.
 * @param assetId            ID of the [Asset] this clip is sourced from.
 * @param timelinePositionMs Position on the timeline in milliseconds (from timeline start).
 * @param trimStartMs        Start point within the source asset in milliseconds.
 * @param trimEndMs          End point within the source asset in milliseconds.
 * @param speedFactor        Playback speed multiplier. 1.0 = normal, 0.5 = half speed, 2.0 = double.
 * @param volume             Audio volume multiplier for this clip (0.0 = muted, 1.0 = full).
 * @param effects            Ordered list of effects applied to this clip at render time.
 */
data class Clip(
    val id: String,
    val assetId: String,
    val timelinePositionMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val speedFactor: Float = 1.0f,
    val volume: Float = 1.0f,
    val effects: List<Effect> = emptyList(),
) {
    /**
     * The duration of this clip on the timeline, accounting for speed.
     * A 10s clip at 0.5x speed occupies 20s on the timeline.
     */
    val durationOnTimelineMs: Long
        get() = ((trimEndMs - trimStartMs) / speedFactor).toLong()

    /**
     * The absolute end position of this clip on the timeline.
     */
    val timelineEndMs: Long
        get() = timelinePositionMs + durationOnTimelineMs

    /**
     * The raw source duration selected from the asset (before speed adjustment).
     */
    val sourceDurationMs: Long
        get() = trimEndMs - trimStartMs
}
