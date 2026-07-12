package com.beatsandbeyond.video_editor.feature.mediaimport

import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.ImportAssetsUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.media.GetMediaItemsUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.CreateProjectUseCase
import com.beatsandbeyond.video_editor.feature.mediaimport.model.MediaImportUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * - Project creation from selected media (Phase 2)
 *
 * Navigation events are emitted via [navigationEvent] SharedFlow so the Fragment
 * never needs to call ViewModel methods that inspect nav state — a clean separation.
 */
@HiltViewModel
class MediaImportViewModel @Inject constructor(
    private val getMediaItemsUseCase: GetMediaItemsUseCase,
    private val createProjectUseCase: CreateProjectUseCase,
    private val importAssetsUseCase: ImportAssetsUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MediaImportUiState>(MediaImportUiState.Checking)
    val uiState: StateFlow<MediaImportUiState> = _uiState.asStateFlow()

    /**
     * One-shot navigation events emitted when project creation succeeds.
     * Fragment collects and navigates to ProjectDetail.
     */
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    sealed class NavigationEvent {
        /** Navigate to the project detail screen with this project ID. */
        data class OpenProject(val projectId: String) : NavigationEvent()
    }

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

    // ── Project creation ────────────────────────────────────────────────────

    /**
     * Creates a new project from the currently selected media items.
     * On success, emits [NavigationEvent.OpenProject] to navigate to the project detail screen.
     * On failure, emits an error state so the Fragment can show a Snackbar.
     */
    fun createProjectFromSelection() {
        val selectedItems = getSelectedItems()
        if (selectedItems.isEmpty()) return

        launchSafely {
            _uiState.update { state ->
                if (state is MediaImportUiState.Content) state.copy(isCreatingProject = true)
                else state
            }

            // Step 1: Create the project skeleton
            val projectName = generateDefaultProjectName()
            val createResult = createProjectUseCase(name = projectName)

            when (createResult) {
                is AppResult.Success -> {
                    val project = createResult.data

                    // Step 2: Import selected media as Assets bound to the new project
                    val importResult = importAssetsUseCase(
                        mediaItems = selectedItems,
                        projectId = project.id,
                    )

                    when (importResult) {
                        is AppResult.Success -> {
                            _navigationEvent.emit(NavigationEvent.OpenProject(project.id))
                        }
                        is AppResult.Error -> {
                            _uiState.update { state ->
                                if (state is MediaImportUiState.Content) {
                                    state.copy(
                                        isCreatingProject = false,
                                        errorMessage = importResult.message ?: "Failed to import media",
                                    )
                                } else state
                            }
                        }
                        is AppResult.Loading -> Unit
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        if (state is MediaImportUiState.Content) {
                            state.copy(
                                isCreatingProject = false,
                                errorMessage = createResult.message ?: "Failed to create project",
                            )
                        } else state
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun dismissError() {
        _uiState.update { state ->
            if (state is MediaImportUiState.Content) state.copy(errorMessage = null)
            else state
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun preserveSelection(newItems: List<MediaItem>): Set<Long> {
        val currentState = _uiState.value
        if (currentState !is MediaImportUiState.Content) return emptySet()
        val newItemIds = newItems.map { it.id }.toSet()
        return currentState.selectedIds.intersect(newItemIds)
    }

    private fun generateDefaultProjectName(): String {
        val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return "Project — ${formatter.format(java.util.Date())}"
    }
}
