package com.beatsandbeyond.video_editor.feature.mediaimport.model

import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem

/**
 * Represents all possible UI states for the Media Import screen.
 */
sealed class MediaImportUiState {

    /** Permission check is in progress or not yet performed. */
    data object Checking : MediaImportUiState()

    /**
     * Storage permission is required but has not been granted.
     * @param shouldShowRationale True if the user has previously denied the permission
     *                            and Android recommends showing an explanation.
     */
    data class PermissionRequired(val shouldShowRationale: Boolean) : MediaImportUiState()

    /** Permission permanently denied — user must go to settings. */
    data object PermissionPermanentlyDenied : MediaImportUiState()

    /** Media is being loaded from MediaStore. */
    data object Loading : MediaImportUiState()

    /**
     * Media loaded successfully and the grid is ready to display.
     * @param items        All media items matching the current [filter].
     * @param selectedIds  Set of IDs of currently selected items.
     * @param filter       The currently applied filter.
     */
    data class Content(
        val items: List<MediaItem>,
        val selectedIds: Set<Long> = emptySet(),
        val filter: MediaFilter = MediaFilter.ALL,
    ) : MediaImportUiState() {
        val selectedCount: Int get() = selectedIds.size
        val hasSelection: Boolean get() = selectedIds.isNotEmpty()
        val selectedItems: List<MediaItem> get() = items.filter { it.id in selectedIds }
    }

    /**
     * An error occurred while loading media.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : MediaImportUiState()
}
