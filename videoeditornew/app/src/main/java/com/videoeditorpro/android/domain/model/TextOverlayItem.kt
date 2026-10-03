package com.videoeditorpro.android.domain.model

import java.util.UUID

/**
 * Represents a text title or subtitle overlay positioned on the video canvas.
 */
data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val colorHex: Long = 0xFFFFFFFF,
    val fontSizeSp: Float = 22f,
    val posX: Float = 0.5f, // Normalized center 0.0 to 1.0
    val posY: Float = 0.8f, // Normalized center 0.0 to 1.0
    val startTimeMs: Int = 0,
    val endTimeMs: Int = 3000
) {
    val durationMs: Int
        get() = (endTimeMs - startTimeMs).coerceAtLeast(500)
}
