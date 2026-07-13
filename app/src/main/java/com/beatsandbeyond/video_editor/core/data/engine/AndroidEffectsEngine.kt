package com.beatsandbeyond.video_editor.core.data.engine

import android.graphics.Bitmap
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.EffectsEngine
import com.beatsandbeyond.video_editor.core.domain.model.Effect
import com.beatsandbeyond.video_editor.core.domain.model.EffectType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [EffectsEngine] using OpenGL or standard Android graphics.
 * Currently color corrections are applied at GPU export time via [GLFilterRenderer].
 */
@Singleton
class AndroidEffectsEngine @Inject constructor() : EffectsEngine {

    override suspend fun applyEffect(effect: Effect, frame: Bitmap): AppResult<Bitmap> {
        // GPU filters are processed via EGL/OpenGL texture rendering during export.
        // For software-based or local preview bitmap rendering, we return the frame as-is for now.
        return AppResult.Success(frame)
    }

    override suspend fun applyLut(lutPath: String, frame: Bitmap): AppResult<Bitmap> {
        return AppResult.Success(frame)
    }

    override fun getSupportedEffects(): List<EffectType> {
        return listOf(
            EffectType.BRIGHTNESS,
            EffectType.CONTRAST,
            EffectType.SATURATION
        )
    }
}
