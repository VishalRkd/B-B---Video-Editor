package com.beatsandbeyond.video_editor.core.domain.model

import java.io.Serializable

/**
 * Supported properties for keyframe animations.
 */
enum class PropertyType {
    SCALE,
    ROTATION,
    POSITION_X,
    POSITION_Y,
    OPACITY
}

/** Default value of an animatable property type. */
val PropertyType.defaultValue: Float
    get() = when (this) {
        PropertyType.SCALE -> 1.0f
        PropertyType.ROTATION -> 0.0f
        PropertyType.POSITION_X -> 0.5f
        PropertyType.POSITION_Y -> 0.5f
        PropertyType.OPACITY -> 1.0f
    }

/** Type of interpolation curve. */
enum class InterpolationType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
}

/**
 * A single keyframe point on the timeline.
 */
data class Keyframe(
    val timeOffsetMs: Long,
    val value: Float,
    val interpolation: InterpolationType = InterpolationType.LINEAR
) : Serializable

/**
 * Holds keyframes of a specific [PropertyType] for a Clip.
 */
data class AnimatableProperty(
    val type: PropertyType,
    val keyframes: List<Keyframe> = emptyList()
) : Serializable {

    /** Returns the evaluated property value at the given offset from clip start. */
    fun getValueAt(timeOffsetMs: Long): Float {
        if (keyframes.isEmpty()) return type.defaultValue
        val sorted = keyframes.sortedBy { it.timeOffsetMs }
        if (timeOffsetMs <= sorted.first().timeOffsetMs) return sorted.first().value
        if (timeOffsetMs >= sorted.last().timeOffsetMs) return sorted.last().value

        val index = sorted.indexOfLast { it.timeOffsetMs <= timeOffsetMs }
        if (index == -1) return sorted.first().value
        val k1 = sorted[index]
        val k2 = sorted.getOrNull(index + 1) ?: return k1.value

        val range = k2.timeOffsetMs - k1.timeOffsetMs
        val progress = if (range == 0L) 0f else (timeOffsetMs - k1.timeOffsetMs).toFloat() / range

        val interpolated = InterpolationUtils.interpolate(progress, k1.interpolation)
        return k1.value + (k2.value - k1.value) * interpolated
    }
}
