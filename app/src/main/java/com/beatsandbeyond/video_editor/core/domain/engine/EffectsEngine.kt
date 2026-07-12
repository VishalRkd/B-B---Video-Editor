package com.beatsandbeyond.video_editor.core.domain.engine

import android.graphics.Bitmap
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Effect
import com.beatsandbeyond.video_editor.core.domain.model.EffectType

/**
 * Engine responsible for applying visual effects, filters, and LUTs to media frames.
 *
 * Effects are applied per-frame at render time. The engine is stateless — each call
 * to [applyEffect] or [applyLut] is independent.
 *
 * Implementations will use OpenGL ES fragment shaders for GPU-accelerated processing.
 * LUT (Look-Up Table) support will use 3D texture sampling.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement software-based color grading (brightness, contrast, saturation).
 * Phase N: Full GPU shader pipeline, LUT support, custom filter marketplace.
 */
interface EffectsEngine {

    /**
     * Applies a single [Effect] to the given [frame] bitmap.
     * Returns a new [Bitmap] with the effect applied. The input [frame] is not modified.
     *
     * @param effect The effect definition including type and parameters.
     * @param frame  The source frame to process.
     */
    suspend fun applyEffect(effect: Effect, frame: Bitmap): AppResult<Bitmap>

    /**
     * Applies a LUT (Look-Up Table) from the given [lutPath] to a [frame].
     * Supports .cube format LUTs.
     *
     * @param lutPath Absolute path to the .cube LUT file.
     * @param frame   The source frame to grade.
     */
    suspend fun applyLut(lutPath: String, frame: Bitmap): AppResult<Bitmap>

    /**
     * Returns all [EffectType] values that this engine implementation supports.
     * Unsupported effects are silently skipped during rendering.
     */
    fun getSupportedEffects(): List<EffectType>
}
