package com.bbvideoeditor.app.playback

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.transformer.CompositionPlayer
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.TrackIndex
import com.bbvideoeditor.core.TrackType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PreviewState(
    val positionUs: Long = 0,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    val player: Player? = null,
    val warnings: List<String> = emptyList(),
)

/**
 * Lifecycle-owned preview of the shared render graph. The project is the only edit source; this
 * controller holds only transient player position and transport intent. All Player calls occur on
 * the main application looper. Compilation is cancellable and the newest project always wins.
 */
@UnstableApi
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
@MainThread
class PreviewEngine(context: Context, private val compiler: CompositionCompiler) : AutoCloseable {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = mutableState.asStateFlow()
    private var player: CompositionPlayer? = null
    private var project: Project? = null
    private var compileJob: Job? = null
    private var clockJob: Job? = null
    private var scrubJob: Job? = null
    private var generation = 0L
    private var appliedGeneration = -1L
    private var active = true
    private var closed = false
    private var requestedPlaying = false
    private var seekingUntilMs = 0L
    private var segmentIds: List<String?> = emptyList()
    private var visualIndices: List<TrackIndex> = emptyList()

    fun updateProject(project: Project) {
        checkMainThread()
        if (this.project == project) return
        this.project = project
        visualIndices = project.timeline.tracks.filter { it.type != TrackType.AUDIO }.map(::TrackIndex)
        mutableState.value = state.value.copy(positionUs = state.value.positionUs.coerceIn(0, project.durationUs))
        if (active) compileProject()
    }

    fun resume() {
        checkMainThread()
        if (closed) return
        if (active && player != null && state.value.error == null) return
        active = true
        compileProject()
    }

    fun play() {
        checkMainThread()
        if (!active || closed || (project?.durationUs ?: 0) == 0L) return
        requestedPlaying = true
        scrubJob?.cancel()
        player?.setScrubbingModeEnabled(false)
        if (state.value.positionUs >= (project?.durationUs ?: 0)) seek(0)
        if (state.value.error != null) {
            compileProject()
        } else if (appliedGeneration == generation) {
            player?.play()
        }
    }

    fun pause() {
        checkMainThread()
        requestedPlaying = false
        player?.pause()
        mutableState.value = state.value.copy(playing = false)
    }

    fun seek(positionUs: Long) {
        checkMainThread()
        val durationUs = project?.durationUs ?: 0L
        val targetUs = positionUs.coerceIn(0L, durationUs)
        mutableState.value = state.value.copy(positionUs = targetUs)
        // Keep the UI at the requested time while the decoder catches up. Player positions are ms.
        seekingUntilMs = android.os.SystemClock.elapsedRealtime() + 150
        val targetSegments = visualIndices.map { it.clipAt(targetUs)?.id }
        if (targetSegments != segmentIds && active) {
            // Media3 1.11 can retain a prewarmed decoder's old frame after a paused period
            // crossing. Recreate at the requested project position only at clip/gap boundaries.
            // In-clip scrubbing retains the graph; the debounce coalesces fast boundary crossings.
            segmentIds = targetSegments
            releasePlayer()
            mutableState.value = state.value.copy(player = null)
            compileProject()
            return
        }
        if (appliedGeneration == generation) player?.let { output ->
            output.setScrubbingModeEnabled(true)
            output.seekTo(targetUs / 1000L)
            scrubJob?.cancel()
            scrubJob = scope.launch {
                delay(180)
                if (player === output) output.setScrubbingModeEnabled(false)
            }
        }
    }

    /** Release surfaces, decoders, GL work and audio when the host leaves the foreground. */
    fun suspendPlayback() {
        checkMainThread()
        active = false
        requestedPlaying = false
        generation++
        compileJob?.cancel()
        compileJob = null
        val positionUs = if (appliedGeneration == generation - 1) player?.currentPosition?.times(1000) else null
        releasePlayer()
        mutableState.value = state.value.copy(
            player = null,
            playing = false,
            buffering = false,
            positionUs = (positionUs ?: state.value.positionUs).coerceIn(0, project?.durationUs ?: 0),
        )
    }

    override fun close() {
        checkMainThread()
        if (closed) return
        suspendPlayback()
        closed = true
        project = null
        scope.cancel()
    }

    private fun compileProject() {
        if (!active || closed) return
        val snapshot = project ?: return
        val revision = ++generation
        compileJob?.cancel()
        player?.pause()
        if (snapshot.durationUs == 0L) {
            releasePlayer()
            mutableState.value = PreviewState()
            return
        }
        mutableState.value = state.value.copy(playing = false, buffering = true, error = null)
        compileJob = scope.launch {
            try {
                // Coalesce continuous trim/drag edits before allocating another decoder graph.
                delay(60)
                // Lower preview resolution reduces GPU memory without changing aspect or edits.
                val ratio = minOf(1.0, 1280.0 / maxOf(snapshot.canvas.width, snapshot.canvas.height))
                val width = ((snapshot.canvas.width * ratio).toInt() / 2 * 2).coerceAtLeast(2)
                val height = ((snapshot.canvas.height * ratio).toInt() / 2 * 2).coerceAtLeast(2)
                val compiled = compiler.compile(snapshot, width, height)
                if (generation != revision || !active) return@launch
                val output = player ?: createPlayer().also { player = it }
                appliedGeneration = revision
                segmentIds = visualIndices.map { it.clipAt(state.value.positionUs)?.id }
                output.setComposition(compiled.composition, state.value.positionUs / 1000)
                output.prepare()
                mutableState.value = state.value.copy(player = output, warnings = compiled.warnings, error = null)
                if (requestedPlaying) output.play()
                startClock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == revision) fail(error.message ?: "This project cannot be previewed.")
            }
        }
    }

    private fun createPlayer(): CompositionPlayer = CompositionPlayer.Builder(context)
        .setVideoGraphFactory(MultipleInputVideoGraph.Factory())
        .setPerStreamMediaProgressionEnabled(true)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (appliedGeneration != generation || !active) return
                    if (player.playbackState == Player.STATE_ENDED) requestedPlaying = false
                    publishPlayerState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (appliedGeneration == generation && active) {
                        requestedPlaying = false
                        mutableState.value = state.value.copy(
                            playing = false, buffering = false,
                            error = "Preview failed (${error.errorCodeName}). ${error.cause?.message ?: error.message.orEmpty()}",
                        )
                    }
                }
            })
        }

    private fun startClock() {
        if (clockJob?.isActive == true) return
        clockJob = scope.launch {
            while (isActive && active) {
                publishPlayerState()
                delay(if (player?.isPlaying == true) 33L else 100L)
            }
        }
    }

    private fun publishPlayerState() {
        val output = player ?: return
        if (appliedGeneration != generation) return
        val old = state.value
        val positionUs = if (android.os.SystemClock.elapsedRealtime() < seekingUntilMs) old.positionUs
        else output.currentPosition.times(1000).coerceIn(0, project?.durationUs ?: 0)
        val snapshot = project
        if (output.isPlaying && snapshot != null && positionUs < snapshot.durationUs) {
            val nextSegments = visualIndices.map { it.clipAt(positionUs)?.id }
            if (nextSegments != segmentIds) {
                // Apply the same boundary reset during playback. All streams restart together
                // at one project time, preserving A/V sync rather than showing a stale layer.
                seek(positionUs)
                return
            }
        }
        mutableState.value = old.copy(
            positionUs = positionUs,
            playing = output.isPlaying,
            buffering = output.playbackState == Player.STATE_BUFFERING,
        )
    }

    private fun fail(message: String) {
        requestedPlaying = false
        releasePlayer()
        mutableState.value = state.value.copy(player = null, playing = false, buffering = false, error = message)
    }

    private fun releasePlayer() {
        clockJob?.cancel()
        scrubJob?.cancel()
        scrubJob = null
        clockJob = null
        val previous = player
        player = null
        appliedGeneration = -1
        previous?.release()
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "PreviewEngine must be accessed on the main thread." }
    }
}
