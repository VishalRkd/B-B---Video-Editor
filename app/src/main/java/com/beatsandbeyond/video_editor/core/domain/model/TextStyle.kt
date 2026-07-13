package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Styling information for a text overlay clip placed on the [TrackType.TEXT] track.
 *
 * All coordinates ([positionX], [positionY]) are normalized to the range [0.0, 1.0]
 * relative to the preview viewport, so they are resolution-independent.
 *
 * @param text             The literal text to render.
 * @param fontFamily       Android font family name (e.g. "sans-serif", "serif", "monospace").
 * @param fontSize         Font size in SP (scaled pixels) at 1080p reference height.
 * @param color            ARGB color of the text glyphs.
 * @param backgroundColor  ARGB background color behind the text (0x00000000 = transparent).
 * @param isBold           Whether the text is bold.
 * @param isItalic         Whether the text is italic.
 * @param alignment        Horizontal alignment of the text block.
 * @param positionX        Normalized horizontal anchor (0 = left, 0.5 = center, 1 = right).
 * @param positionY        Normalized vertical anchor (0 = top, 0.5 = center, 1 = bottom).
 */
data class TextStyle(
    val text: String,
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 48f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0x00000000,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
) {
    /** True when a visible (non-transparent) background is configured. */
    val hasBackground: Boolean get() = (backgroundColor and 0xFF000000.toInt()) != 0
}

/**
 * Horizontal alignment of a [TextStyle] block within the preview viewport.
 */
enum class TextAlignment {
    /** Left-aligned. */
    START,
    /** Centered. */
    CENTER,
    /** Right-aligned. */
    END,
}
