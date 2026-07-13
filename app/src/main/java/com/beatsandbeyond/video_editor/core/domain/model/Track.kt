package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents a single lane in a [Timeline] that holds a sequence of [Clip]s.
 *
 * Tracks are typed — a VIDEO track holds video clips, an AUDIO track holds audio clips, etc.
 * In a future multi-track editor, a project may have multiple tracks of each type.
 *
 * @param id       Unique identifier.
 * @param type     The kind of content this track holds.
 * @param clips    Ordered list of clips on this track. Must not overlap.
 * @param isMuted  If true, the track's audio output is silenced during preview and export.
 * @param isLocked If true, clips on this track cannot be moved or edited in the UI.
 * @param volume   Master volume for the entire track (0.0 to 1.0).
 */
data class Track(
    val id: String,
    val type: TrackType,
    val clips: List<Clip> = emptyList(),
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val volume: Float = 1.0f,
) {
    /** The total duration occupied by clips on this track. */
    val durationMs: Long
        get() = clips.maxOfOrNull { it.timelineEndMs } ?: 0L
}

/**
 * Defines the purpose of a [Track].
 */
enum class TrackType {
    /** Primary video track. Every project starts with one. */
    VIDEO,
    /** Background music, voice-over, or extracted audio. */
    AUDIO,
    /** Secondary video, image, or sticker placed over the primary track. */
    OVERLAY,
    /** Text and title overlays. */
    TEXT,
    /** Visual effect filters applied to clips below it. */
    EFFECT,
    /** Color grading and adjustment layers. */
    ADJUSTMENT,
}
