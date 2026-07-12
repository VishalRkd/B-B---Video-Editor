package com.beatsandbeyond.video_editor.core.domain.model

import android.graphics.Bitmap

/**
 * A single rendered frame produced by [RenderEngine].
 *
 * @param bitmap      The rendered frame as a Bitmap.
 * @param positionMs  The timeline position this frame represents, in milliseconds.
 * @param frameIndex  The sequential index of this frame in the render output.
 */
data class RenderFrame(
    val bitmap: Bitmap,
    val positionMs: Long,
    val frameIndex: Int,
)
