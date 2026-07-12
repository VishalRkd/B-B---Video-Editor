package com.beatsandbeyond.video_editor.feature.projectdetail.model

import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Project

/**
 * All possible UI states for the Project Detail / Editor screen.
 */
sealed class ProjectDetailUiState {

    /** Project data is being loaded from Room. */
    data object Loading : ProjectDetailUiState()

    /**
     * Project loaded and ready for preview/editing.
     *
     * @param project        The loaded project.
     * @param assets         Assets belonging to this project, in timeline order.
     * @param playbackState  Current player state (Idle, Ready, Playing, Paused, etc.).
     * @param currentPositionMs Current playback position in milliseconds.
     * @param totalDurationMs   Total duration of the timeline in milliseconds.
     */
    data class Ready(
        val project: Project,
        val assets: List<Asset>,
        val playbackState: PlaybackState = PlaybackState.Idle,
        val currentPositionMs: Long = 0L,
        val totalDurationMs: Long = 0L,
    ) : ProjectDetailUiState() {
        val isPlaying: Boolean get() = playbackState == PlaybackState.Playing
        val primaryVideoAsset: Asset? get() = assets.firstOrNull()
    }

    /**
     * An unrecoverable error occurred.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : ProjectDetailUiState()
}
