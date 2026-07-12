package com.beatsandbeyond.video_editor.core.domain.engine

import android.graphics.Bitmap
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.RenderFrame
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import kotlinx.coroutines.flow.Flow

/**
 * Engine responsible for compositing and rendering frames from a [Timeline].
 *
 * The [RenderEngine] is distinct from [PreviewEngine]:
 * - [PreviewEngine] is optimized for real-time interactive playback.
 * - [RenderEngine] is optimized for correctness and quality (used in export and frame capture).
 *
 * Implementations will use OpenGL ES or Vulkan for GPU-accelerated compositing.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement software renderer for basic single-track rendering.
 * Phase N: Implement GPU-accelerated multi-layer compositing.
 */
interface RenderEngine {

    /**
     * Renders a single composited frame at the given [positionMs].
     * Applies all effects, filters, and overlays defined in the [Timeline].
     *
     * @param timeline   The timeline to render.
     * @param positionMs The timestamp of the desired frame in milliseconds.
     */
    suspend fun renderFrame(timeline: Timeline, positionMs: Long): AppResult<Bitmap>

    /**
     * Renders a sequence of frames from [startMs] to [endMs] as a [Flow].
     * Each emission is a [RenderFrame] containing the bitmap and its timeline position.
     *
     * The caller is responsible for managing the output (writing to encoder, disk, etc.).
     *
     * @param timeline   The timeline to render.
     * @param startMs    Start position in milliseconds.
     * @param endMs      End position in milliseconds.
     * @param frameRate  Frames per second to render (e.g., 30.0f).
     */
    fun renderRange(
        timeline: Timeline,
        startMs: Long,
        endMs: Long,
        frameRate: Float,
    ): Flow<AppResult<RenderFrame>>
}
