package com.beatsandbeyond.video_editor.feature.home.model

import com.beatsandbeyond.video_editor.core.domain.model.Project

/**
 * Represents all possible UI states for the Home screen.
 *
 * Using a sealed class ensures exhaustive [when] expressions in the Fragment,
 * preventing unhandled states from being silently ignored.
 */
sealed class HomeUiState {

    /** Initial state before any data has been loaded. */
    data object Loading : HomeUiState()

    /**
     * No projects exist yet.
     * The UI should show an empty state with a "New Project" call-to-action.
     */
    data object Empty : HomeUiState()

    /**
     * Projects are loaded and ready to display.
     * @param projects Non-empty list of projects, ordered by most recently updated.
     */
    data class Content(val projects: List<Project>) : HomeUiState()

    /**
     * An error occurred while loading projects.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : HomeUiState()
}
