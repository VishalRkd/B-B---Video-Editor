package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents the full timeline of a [Project].
 *
 * A Timeline contains one or more [Track]s. The total duration is derived from
 * the longest track, so it is computed rather than stored directly.
 *
 * Timelines are immutable — all editing operations in [TimelineEngine] return a new
 * Timeline instance, enabling straightforward undo/redo via state history.
 *
 * @param id     Unique identifier.
 * @param tracks Ordered list of tracks. Index 0 is the primary video track.
 */
data class Timeline(
    val id: String,
    val tracks: List<Track> = emptyList(),
) {
    /**
     * The total duration of the timeline in milliseconds.
     * Computed as the maximum [Track.durationMs] across all tracks.
     */
    val totalDurationMs: Long
        get() = tracks.maxOfOrNull { it.durationMs } ?: 0L

    /**
     * Shortcut to retrieve the primary video track (first track with type VIDEO).
     */
    val primaryVideoTrack: Track?
        get() = tracks.firstOrNull { it.type == TrackType.VIDEO }

    /**
     * True if the timeline has no clips on any track.
     */
    val isEmpty: Boolean
        get() = tracks.all { it.clips.isEmpty() }

    companion object {
        /** Creates a new empty timeline with a single primary video track. */
        fun empty(timelineId: String, primaryTrackId: String): Timeline = Timeline(
            id = timelineId,
            tracks = listOf(
                Track(id = primaryTrackId, type = TrackType.VIDEO)
            )
        )
    }
}
