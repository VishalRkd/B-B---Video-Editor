package com.beatsandbeyond.video_editor.core.domain.engine

import android.view.Surface
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import kotlinx.coroutines.flow.Flow

/**
 * Engine responsible for real-time preview playback of a [Timeline].
 *
 * Implementations will use MediaCodec + MediaSync or ExoPlayer to decode and
 * render frames to an attached [Surface] (typically a SurfaceView or TextureView).
 *
 * All state is exposed as [Flow] to allow composable, lifecycle-aware UI observation.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement with ExoPlayer for single-clip preview.
 * Phase N: Extend to multi-track timeline composition.
 */
interface PreviewEngine {

    /** The current playback state as an observable [Flow]. */
    val playbackState: Flow<PlaybackState>

    /** The current playback position in milliseconds as an observable [Flow]. */
    val currentPositionMs: Flow<Long>

    /** The active filter at the current playback position. */
    val activeClipFilter: Flow<com.beatsandbeyond.video_editor.core.domain.model.VideoFilter>

    /**
     * Attaches a rendering [Surface] to the engine.
     * Must be called before [play] or [seekTo].
     */
    fun attach(surface: Surface)

    /**
     * Releases the [Surface] from the engine.
     * Call this when the surface is destroyed (e.g., in onSurfaceDestroyed).
     */
    fun detach()

    /**
     * Loads a [Timeline] into the engine for preview.
     * The engine will be in [PlaybackState.Ready] after successful loading.
     */
    suspend fun loadTimeline(timeline: Timeline)

    /** Starts or resumes playback. */
    fun play()

    /** Pauses playback at the current position. */
    fun pause()

    /** Stops playback and resets to position 0. */
    fun stop()

    /**
     * Seeks to the given [positionMs] without changing the play/pause state.
     */
    suspend fun seekTo(positionMs: Long)

    /**
     * Releases all engine resources. Call when the preview session is over.
     * After release, the engine must be re-initialized before use.
     */
    fun release()
}
