package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents the current playback state of the [PreviewEngine].
 */
sealed class PlaybackState {
    /** No media is loaded. */
    data object Idle : PlaybackState()

    /** Media is loaded and ready to play. */
    data object Ready : PlaybackState()

    /** Media is actively playing. */
    data object Playing : PlaybackState()

    /** Playback is paused. */
    data object Paused : PlaybackState()

    /** Playback has reached the end of the timeline. */
    data object Ended : PlaybackState()

    /** The engine is buffering/loading frames. */
    data object Buffering : PlaybackState()

    /** A playback error occurred. */
    data class Error(val cause: Throwable, val message: String?) : PlaybackState()
}
