package com.beatsandbeyond.video_editor.feature.projectdetail

import android.view.Surface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.*
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.GetAssetsByProjectIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetProjectByIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.UpdateProjectUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.*
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Project Detail / Editor screen.
 *
 * Responsibilities:
 * - Load the project and its assets from Room.
 * - Manage history (undo/redo stacks) and auto-saving to Room.
 * - Dispatch timeline modifications: trim, split, delete, move, speed.
 * - Manage player lifecycle and playback position.
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val getAssetsByProjectIdUseCase: GetAssetsByProjectIdUseCase,
    private val previewEngine: PreviewEngine,
    private val trimClipUseCase: TrimClipUseCase,
    private val splitClipUseCase: SplitClipUseCase,
    private val deleteClipUseCase: DeleteClipUseCase,
    private val moveClipUseCase: MoveClipUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private var editingHistory = EditingHistory()
    private var currentProject: Project? = null
    private var assetsMap: Map<String, Asset> = emptyMap()

    private val currentContent: ProjectDetailUiState.Content?
        get() = _uiState.value as? ProjectDetailUiState.Content

    // ── Project loading ─────────────────────────────────────────────────────

    fun loadProject(projectId: String) {
        launchSafely {
            _uiState.value = ProjectDetailUiState.Loading

            val projectResult = getProjectByIdUseCase(projectId)
            if (projectResult is AppResult.Error) {
                _uiState.value = ProjectDetailUiState.Error(
                    projectResult.exception.message ?: "Project not found"
                )
                return@launchSafely
            }
            var project = (projectResult as AppResult.Success).data

            // Load assets — take the first emission
            val assetsResult = getAssetsByProjectIdUseCase(projectId).first()
            val assets = when (assetsResult) {
                is AppResult.Success -> assetsResult.data
                else -> emptyList()
            }
            assetsMap = assets.associateBy { it.id }

            // If empty, initialize timeline from assets for compatibility
            if (project.timeline.isEmpty && assets.isNotEmpty()) {
                val primaryTrack = Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.VIDEO,
                    clips = assets.mapIndexed { index, asset ->
                        Clip(
                            id = UUID.randomUUID().toString(),
                            assetId = asset.id,
                            timelinePositionMs = assets.take(index).sumOf { it.durationMs },
                            trimStartMs = 0L,
                            trimEndMs = asset.durationMs,
                        )
                    }
                )
                val newTimeline = Timeline(
                    id = UUID.randomUUID().toString(),
                    tracks = listOf(primaryTrack),
                )
                project = project.copy(timeline = newTimeline)
                updateProjectUseCase(project)
            }

            currentProject = project
            editingHistory = EditingHistory()

            _uiState.value = ProjectDetailUiState.Content(
                project = project,
                assets = assetsMap,
                playbackState = PlaybackState.Idle,
                currentPositionMs = 0L,
                totalDurationMs = project.durationMs,
                selectedClipId = null,
                canUndo = false,
                canRedo = false,
            )

            observePlaybackState()
            observePosition()

            previewEngine.loadTimeline(project.timeline)
        }
    }

    // ── Surface management ──────────────────────────────────────────────────

    fun onSurfaceReady(surface: Surface) {
        previewEngine.attach(surface)
    }

    fun onSurfaceDestroyed() {
        previewEngine.detach()
    }

    // ── Playback controls ───────────────────────────────────────────────────

    fun togglePlayPause() {
        val content = currentContent ?: return
        if (content.isPlaying) previewEngine.pause() else previewEngine.play()
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            previewEngine.seekTo(positionMs)
        }
    }

    // ── Selection management ───────────────────────────────────────────────

    fun selectClip(clipId: String?) {
        _uiState.update { state ->
            if (state is ProjectDetailUiState.Content) {
                state.copy(selectedClipId = clipId)
            } else state
        }
    }

    // ── Edit operations ─────────────────────────────────────────────────────

    fun trimSelectedClip(newTrimStartMs: Long, newTrimEndMs: Long, isFinal: Boolean) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val originalClip = project.timeline.findClip(clipId) ?: return

        when (val result = trimClipUseCase(project, clipId, newTrimStartMs, newTrimEndMs)) {
            is AppResult.Success -> {
                if (isFinal) {
                    val updatedProject = result.data
                    val updatedClip = updatedProject.timeline.findClip(clipId)
                    val isTrimmingLeft = newTrimStartMs != originalClip.trimStartMs
                    
                    val targetTimelinePos = if (isTrimmingLeft) {
                        updatedClip?.timelinePositionMs ?: 0L
                    } else {
                        updatedClip?.timelineEndMs ?: 0L
                    }

                    launchSafely {
                        updateProjectUseCase(updatedProject) // auto-save to Room
                        currentProject = updatedProject
                        refreshContent(updatedProject, content.selectedClipId)
                        previewEngine.loadTimeline(updatedProject.timeline)
                        previewEngine.seekTo(targetTimelinePos)
                    }
                } else {
                    currentProject = result.data
                    refreshContent(result.data, content.selectedClipId)
                    
                    // Seek ExoPlayer to the active trim frame relative to the currently loaded clipping config
                    val isTrimmingLeft = newTrimStartMs != originalClip.trimStartMs
                    val targetSourceMs = if (isTrimmingLeft) newTrimStartMs else newTrimEndMs
                    val localOffsetMs = targetSourceMs - originalClip.trimStartMs
                    val relativeTimelineOffsetMs = (localOffsetMs / originalClip.speedFactor).toLong()
                    val targetTimelinePositionMs = originalClip.timelinePositionMs + relativeTimelineOffsetMs
                    seekTo(targetTimelinePositionMs)
                }
            }
            is AppResult.Error -> {
                if (isFinal) {
                    _uiState.value = ProjectDetailUiState.Error(result.exception.message ?: "Trim failed")
                }
            }
            AppResult.Loading -> {
                // Not emitted by usecases
            }
        }
    }

    fun splitAtPlayhead() {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val positionMs = content.currentPositionMs

        when (val result = splitClipUseCase(project, clipId, positionMs)) {
            is AppResult.Success -> {
                applyEdit(result.data)
                selectClip(null) // Clear selection after splitting
            }
            is AppResult.Error -> {
                // Splitting might fail if playhead is out of bounds of the selected clip
            }
            AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    fun deleteSelectedClip() {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return

        when (val result = deleteClipUseCase(project, clipId)) {
            is AppResult.Success -> {
                applyEdit(result.data)
                selectClip(null)
            }
            is AppResult.Error -> {
                // Delete failed
            }
            AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    fun moveSelectedClip(newTimelinePositionMs: Long, isFinal: Boolean) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return

        when (val result = moveClipUseCase(project, clipId, newTimelinePositionMs)) {
            is AppResult.Success -> {
                if (isFinal) {
                    applyEdit(result.data)
                } else {
                    currentProject = result.data
                    refreshContent(result.data, content.selectedClipId)
                }
            }
            is AppResult.Error -> {
                // Move failed
            }
            AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    fun changeSelectedClipSpeed(speedFactor: Float) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return

        when (val result = setSpeedUseCase(project, clipId, speedFactor)) {
            is AppResult.Success -> {
                applyEdit(result.data)
            }
            is AppResult.Error -> {
                // Speed failed
            }
            AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    // ── Undo / Redo ─────────────────────────────────────────────────────────

    fun undo() {
        val project = currentProject ?: return
        val (newHistory, revertedProject) = editingHistory.undo(project) ?: return
        editingHistory = newHistory
        currentProject = revertedProject
        saveAndRefreshUi(revertedProject)
    }

    fun redo() {
        val project = currentProject ?: return
        val (newHistory, redoneProject) = editingHistory.redo(project) ?: return
        editingHistory = newHistory
        currentProject = redoneProject
        saveAndRefreshUi(redoneProject)
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun applyEdit(newProject: Project) {
        val old = currentProject ?: return
        editingHistory = editingHistory.push(old, newProject)
        currentProject = newProject
        saveAndRefreshUi(newProject)
    }

    private fun saveAndRefreshUi(project: Project) {
        launchSafely {
            updateProjectUseCase(project) // auto-save to Room
            refreshContent(project, currentContent?.selectedClipId)
            previewEngine.loadTimeline(project.timeline)
        }
    }

    private fun refreshContent(project: Project, selectedClipId: String?) {
        _uiState.update { state ->
            if (state is ProjectDetailUiState.Content) {
                state.copy(
                    project = project,
                    totalDurationMs = project.durationMs,
                    selectedClipId = selectedClipId,
                    canUndo = editingHistory.canUndo,
                    canRedo = editingHistory.canRedo,
                )
            } else state
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            previewEngine.playbackState.collect { playbackState ->
                _uiState.update { state ->
                    if (state is ProjectDetailUiState.Content) {
                        state.copy(playbackState = playbackState)
                    } else state
                }
            }
        }
    }

    private fun observePosition() {
        viewModelScope.launch {
            previewEngine.currentPositionMs.collect { positionMs ->
                _uiState.update { state ->
                    if (state is ProjectDetailUiState.Content) {
                        state.copy(currentPositionMs = positionMs)
                    } else state
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewEngine.release()
    }
}
