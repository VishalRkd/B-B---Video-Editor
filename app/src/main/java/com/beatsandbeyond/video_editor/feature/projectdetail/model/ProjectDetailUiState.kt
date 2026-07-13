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
     */
    data class Content(
        val project: Project,
        val assets: Map<String, Asset>,         // Keyed by assetId for O(1) lookups
        val playbackState: PlaybackState,
        val currentPositionMs: Long,
        val totalDurationMs: Long,
        val selectedClipId: String? = null,     // primary selection (null = no selection)
        val selectedClipIds: Set<String> = emptySet(), // all selected clips (group select)
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : ProjectDetailUiState() {
        val isPlaying: Boolean get() = playbackState == PlaybackState.Playing
    }

    /**
     * An unrecoverable error occurred.
     */
    data class Error(val message: String) : ProjectDetailUiState()
}
