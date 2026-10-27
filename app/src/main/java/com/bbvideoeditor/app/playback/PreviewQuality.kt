package com.bbvideoeditor.app.playback

import com.bbvideoeditor.core.CanvasSettings

/** GPU output size only. Project dimensions, source media and export settings remain independent. */
enum class PreviewQuality(val label: String, private val maximumEdge: Int) {
    FAST("Fast", 640), BALANCED("Balanced", 1280), HIGH("High", 1920);

    fun dimensions(canvas: CanvasSettings): Pair<Int, Int> {
        val scale = minOf(1.0, maximumEdge.toDouble() / maxOf(canvas.width, canvas.height))
        return even(canvas.width * scale) to even(canvas.height * scale)
    }

    private fun even(value: Double) = (value.toInt() / 2 * 2).coerceAtLeast(2)
}
