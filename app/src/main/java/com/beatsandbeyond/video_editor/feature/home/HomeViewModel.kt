package com.beatsandbeyond.video_editor.feature.home

import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import com.beatsandbeyond.video_editor.feature.home.model.HomeUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * Observes [ProjectRepository.getAllProjects] and maps the result to [HomeUiState].
 * The UI simply collects [uiState] — it has no direct knowledge of the repository.
 *
 * Phase 1: Shows Loading → Empty (no projects exist yet).
 * Phase 2: Will show Content with the list of saved projects.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : BaseViewModel() {

    /**
     * The current UI state, derived reactively from the project repository.
     * Backed by a [StateFlow] so the Fragment always has the latest value on
     * configuration changes without re-subscribing.
     */
    val uiState: StateFlow<HomeUiState> = projectRepository
        .getAllProjects()
        .map { result ->
            when (result) {
                is AppResult.Loading -> HomeUiState.Loading
                is AppResult.Success -> {
                    if (result.data.isEmpty()) HomeUiState.Empty
                    else HomeUiState.Content(result.data)
                }
                is AppResult.Error -> HomeUiState.Error(
                    result.message ?: "Failed to load projects"
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )
}
