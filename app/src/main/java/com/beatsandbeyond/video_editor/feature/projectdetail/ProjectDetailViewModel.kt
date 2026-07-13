package com.beatsandbeyond.video_editor.feature.projectdetail

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.common.onSuccess
import com.beatsandbeyond.video_editor.core.domain.engine.AudioEngine
import com.beatsandbeyond.video_editor.core.domain.engine.ExportEngine
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.*
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.GetAssetsByProjectIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.audio.AddAudioTrackUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.audio.SetClipVolumeUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddOverlayClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddTextClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.ApplyFilterUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddEffectClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.effects.AddAdjustmentClipUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetProjectByIdUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.project.UpdateProjectUseCase
import com.beatsandbeyond.video_editor.core.domain.usecase.timeline.*
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.BufferOverflow
import java.util.UUID
import javax.inject.Inject
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.beatsandbeyond.video_editor.core.data.local.worker.ExportWorker

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
    private val moveClipsUseCase: MoveClipsUseCase,
    private val rippleTrimUseCase: RippleTrimUseCase,
    private val setSpeedUseCase: SetSpeedUseCase,
    private val addAudioTrackUseCase: AddAudioTrackUseCase,
    private val setClipVolumeUseCase: SetClipVolumeUseCase,
    private val addTextClipUseCase: AddTextClipUseCase,
    private val addOverlayClipUseCase: AddOverlayClipUseCase,
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val audioEngine: AudioEngine,
    private val exportEngine: ExportEngine,
    @ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val addVideoClipUseCase: AddVideoClipUseCase,
    private val addEffectClipUseCase: AddEffectClipUseCase,
    private val addAdjustmentClipUseCase: AddAdjustmentClipUseCase,
    private val dispatchers: CoroutineDispatchers,
) : BaseViewModel() {

    private val _waveforms = MutableStateFlow<Map<String, FloatArray>>(emptyMap())
    val waveforms: StateFlow<Map<String, FloatArray>> = _waveforms.asStateFlow()

    /** One-shot export progress events for the UI to render. */
    private val _exportProgress = MutableSharedFlow<ExportProgress>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val exportProgress: SharedFlow<ExportProgress> = _exportProgress.asSharedFlow()

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private var editingHistory = EditingHistory()
    private var currentProject: Project? = null
    // Snapshot of the project BEFORE a drag edit began. Drag edits mutate currentProject on
    // every isFinal=false frame, so by the time isFinal=true commits, currentProject already
    // equals the final state. We capture the pre-edit baseline on the first drag frame and use
    // it as the undo history entry, otherwise undo would be a no-op (push(final, final)).
    private var preEditProject: Project? = null
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
                val updatedTracks = project.timeline.tracks.map { track ->
                    if (track.type == TrackType.VIDEO && track.clips.isEmpty()) {
                        track.copy(
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
                    } else track
                }
                project = project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
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

    fun pause() {
        previewEngine.pause()
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
                state.copy(
                    selectedClipId = clipId,
                    selectedClipIds = if (clipId != null) setOf(clipId) else emptySet(),
                )
            } else state
        }
    }

    fun toggleClipSelection(clipId: String) {
        _uiState.update { state ->
            if (state is ProjectDetailUiState.Content) {
                val newSet = if (clipId in state.selectedClipIds) {
                    state.selectedClipIds - clipId
                } else {
                    state.selectedClipIds + clipId
                }
                state.copy(
                    selectedClipIds = newSet,
                    selectedClipId = newSet.lastOrNull(),
                )
            } else state
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            if (state is ProjectDetailUiState.Content) {
                state.copy(selectedClipId = null, selectedClipIds = emptySet())
            } else state
        }
    }

    // ── Live read accessors (used by gestures for direct, jank-free manipulation) ──

    /** Returns the current clip from the live project model (not a snapshot). */
    fun getClip(clipId: String): Clip? = currentProject?.timeline?.findClip(clipId)

    /** Returns the source asset duration for a clip (used to clamp trims). */
    fun getAssetDurationMs(clipId: String): Long {
        val clip = getClip(clipId) ?: return 0L
        return assetsMap[clip.assetId]?.durationMs ?: 0L
    }

    // ── Edit operations ─────────────────────────────────────────────────────

    fun trimSelectedClip(
        newTrimStartMs: Long,
        newTrimEndMs: Long,
        isFinal: Boolean,
        ripple: Boolean = false,
    ) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val originalClip = project.timeline.findClip(clipId) ?: return

        val result = if (ripple) {
            rippleTrimUseCase(project, clipId, newTrimStartMs, newTrimEndMs)
        } else {
            trimClipUseCase(project, clipId, newTrimStartMs, newTrimEndMs)
        }
        when (result) {
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

                    // Use the pre-drag snapshot as the undo baseline (see preEditProject docs).
                    val baseline = preEditProject ?: project
                    preEditProject = null

                    launchSafely {
                        editingHistory = editingHistory.push(baseline, updatedProject)
                        updateProjectUseCase(updatedProject) // auto-save to Room
                        currentProject = updatedProject
                        refreshContent(updatedProject, content.selectedClipId)
                        previewEngine.loadTimeline(updatedProject.timeline)
                        previewEngine.seekTo(targetTimelinePos)
                    }
                } else {
                    if (preEditProject == null) preEditProject = project
                    currentProject = result.data
                    // IMPORTANT: do NOT call refreshContent() here. Emitting a new StateFlow mid-gesture
                    // triggers renderTracks() -> rebindSelectedClipGestures(), which replaces the trim
                    // OnTouchListener while the finger is still down. The new listener's captured
                    // start values reset to 0, making coerceIn(0, 0-500) throw and crash the app.
                    // The Fragment already moves/resizes the view directly via the lane controller,
                    // so we only sync the model + seek the preview here.
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
            is AppResult.Loading -> {
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
            is AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    fun trimLeftToPlayhead() {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val positionMs = content.currentPositionMs

        val clip = project.timeline.findClip(clipId) ?: return
        if (positionMs <= clip.timelinePositionMs || positionMs >= clip.timelineEndMs) return

        val offsetOnTimelineMs = positionMs - clip.timelinePositionMs
        val offsetInSourceMs = (offsetOnTimelineMs * clip.speedFactor).toLong()
        val newTrimStartMs = clip.trimStartMs + offsetInSourceMs

        when (val result = rippleTrimUseCase(project, clipId, newTrimStartMs, clip.trimEndMs)) {
            is AppResult.Success -> applyEdit(result.data)
            else -> {}
        }
    }

    fun trimRightToPlayhead() {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        val positionMs = content.currentPositionMs

        val clip = project.timeline.findClip(clipId) ?: return
        if (positionMs <= clip.timelinePositionMs || positionMs >= clip.timelineEndMs) return

        val offsetOnTimelineMs = positionMs - clip.timelinePositionMs
        val offsetInSourceMs = (offsetOnTimelineMs * clip.speedFactor).toLong()
        val newTrimEndMs = clip.trimStartMs + offsetInSourceMs

        when (val result = trimClipUseCase(project, clipId, clip.trimStartMs, newTrimEndMs)) {
            is AppResult.Success -> applyEdit(result.data)
            else -> {}
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
            is AppResult.Loading -> {
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
                    val baseline = preEditProject ?: project
                    preEditProject = null
                    editingHistory = editingHistory.push(baseline, result.data)
                    currentProject = result.data
                    saveAndRefreshUi(result.data)
                } else {
                    if (preEditProject == null) preEditProject = project
                    currentProject = result.data
                    // Do NOT refreshContent() mid-drag: it would emit StateFlow and trigger
                    // renderTracks() -> rebindSelectedClipGestures(), replacing the move
                    // OnTouchListener while the finger is still down (causes jank/crash).
                    // The Fragment moves the view directly via the lane controller.
                }
            }
            is AppResult.Error -> {
                if (isFinal) {
                    refreshContent(project, clipId)
                }
            }
            is AppResult.Loading -> {
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
            is AppResult.Loading -> {
                // Not emitted
            }
        }
    }

    // ── Phase 4: Audio ──────────────────────────────────────────────────────

    /** Adds an audio clip from [asset] to the AUDIO track. */
    fun addAudioClip(asset: Asset) {
        val project = currentProject ?: return
        when (val result = addAudioTrackUseCase(project, asset)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> { /* add audio failed */ }
            is AppResult.Loading -> { /* not emitted */ }
        }
    }

    /** Sets the volume of the currently selected clip (0.0 muted → 2.0 boost). */
    fun setSelectedClipVolume(volume: Float) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        when (val result = setClipVolumeUseCase(project, clipId, volume)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> { /* volume set failed */ }
            is AppResult.Loading -> { /* not emitted */ }
        }
    }

    // ── Phase 5: Text / Overlay / Filters ───────────────────────────────────

    /** Adds a text overlay clip with the given [textStyle]. */
    fun addTextClip(textStyle: TextStyle) {
        val project = currentProject ?: return
        when (val result = addTextClipUseCase(project, textStyle)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> { /* add text failed */ }
            is AppResult.Loading -> { /* not emitted */ }
        }
    }

    /** Adds an image/video overlay clip from [asset]. */
    fun addOverlayClip(asset: Asset) {
        val project = currentProject ?: return
        when (val result = addOverlayClipUseCase(project, asset)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> { /* add overlay failed */ }
            is AppResult.Loading -> { /* not emitted */ }
        }
    }

    /** Applies a [VideoFilter] to the currently selected clip. */
    fun applyFilterToSelectedClip(filter: VideoFilter) {
        val content = currentContent ?: return
        val clipId = content.selectedClipId ?: return
        val project = currentProject ?: return
        when (val result = applyFilterUseCase(project, clipId, filter)) {
            is AppResult.Success -> applyEdit(result.data)
            is AppResult.Error -> { /* filter apply failed */ }
            is AppResult.Loading -> { /* not emitted */ }
        }
    }

    /** Returns the currently selected clip (or null). */
    fun getSelectedClip(): Clip? {
        val clipId = currentContent?.selectedClipId ?: return null
        return getClip(clipId)
    }

    /** Returns the [Asset] for a given assetId, or null. */
    fun getAsset(assetId: String): Asset? = assetsMap[assetId]

    // ── Phase 6: Export ──────────────────────────────────────────────────────

    /** Starts exporting the current project with the given [config]. Progress is emitted
     *  via [exportProgress]. Runs inside a persistent WorkManager foreground worker. */
    fun startExport(config: ExportConfig) {
        val project = currentProject ?: return
        val workData = workDataOf(
            ExportWorker.KEY_PROJECT_ID to project.id,
            ExportWorker.KEY_OUTPUT_PATH to config.outputPath,
            ExportWorker.KEY_RESOLUTION_WIDTH to config.resolution.width,
            ExportWorker.KEY_RESOLUTION_HEIGHT to config.resolution.height,
            ExportWorker.KEY_FRAME_RATE to config.frameRate,
            ExportWorker.KEY_VIDEO_BITRATE to config.videoBitrateBps,
            ExportWorker.KEY_AUDIO_BITRATE to config.audioBitrateBps,
            ExportWorker.KEY_FORMAT to config.format.name
        )

        val exportWorkRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(workData)
            .addTag("export_work_${project.id}")
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "export_unique_work_${project.id}",
            ExistingWorkPolicy.REPLACE,
            exportWorkRequest
        )

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(exportWorkRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            _exportProgress.emit(ExportProgress.Started)
                        }
                        WorkInfo.State.RUNNING -> {
                            val progressData = workInfo.progress
                            val stateName = progressData.getString(ExportWorker.KEY_STATE)
                            if (stateName == "IN_PROGRESS") {
                                val fraction = progressData.getFloat(ExportWorker.KEY_PROGRESS_FRACTION, 0f)
                                val frameMs = progressData.getLong(ExportWorker.KEY_FRAME_MS, 0L)
                                _exportProgress.emit(ExportProgress.InProgress(fraction, frameMs))
                            } else {
                                _exportProgress.emit(ExportProgress.Started)
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val outputPathResult = workInfo.outputData.getString(ExportWorker.KEY_COMPLETED_PATH) ?: config.outputPath
                            _exportProgress.emit(ExportProgress.Completed(outputPathResult, 0L))
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(ExportWorker.KEY_ERROR) ?: "Export failed"
                            _exportProgress.emit(ExportProgress.Failed(Exception(error)))
                        }
                        WorkInfo.State.CANCELLED -> {
                            _exportProgress.emit(ExportProgress.Cancelled)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /** Requests cancellation of an in-progress export. */
    fun cancelExport() {
        val project = currentProject ?: return
        WorkManager.getInstance(context).cancelUniqueWork("export_unique_work_${project.id}")
    }

    /**
     * Moves the entire multi-selection group by [deltaMs] (relative offset).
     * Used for group drag. [isFinal] pushes history once on release.
     */
    fun moveSelectedClips(deltaMs: Long, isFinal: Boolean) {
        val content = currentContent ?: return
        val project = currentProject ?: return
        val clipIds = content.selectedClipIds
        if (clipIds.isEmpty()) return

        when (val result = moveClipsUseCase(project, clipIds, deltaMs)) {
            is AppResult.Success -> {
                if (isFinal) {
                    val baseline = preEditProject ?: project
                    preEditProject = null
                    editingHistory = editingHistory.push(baseline, result.data)
                    currentProject = result.data
                    saveAndRefreshUi(result.data)
                } else {
                    if (preEditProject == null) preEditProject = project
                    currentProject = result.data
                    // Do NOT refreshContent() mid-drag (see moveSelectedClip for rationale).
                }
            }
            is AppResult.Error -> {
                // Group move failed
            }
            is AppResult.Loading -> {
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
                    assets = assetsMap,
                    totalDurationMs = project.durationMs,
                    selectedClipId = selectedClipId,
                    selectedClipIds = if (selectedClipId != null) setOf(selectedClipId) else state.selectedClipIds,
                    canUndo = editingHistory.canUndo,
                    canRedo = editingHistory.canRedo,
                )
            } else state
        }
        extractWaveforms(project)
    }

    private fun extractWaveforms(project: Project) {
        project.timeline.tracks
            .filter { it.type == TrackType.AUDIO }
            .flatMap { it.clips }
            .forEach { clip ->
                if (!_waveforms.value.containsKey(clip.assetId)) {
                    val asset = assetsMap[clip.assetId] ?: return@forEach
                    launchSafely {
                        audioEngine.extractWaveform(asset).onSuccess { data ->
                            _waveforms.update { it + (clip.assetId to data) }
                        }
                    }
                }
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

    fun importMedia(uri: Uri, mediaType: MediaType, customPositionMs: Long? = null) {
        val content = currentContent ?: return
        val project = currentProject ?: return
        val playheadPosition = customPositionMs ?: content.currentPositionMs

        launchSafely {
            _uiState.update { state ->
                if (state is ProjectDetailUiState.Content) {
                    state.copy(isImporting = true)
                } else state
            }

            try {
                val asset = createAssetFromUri(uri, mediaType)
                if (asset != null) {
                    val importResult = assetRepository.importAssets(listOf(asset), project.id)
                    if (importResult is AppResult.Success) {
                        val importedAsset = importResult.data.first()
                        assetsMap = assetsMap + (importedAsset.id to importedAsset)
                        
                        val beforeIds = project.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
                        val editResult = when (mediaType) {
                            MediaType.VIDEO -> addVideoClipUseCase(project, importedAsset, playheadPosition)
                            MediaType.AUDIO -> addAudioTrackUseCase(project, importedAsset, playheadPosition)
                            MediaType.IMAGE -> addOverlayClipUseCase(project, importedAsset, playheadPosition)
                        }

                        when (editResult) {
                            is AppResult.Success -> {
                                val updatedProject = editResult.data
                                val afterIds = updatedProject.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
                                val newClipId = (afterIds - beforeIds).firstOrNull()
                                
                                applyEdit(updatedProject)
                                if (newClipId != null) {
                                    selectClip(newClipId)
                                }
                            }
                            is AppResult.Error -> {
                                Logger.e("ProjectDetailViewModel", "Failed to apply edit for imported media: ${editResult.message}")
                            }
                            else -> Unit
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e("ProjectDetailViewModel", "Failed to import media", e)
            } finally {
                _uiState.update { state ->
                    if (state is ProjectDetailUiState.Content) {
                        state.copy(isImporting = false)
                    } else state
                }
            }
        }
    }

    private suspend fun createAssetFromUri(uri: Uri, mediaType: MediaType): Asset? = withContext(dispatchers.io) {
        val retriever = android.media.MediaMetadataRetriever()
        var durationMs = 0L
        var width = 0
        var height = 0
        var displayName = "Imported_${System.currentTimeMillis()}"
        var sizeBytes = 0L

        try {
            retriever.setDataSource(context, uri)
            
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L

            val wStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            width = wStr?.toIntOrNull() ?: 0
            height = hStr?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Logger.e("ProjectDetailViewModel", "Failed to retrieve media metadata", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("ProjectDetailViewModel", "Failed to query ContentResolver for metadata", e)
        }

        if (mediaType == MediaType.IMAGE) {
            durationMs = 0L
        }

        Asset(
            id = UUID.randomUUID().toString(),
            mediaUri = uri.toString(),
            mediaType = mediaType,
            displayName = displayName,
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            importedAt = System.currentTimeMillis()
        )
    }

    fun addDefaultTextAtPlayhead() {
        val content = currentContent ?: return
        val project = currentProject ?: return
        val playheadPosition = content.currentPositionMs
        val defaultStyle = TextStyle(text = "Default Text")
        
        launchSafely {
            val beforeIds = project.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
            when (val result = addTextClipUseCase(project, defaultStyle, playheadPosition)) {
                is AppResult.Success -> {
                    val updatedProject = result.data
                    val afterIds = updatedProject.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
                    val newClipId = (afterIds - beforeIds).firstOrNull()
                    applyEdit(updatedProject)
                    if (newClipId != null) {
                        selectClip(newClipId)
                    }
                }
                else -> Unit
            }
        }
    }

    fun addDefaultEffectAtPlayhead() {
        val content = currentContent ?: return
        val project = currentProject ?: return
        val playheadPosition = content.currentPositionMs
        
        launchSafely {
            val beforeIds = project.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
            when (val result = addEffectClipUseCase(project, playheadPosition)) {
                is AppResult.Success -> {
                    val updatedProject = result.data
                    val afterIds = updatedProject.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
                    val newClipId = (afterIds - beforeIds).firstOrNull()
                    applyEdit(updatedProject)
                    if (newClipId != null) {
                        selectClip(newClipId)
                    }
                }
                else -> Unit
            }
        }
    }

    fun addDefaultAdjustmentAtPlayhead() {
        val content = currentContent ?: return
        val project = currentProject ?: return
        val playheadPosition = content.currentPositionMs
        
        launchSafely {
            val beforeIds = project.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
            when (val result = addAdjustmentClipUseCase(project, playheadPosition)) {
                is AppResult.Success -> {
                    val updatedProject = result.data
                    val afterIds = updatedProject.timeline.tracks.flatMap { it.clips }.map { it.id }.toSet()
                    val newClipId = (afterIds - beforeIds).firstOrNull()
                    applyEdit(updatedProject)
                    if (newClipId != null) {
                        selectClip(newClipId)
                    }
                }
                else -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewEngine.release()
    }
}
