package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents an effect applied to a [Clip].
 *
 * Effects are non-destructive — they are applied at render/preview time and can be
 * freely added, reordered, or removed without altering the source asset.
 *
 * [parameters] is a flexible key-value map allowing each effect type to define its
 * own parameter set. Future: this may be replaced with typed sealed classes per effect.
 *
 * @param id           Unique identifier for this effect instance.
 * @param type         The kind of effect.
 * @param parameters   Named float parameters (e.g., "intensity" → 0.8f, "hue" → 30f).
 * @param startOffsetMs Time within the clip (from clip start) when this effect begins. 0 = from start.
 * @param endOffsetMs   Time within the clip when this effect ends. Long.MAX_VALUE = until clip end.
 */
data class Effect(
    val id: String,
    val type: EffectType,
    val parameters: Map<String, Float> = emptyMap(),
    val startOffsetMs: Long = 0L,
    val endOffsetMs: Long = Long.MAX_VALUE,
)

/**
 * All supported effect types.
 * Future: each type will be backed by an implementation in [EffectsEngine].
 */
enum class EffectType {
    FILTER,
    COLOR_CORRECTION,
    LUT,
    ANIMATION,
    BLUR,
    VIGNETTE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    SHARPEN,
    CUSTOM,
}
