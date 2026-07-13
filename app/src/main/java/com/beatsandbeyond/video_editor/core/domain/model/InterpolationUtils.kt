package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Utility class for keyframe interpolation calculations.
 */
object InterpolationUtils {
    /** Interpolates a progress value ([0.0, 1.0]) based on the chosen [InterpolationType]. */
    fun interpolate(progress: Float, type: InterpolationType): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (type) {
            InterpolationType.LINEAR -> p
            InterpolationType.EASE_IN -> p * p
            InterpolationType.EASE_OUT -> p * (2f - p)
            InterpolationType.EASE_IN_OUT -> {
                if (p < 0.5f) {
                    2f * p * p
                } else {
                    -1f + (4f - 2f * p) * p
                }
            }
        }
    }
}
