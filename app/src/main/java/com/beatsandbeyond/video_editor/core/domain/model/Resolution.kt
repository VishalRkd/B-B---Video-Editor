package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents the output resolution of a [Project].
 * Standard values: 1920×1080 (FHD), 1280×720 (HD), 3840×2160 (4K).
 */
data class Resolution(
    val width: Int,
    val height: Int,
) {
    companion object {
        val FHD = Resolution(1920, 1080)
        val HD = Resolution(1280, 720)
        val SQUARE = Resolution(1080, 1080)
        val PORTRAIT_9_16 = Resolution(1080, 1920)
        val UHD_4K = Resolution(3840, 2160)
    }

    val aspectRatio: Float get() = width.toFloat() / height.toFloat()
    override fun toString(): String = "${width}x${height}"
}
