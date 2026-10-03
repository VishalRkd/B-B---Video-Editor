package com.videoeditorpro.android.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoeditorpro.android.domain.command.ApplyFilterCommand
import com.videoeditorpro.android.domain.command.ChangeSpeedCommand
import com.videoeditorpro.android.domain.command.ChangeVolumeCommand
import com.videoeditorpro.android.domain.command.CommandManager
import com.videoeditorpro.android.domain.command.EditCommand
import com.videoeditorpro.android.domain.command.SplitClipCommand
import com.videoeditorpro.android.domain.command.TrimClipCommand
import com.videoeditorpro.android.domain.model.EditorProject
import com.videoeditorpro.android.domain.model.FilterItem
import com.videoeditorpro.android.domain.model.VideoClipItem
import com.videoeditorpro.android.domain.repository.ProjectDraftRepository
import com.videoeditorpro.android.domain.state.ActiveTool
import com.videoeditorpro.android.domain.state.EditorUiState
import com.videoeditorpro.android.engine.VideoEngineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Single Source of Truth for the video editor UI.
 * Coordinates Domain State, Undo/Redo commands, and Native Engine Subsystems.
 */
class EditorViewModel : ViewModel() {
    private val TAG = "EditorViewModel"

    private val commandManager = CommandManager()
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var draftRepo: ProjectDraftRepository? = null

    init {
        // Listen to playback time updates from native player
        VideoEngineManager.setProgressListener { curMs, durMs ->
            _uiState.update { current ->
                current.copy(
                    currentPlayheadMs = curMs,
                    isPlaying = VideoEngineManager.isPlaying()
                )
            }
        }
    }

    /**
     * Initializes project session with a video path.
     */
    fun loadVideo(videoPath: String, context: Context) {
        draftRepo = ProjectDraftRepository(context)
        VideoEngineManager.setContext(context)

        _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading project...") }

        viewModelScope.launch {
            val opened = VideoEngineManager.openVideo(videoPath)
            if (opened) {
                val proj = VideoEngineManager.getActiveProject() ?: EditorProject(
                    name = File(videoPath).nameWithoutExtension,
                    videoClips = listOf(
                        VideoClipItem(
                            sourcePath = videoPath,
                            sourceDurationMs = VideoEngineManager.getDuration()
                        )
                    )
                )

                _uiState.update {
                    it.copy(
                        project = proj,
                        selectedClipId = proj.videoClips.firstOrNull()?.id,
                        isLoading = false,
                        canUndo = commandManager.canUndo,
                        canRedo = commandManager.canRedo
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load video with native engine"
                    )
                }
            }
        }
    }

    fun play() {
        VideoEngineManager.play()
        _uiState.update { it.copy(isPlaying = true) }
    }

    fun pause() {
        VideoEngineManager.pause()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        if (VideoEngineManager.isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Int) {
        val clamped = positionMs.coerceIn(0, _uiState.value.totalDurationMs)
        VideoEngineManager.seekTo(clamped)
        _uiState.update { it.copy(currentPlayheadMs = clamped) }
    }

    fun selectClip(clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun setActiveTool(tool: ActiveTool) {
        _uiState.update { it.copy(activeTool = tool) }
    }

    // --- Editing Operations via Command Pattern (Enables Undo/Redo) ---

    private fun executeCommand(command: EditCommand) {
        val currentProject = _uiState.value.project
        val updatedProject = commandManager.execute(command, currentProject)

        // Sync with native engine
        VideoEngineManager.openProject(updatedProject)

        _uiState.update {
            it.copy(
                project = updatedProject,
                canUndo = commandManager.canUndo,
                canRedo = commandManager.canRedo,
                statusToast = command.description
            )
        }
    }

    fun undo() {
        val currentProject = _uiState.value.project
        val result = commandManager.undo(currentProject) ?: return
        val (revertedProject, command) = result

        VideoEngineManager.openProject(revertedProject)

        _uiState.update {
            it.copy(
                project = revertedProject,
                canUndo = commandManager.canUndo,
                canRedo = commandManager.canRedo,
                statusToast = "Undid: ${command.description}"
            )
        }
    }

    fun redo() {
        val currentProject = _uiState.value.project
        val result = commandManager.redo(currentProject) ?: return
        val (reAppliedProject, command) = result

        VideoEngineManager.openProject(reAppliedProject)

        _uiState.update {
            it.copy(
                project = reAppliedProject,
                canUndo = commandManager.canUndo,
                canRedo = commandManager.canRedo,
                statusToast = "Redid: ${command.description}"
            )
        }
    }

    /**
     * Splits selected clip at the current playhead position.
     */
    fun splitAtPlayhead() {
        val state = _uiState.value
        val playhead = state.currentPlayheadMs
        val clip = state.selectedClip ?: return

        val clipStartOnTimeline = state.project.getClipTimelineStartMs(clip.id)
        val clipRelativeMs = playhead - clipStartOnTimeline

        // Ensure minimum 300ms clip halves
        if (clipRelativeMs < 300 || clipRelativeMs > clip.effectiveDurationMs - 300) {
            _uiState.update { it.copy(statusToast = "Cannot split too close to clip boundary") }
            return
        }

        executeCommand(SplitClipCommand(clip.id, clipRelativeMs))
    }

    /**
     * Trims in/out points for the selected clip.
     */
    fun trimSelectedClip(newStartMs: Int, newEndMs: Int) {
        val clip = _uiState.value.selectedClip ?: return
        executeCommand(
            TrimClipCommand(
                clipId = clip.id,
                oldTrimStartMs = clip.trimStartMs,
                oldTrimEndMs = clip.trimEndMs,
                newTrimStartMs = newStartMs,
                newTrimEndMs = newEndMs
            )
        )
    }

    /**
     * Updates speed multiplier for selected clip.
     */
    fun setSpeed(speed: Float) {
        val clip = _uiState.value.selectedClip ?: return
        executeCommand(
            ChangeSpeedCommand(
                clipId = clip.id,
                oldSpeed = clip.speed,
                newSpeed = speed
            )
        )
    }

    /**
     * Updates volume and mute state for selected clip.
     */
    fun setVolume(volume: Int, isMuted: Boolean) {
        val clip = _uiState.value.selectedClip ?: return
        executeCommand(
            ChangeVolumeCommand(
                clipId = clip.id,
                oldVolume = clip.volume,
                oldMuted = clip.isMuted,
                newVolume = volume,
                newMuted = isMuted
            )
        )
    }

    /**
     * Applies filter preset or color adjustments to selected clip.
     */
    fun setFilter(filter: FilterItem) {
        val clip = _uiState.value.selectedClip ?: return
        executeCommand(
            ApplyFilterCommand(
                clipId = clip.id,
                oldFilter = clip.filter,
                newFilter = filter
            )
        )
    }

    /**
     * Exports the project to MP4 using native QProducer.
     */
    fun exportVideo(outputPath: String, context: Context) {
        val sb = VideoEngineManager.storyboardBridge.getStoryboard() ?: return
        _uiState.update { it.copy(exportProgress = 0, exportSuccessPath = null) }

        VideoEngineManager.exportEngine.export(
            context = context,
            storyboard = sb,
            outputPath = outputPath,
            onProgress = { progressPct ->
                _uiState.update { it.copy(exportProgress = progressPct) }
            },
            onComplete = { success, _, path ->
                _uiState.update {
                    it.copy(
                        exportProgress = null,
                        exportSuccessPath = if (success) path else null,
                        errorMessage = if (!success) "Export failed" else null
                    )
                }
            }
        )
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(exportProgress = null, exportSuccessPath = null) }
    }

    fun clearStatusToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    override fun onCleared() {
        super.onCleared()
        VideoEngineManager.release()
    }
}
