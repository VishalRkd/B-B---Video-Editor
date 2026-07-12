package com.beatsandbeyond.video_editor.feature.projectdetail

import android.view.Surface
import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.GetAllAssetsUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetProjectByIdUseCase
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Project Detail / Editor screen.
 *
 * Responsibilities:
 * - Load the project and its assets from Room
 * - Build a [Timeline] from assets and load it into [PreviewEngine]
 * - Manage player lifecycle (attach surface, play, pause, seek, release)
 * - Expose combined UI state (project data + playback state) as a single [StateFlow]
 *
 * The [PreviewEngine] is injected — this ViewModel knows nothing about ExoPlayer.
 * Engine cleanup ([PreviewEngine.release]) is called in [onCleared].
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val getProjectByIdUseCase: GetProjectByIdUseCase,
    private val getAllAssetsUseCase: GetAllAssetsUseCase,
    private val previewEngine: PreviewEngine,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    // ── Project loading ─────────────────────────────────────────────────────

    fun loadProject(projectId: String) {
        launchSafely {
            _uiState.value = ProjectDetailUiState.Loading

            val projectResult = getProjectByIdUseCase(projectId)
            if (projectResult is AppResult.Error) {
                _uiState.value = ProjectDetailUiState.Error(
                    projectResult.message ?: "Project not found"
                )
                return@launchSafely
            }
            val project = (projectResult as AppResult.Success).data

            // Load assets — take the first emission
            val assetsResult = getAllAssetsUseCase().first()
            val assets = when (assetsResult) {
                is AppResult.Success -> assetsResult.data
                else -> emptyList()
            }

            _uiState.value = ProjectDetailUiState.Ready(
                project = project,
                assets = assets,
                totalDurationMs = calculateDuration(assets),
            )

            // Subscribe to real-time playback state
            observePlaybackState()
            observePosition()

            // Build timeline and load into engine
            buildAndLoadTimeline(project, assets)
        }
    }

    // ── Surface management ──────────────────────────────────────────────────

    /** Called when the SurfaceView is created / becomes available. */
    fun onSurfaceReady(surface: Surface) {
        previewEngine.attach(surface)
    }

    /** Called when the SurfaceView is destroyed (e.g. fragment goes to backstack). */
    fun onSurfaceDestroyed() {
        previewEngine.detach()
    }

    // ── Playback controls ───────────────────────────────────────────────────

    fun togglePlayPause() {
        val state = _uiState.value
        if (state !is ProjectDetailUiState.Ready) return
        if (state.isPlaying) previewEngine.pause() else previewEngine.play()
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            previewEngine.seekTo(positionMs)
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun buildAndLoadTimeline(project: Project, assets: List<Asset>) {
        launchSafely {
            // Build a minimal timeline from assets for Phase 2 preview
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

            val timeline = Timeline(
                id = UUID.randomUUID().toString(),
                tracks = listOf(primaryTrack),
            )

            previewEngine.loadTimeline(timeline)
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            previewEngine.playbackState.collect { playbackState ->
                _uiState.update { state ->
                    if (state is ProjectDetailUiState.Ready) {
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
                    if (state is ProjectDetailUiState.Ready) {
                        state.copy(currentPositionMs = positionMs)
                    } else state
                }
            }
        }
    }

    private fun calculateDuration(assets: List<Asset>): Long =
        assets.sumOf { it.durationMs }

    // ── ViewModel cleared ───────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        previewEngine.release()
    }
}
