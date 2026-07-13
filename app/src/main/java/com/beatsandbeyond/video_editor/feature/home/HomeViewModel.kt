package com.beatsandbeyond.video_editor.feature.home

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.usecase.project.DeleteProjectUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetAllProjectsUseCase
import com.beatsandbeyond.video_editor.core.utils.Logger
import com.beatsandbeyond.video_editor.feature.home.model.HomeUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 *
 * Loads the project list reactively from Room — any change (create/delete) is reflected
 * automatically without manual refresh. Handles soft-delete via [deleteProject].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllProjectsUseCase: GetAllProjectsUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        launchSafely {
            getAllProjectsUseCase().collect { result ->
                Logger.d(tag, "Projects flow emitted: $result")
                _uiState.value = when (result) {
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
        }
    }

    /**
     * Permanently deletes a project and all its associated assets.
     * The project list updates automatically via the reactive [getAllProjectsUseCase] Flow.
     */
    fun deleteProject(projectId: String) {
        launchSafely {
            deleteProjectUseCase(projectId)
        }
    }
}
