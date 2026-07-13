package com.beatsandbeyond.video_editor.core.domain.model

import java.io.Serializable

/**
 * A non-destructive visual filter that can be applied to a [Clip].
 *
 * Filters are stored on the clip and applied at render/preview time. The actual
 * GLSL rendering pipeline lands in Phase 7; in Phase 5 the filter is modeled and
 * selectable, and the editor persists the user's choice on the clip.
 *
 * All parameter values are normalized:
 * - [Brightness.value]  range [-1.0, 1.0]  (0 = unchanged)
 * - [Contrast.value]    range [0.0, 2.0]   (1 = unchanged)
 * - [Saturation.value]  range [0.0, 2.0]   (1 = unchanged)
 * - [Vintage.intensity] range [0.0, 1.0]
 * - [Vignette.radius]   range [0.0, 1.0]
 */
sealed class VideoFilter : Serializable {

    /** No filter applied. */
    data object None : VideoFilter()

    /** Brightness adjustment. */
    data class Brightness(val value: Float) : VideoFilter()

    /** Contrast adjustment. */
    data class Contrast(val value: Float) : VideoFilter()

    /** Saturation adjustment. */
    data class Saturation(val value: Float) : VideoFilter()

    /** Vintage / retro color grade. */
    data class Vintage(val intensity: Float) : VideoFilter()

    /** Radial vignette. */
    data class Vignette(val radius: Float) : VideoFilter()

    /** Human-readable name for UI display. */
    val displayName: String
        get() = when (this) {
            is None -> "None"
            is Brightness -> "Brightness"
            is Contrast -> "Contrast"
            is Saturation -> "Saturation"
            is Vintage -> "Vintage"
            is Vignette -> "Vignette"
        }
}
