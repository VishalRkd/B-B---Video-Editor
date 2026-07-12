package com.beatsandbeyond.video_editor.feature.home.model

import com.beatsandbeyond.video_editor.core.domain.model.Project

/**
 * All possible UI states for the Home screen.
 */
sealed class HomeUiState {

    /** Projects are being loaded from Room. */
    data object Loading : HomeUiState()

    /** No projects have been created yet. */
    data object Empty : HomeUiState()

    /**
     * Projects loaded and ready to display.
     * @param projects Sorted list of projects (most recently modified first).
     */
    data class Content(val projects: List<Project>) : HomeUiState()

    /**
     * An unrecoverable error occurred.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : HomeUiState()
}
