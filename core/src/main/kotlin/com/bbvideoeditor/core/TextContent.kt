package com.bbvideoeditor.core

import kotlinx.serialization.Serializable

@Serializable enum class TextFont { SANS, SERIF, MONO, CONDENSED }
@Serializable enum class TextAlignment { START, CENTER, END }

/** Typography is expressed on a canvas with height 1080, independent of preview/export resolution. */
@Serializable
data class TextContent(
    val text: String,
    val font: TextFont = TextFont.SANS,
    val fontSize: Float = 64f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val color: Long = 0xFFFFFFFF,
    val maxWidthFraction: Float = .85f,
    val outlineColor: Long = 0xFF000000,
    val outlineWidth: Float = 0f,
    val shadowColor: Long = 0xB3000000,
    val shadowRadius: Float = 0f,
    val shadowOffsetX: Float = 4f,
    val shadowOffsetY: Float = 4f,
    val backgroundColor: Long = 0x00000000,
    val padding: Float = 16f,
    val cornerRadius: Float = 12f,
) {
    init {
        require(text.isNotBlank() && text.length <= 2000) { "Enter between 1 and 2000 characters" }
        require(fontSize.isFinite() && fontSize in 12f..240f)
        require(maxWidthFraction.isFinite() && maxWidthFraction in .2f..1f)
        require(listOf(color, outlineColor, shadowColor, backgroundColor).all { it in 0..0xFFFFFFFF })
        require(outlineWidth.isFinite() && outlineWidth in 0f..16f)
        require(shadowRadius.isFinite() && shadowRadius in 0f..32f)
        require(shadowOffsetX.isFinite() && shadowOffsetX in -32f..32f)
        require(shadowOffsetY.isFinite() && shadowOffsetY in -32f..32f)
        require(padding.isFinite() && padding in 0f..64f)
        require(cornerRadius.isFinite() && cornerRadius in 0f..64f)
    }
}
