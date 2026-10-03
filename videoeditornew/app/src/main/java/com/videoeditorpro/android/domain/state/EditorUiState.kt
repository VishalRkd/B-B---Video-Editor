package com.videoeditorpro.android.domain.state

import com.videoeditorpro.android.domain.model.EditorProject
import com.videoeditorpro.android.domain.model.VideoClipItem

enum class ActiveTool {
    NONE,
    TRIM,
    SPLIT,
    SPEED,
    MUSIC,
    FILTERS,
    TEXT,
    CANVAS
}

/**
 * Immutable StateFlow snapshot representing the entire Editor screen state.
 * Single source of truth consumed by Jetpack Compose.
 */
data class EditorUiState(
    val project: EditorProject = EditorProject(),
    val selectedClipId: String? = null,
    val currentPlayheadMs: Int = 0,
    val isPlaying: Boolean = false,
    val activeTool: ActiveTool = ActiveTool.NONE,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val exportProgress: Int? = null, // null when not exporting, 0..100 during export
    val exportSuccessPath: String? = null,
    val errorMessage: String? = null,
    val statusToast: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
) {
    val selectedClip: VideoClipItem?
        get() = project.videoClips.find { it.id == selectedClipId } ?: project.videoClips.firstOrNull()

    val selectedClipIndex: Int
        get() = project.videoClips.indexOfFirst { it.id == (selectedClip?.id ?: "") }.coerceAtLeast(0)

    val totalDurationMs: Int
        get() = project.totalDurationMs

    val isExporting: Boolean
        get() = exportProgress != null
}
