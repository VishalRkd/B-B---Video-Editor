package com.beatsandbeyond.video_editor.feature.mediaimport

import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.usecase.media.GetMediaItemsUseCase
import com.beatsandbeyond.video_editor.feature.mediaimport.model.MediaImportUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Media Import screen.
 *
 * Manages:
 * - Permission state transitions
 * - Loading media from device storage
 * - Multi-selection of media items
 * - Filter changes (All / Videos / Photos)
 */
@HiltViewModel
class MediaImportViewModel @Inject constructor(
    private val getMediaItemsUseCase: GetMediaItemsUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MediaImportUiState>(MediaImportUiState.Checking)
    val uiState: StateFlow<MediaImportUiState> = _uiState.asStateFlow()

    // ── Permission handling ─────────────────────────────────────────────────

    fun onPermissionGranted() {
        loadMedia()
    }

    fun onPermissionDenied(shouldShowRationale: Boolean) {
        _uiState.value = MediaImportUiState.PermissionRequired(shouldShowRationale)
    }

    fun onPermissionPermanentlyDenied() {
        _uiState.value = MediaImportUiState.PermissionPermanentlyDenied
    }

    // ── Media loading ───────────────────────────────────────────────────────

    private fun loadMedia(filter: MediaFilter = MediaFilter.ALL) {
        viewModelScope.launch {
            getMediaItemsUseCase(filter).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Loading -> MediaImportUiState.Loading
                    is AppResult.Success -> MediaImportUiState.Content(
                        items = result.data,
                        selectedIds = preserveSelection(result.data),
                        filter = filter,
                    )
                    is AppResult.Error -> MediaImportUiState.Error(
                        result.message ?: "Failed to load media"
                    )
                }
            }
        }
    }

    fun applyFilter(filter: MediaFilter) {
        val currentState = _uiState.value
        if (currentState is MediaImportUiState.Content && currentState.filter == filter) return
        loadMedia(filter)
    }

    // ── Selection management ────────────────────────────────────────────────

    fun toggleSelection(item: MediaItem) {
        _uiState.update { state ->
            if (state !is MediaImportUiState.Content) return@update state
            val newSelectedIds = if (item.id in state.selectedIds) {
                state.selectedIds - item.id
            } else {
                state.selectedIds + item.id
            }
            state.copy(selectedIds = newSelectedIds)
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            if (state is MediaImportUiState.Content) state.copy(selectedIds = emptySet())
            else state
        }
    }

    fun getSelectedItems(): List<MediaItem> {
        val state = _uiState.value
        return if (state is MediaImportUiState.Content) state.selectedItems else emptyList()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * When reloading media (e.g., after a filter change), preserves selection for
     * items that still appear in the new result set.
     */
    private fun preserveSelection(newItems: List<MediaItem>): Set<Long> {
        val currentState = _uiState.value
        if (currentState !is MediaImportUiState.Content) return emptySet()
        val newItemIds = newItems.map { it.id }.toSet()
        return currentState.selectedIds.intersect(newItemIds)
    }
}
