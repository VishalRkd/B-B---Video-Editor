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
import androidx.media3.effect.SingleInputVideoGraph
import androidx.media3.transformer.CompositionPlayer
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.RenderTrack
import com.bbvideoeditor.core.RenderPlan
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
import kotlinx.coroutines.withContext

data class PreviewState(
    val positionUs: Long = 0,
    val playing: Boolean = false,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    val player: Player? = null,
    val warnings: List<String> = emptyList(),
    val quality: PreviewQuality = PreviewQuality.BALANCED,
)

/**
 * Lifecycle-owned preview of the shared render graph. The project is the only edit source; this
 * controller holds only transient player position and transport intent. All Player calls occur on
 * the main application looper. Compilation is cancellable and the newest project always wins.
 */
@UnstableApi
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
@MainThread
class PreviewEngine(
    context: Context,
    private val compiler: CompositionCompiler,
    initialPositionUs: Long = 0,
    initialQuality: PreviewQuality = PreviewQuality.BALANCED,
    private val renderTextInPlayer: Boolean = true,
) : AutoCloseable {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PreviewState(positionUs = initialPositionUs.coerceAtLeast(0), quality = initialQuality))
    val state: StateFlow<PreviewState> = mutableState.asStateFlow()
    private var player: CompositionPlayer? = null
    private var project: Project? = null
    private var renderPlan: RenderPlan? = null
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
    private var visualIndices: List<RenderTrack> = emptyList()
    private var requiresCompositor = true
    private var configuringPlayer = false
    private var hasRenderedFrame = false

    fun updateProject(project: Project) {
        checkMainThread()
        if (closed) return
        if (this.project == project) return
        val previous = this.project
        this.project = project
        mutableState.value = state.value.copy(positionUs = state.value.positionUs.coerceIn(0, project.durationUs))
        // Names, history timestamps, selection and locks do not change rendered content.
        if (previous != null && previous.canvas == project.canvas && previous.media == project.media &&
            previous.timeline.transitions == project.timeline.transitions &&
            previous.timeline.tracks.size == project.timeline.tracks.size &&
            previous.timeline.tracks.zip(project.timeline.tracks).all { (a, b) ->
                a.type == b.type && a.muted == b.muted && a.clips == b.clips
            }) return
        // The editor's canvas adapter renders the shared text bitmaps directly for under-finger
        // movement. Text-only changes do not require a decoder rebuild while duration is stable.
        if (!renderTextInPlayer && previous != null && previous.canvas == project.canvas &&
            previous.durationUs == project.durationUs && previous.media == project.media &&
            previous.timeline.tracks.filter { it.type != TrackType.TEXT } == project.timeline.tracks.filter { it.type != TrackType.TEXT } &&
            previous.timeline.transitions == project.timeline.transitions &&
            project.timeline.tracks.filter { it.type == TrackType.TEXT }.flatMap { it.clips }.all {
                it.effects.none { effect -> effect.enabled } && it.properties.isEmpty()
            }) return
        if (active) compileProject()
    }

    fun setQuality(quality: PreviewQuality) {
        checkMainThread()
        if (closed || state.value.quality == quality) return
        mutableState.value = state.value.copy(quality = quality)
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
        stopScrubbing()
        requestedPlaying = true
        mutableState.value = state.value.copy(playWhenReady = true)
        if (state.value.positionUs >= (project?.durationUs ?: 0)) seek(0)
        if (state.value.error != null) {
            compileProject()
        } else if (appliedGeneration == generation) {
            player?.play()
        }
    }

    fun pause() {
        checkMainThread()
        stopScrubbing()
        requestedPlaying = false
        player?.pause()
        mutableState.value = state.value.copy(playing = false, playWhenReady = false)
    }

    fun seek(positionUs: Long) {
        checkMainThread()
        if (closed) return
        val durationUs = project?.durationUs ?: 0L
        val targetUs = positionUs.coerceIn(0L, durationUs)
        mutableState.value = state.value.copy(positionUs = targetUs)
        // Keep the UI at the requested time while the decoder catches up. Player positions are ms.
        seekingUntilMs = android.os.SystemClock.elapsedRealtime() + 150
        val targetSegments = visualIndices.map { it.clipAt(targetUs)?.id }
        val animatedTarget = visualIndices.any { it.clipAt(targetUs)?.properties?.isNotEmpty() == true }
        if (requiresCompositor && (targetSegments != segmentIds || !hasRenderedFrame || animatedTarget) && active && appliedGeneration == generation) {
            // Media3 1.11 can retain a prewarmed decoder's old frame after a paused period
            // crossing. Recreate at the requested project position only at clip/gap boundaries.
            // Animated seeks also require a fresh compositor timestamp; static in-clip scrubs
            // retain their graph. The debounce coalesces rapid requests.
            segmentIds = targetSegments
            releasePlayer()
            mutableState.value = state.value.copy(player = null)
            compileProject()
            return
        }
        segmentIds = targetSegments
        if (appliedGeneration == generation) player?.let { output ->
            // Native scrubbing mode temporarily changes playWhenReady and restores its previous
            // value on exit. Use it only for paused scrubs, not seeks during requested playback.
            if (requestedPlaying) stopScrubbing() else {
                // At STATE_ENDED the native player may still have playWhenReady=true. Clear it
                // before entering scrub mode so a paused seek cannot resurrect playback.
                output.pause()
                output.setScrubbingModeEnabled(true)
            }
            output.seekTo(targetUs / 1000L)
            scrubJob?.cancel()
            scrubJob = if (requestedPlaying) null else scope.launch {
                delay(180)
                if (player === output) {
                    output.setScrubbingModeEnabled(false)
                    if (!requestedPlaying) output.pause()
                }
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
        val positionUs = if (appliedGeneration == generation - 1 && android.os.SystemClock.elapsedRealtime() >= seekingUntilMs) {
            player?.currentPosition?.times(1000)
        } else null
        releasePlayer()
        mutableState.value = state.value.copy(
            player = null,
            playing = false,
            playWhenReady = false,
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
        renderPlan = null
        visualIndices = emptyList()
        scope.cancel()
    }

    private fun compileProject() {
        if (!active || closed) return
        val snapshot = project ?: return
        val revision = ++generation
        compileJob?.cancel()
        stopScrubbing()
        player?.pause()
        if (snapshot.durationUs == 0L) {
            requestedPlaying = false
            releasePlayer()
            renderPlan = null
            visualIndices = emptyList()
            mutableState.value = PreviewState(quality = state.value.quality)
            return
        }
        mutableState.value = state.value.copy(playing = false, buffering = true, error = null)
        compileJob = scope.launch {
            try {
                // Coalesce continuous trim/drag edits before allocating another decoder graph.
                delay(60)
                // Lower preview resolution reduces GPU memory without changing aspect or edits.
                val (width, height) = state.value.quality.dimensions(snapshot.canvas)
                val plan = renderPlan?.takeIf { it.project === snapshot }
                    ?: withContext(Dispatchers.Default) { RenderPlan.derive(snapshot) }
                val compiled = compiler.compile(plan, width, height, preferSingleVisualInput = true, renderText = renderTextInPlayer)
                if (generation != revision || !active) return@launch
                // Legacy multi-input graphs cannot reliably flush their compositor queues after
                // edits. Single-input graphs can be updated in place; layered graphs start fresh.
                if (player != null && (requiresCompositor || requiresCompositor != compiled.requiresCompositor)) releasePlayer()
                requiresCompositor = compiled.requiresCompositor
                val output = player ?: createPlayer().also { player = it }
                appliedGeneration = revision
                renderPlan = compiled.plan
                visualIndices = compiled.plan.visuals
                segmentIds = visualIndices.map { it.clipAt(state.value.positionUs)?.id }
                configuringPlayer = true
                try {
                    hasRenderedFrame = false
                    output.setComposition(compiled.composition, state.value.positionUs / 1000)
                    output.prepare()
                    mutableState.value = state.value.copy(player = output, warnings = compiled.warnings, error = null)
                    if (requestedPlaying) output.play()
                } finally { configuringPlayer = false }
                startClock()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == revision) fail(error.message ?: "This project cannot be previewed.")
            }
        }
    }

    private fun createPlayer(): CompositionPlayer = CompositionPlayer.Builder(context)
        .setVideoGraphFactory(if (requiresCompositor) MultipleInputVideoGraph.Factory() else SingleInputVideoGraph.Factory())
        .setPerStreamMediaProgressionEnabled(true)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        .build()
        .apply {
            val owner = this
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (player !== this@PreviewEngine.player || configuringPlayer || appliedGeneration != generation || !active) return
                    if (player.playbackState == Player.STATE_ENDED) requestedPlaying = false
                    else if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) requestedPlaying = player.playWhenReady
                    publishPlayerState()
                }

                override fun onRenderedFirstFrame() {
                    if (owner === this@PreviewEngine.player) hasRenderedFrame = true
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (owner === this@PreviewEngine.player && appliedGeneration == generation && active) {
                        requestedPlaying = false
                        mutableState.value = state.value.copy(
                            playing = false, playWhenReady = false, buffering = false,
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
        if (requiresCompositor && output.isPlaying && positionUs < (project?.durationUs ?: 0)) {
            val nextSegments = visualIndices.map { it.clipAt(positionUs)?.id }
            // A new item can retain the previous decoded frame in Media3 1.11.1.
            // A disappearing layer is hidden by timeline alpha and needs no new input frame.
            if (nextSegments.indices.any { nextSegments[it] != null && nextSegments[it] != segmentIds.getOrNull(it) }) {
                seek(positionUs)
                return
            }
            segmentIds = nextSegments
        }
        // Some multi-input compositions never report native EOS even after their clock passes
        // the declared duration. The project is authoritative; stop transport at its exact end.
        if (requiresCompositor && positionUs >= (project?.durationUs ?: Long.MAX_VALUE) && requestedPlaying) {
            requestedPlaying = false
            output.pause()
        }
        mutableState.value = old.copy(
            positionUs = positionUs,
            playing = output.isPlaying,
            playWhenReady = requestedPlaying,
            buffering = output.playbackState == Player.STATE_BUFFERING,
        )
    }

    private fun fail(message: String) {
        requestedPlaying = false
        releasePlayer()
        mutableState.value = state.value.copy(player = null, playing = false, playWhenReady = false, buffering = false, error = message)
    }

    private fun stopScrubbing() {
        scrubJob?.cancel()
        scrubJob = null
        player?.setScrubbingModeEnabled(false)
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
