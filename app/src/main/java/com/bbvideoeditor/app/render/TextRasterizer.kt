package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.core.graphics.createBitmap
import com.bbvideoeditor.core.CanvasSettings
import com.bbvideoeditor.core.TextAlignment
import com.bbvideoeditor.core.TextContent
import com.bbvideoeditor.core.TextFont
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

data class RasterizedText(val bitmap: Bitmap, val logicalWidth: Float, val logicalHeight: Float)

/** Shared typography for the inspector, canvas interaction, preview, and export. No Activity refs. */
class TextBitmapCache {
    private data class Key(val text: TextContent, val width: Int, val height: Int, val outputHeight: Int)
    private val cache = object : LruCache<Key, RasterizedText>(32 * 1024 * 1024) {
        override fun sizeOf(key: Key, value: RasterizedText) = value.bitmap.allocationByteCount
    }

    // Call on a worker/render thread. Eviction never recycles bitmaps still held by UI/GL consumers.
    @Synchronized
    fun get(content: TextContent, canvas: CanvasSettings, outputHeight: Int): RasterizedText {
        val key = Key(content, canvas.width, canvas.height, outputHeight)
        return cache.get(key) ?: TextRasterizer.render(content, canvas, outputHeight).also { cache.put(key, it) }
    }
    // LruCache is thread-safe. Main-thread trim callbacks must not wait on rasterization's lock.
    fun clearMemory() = cache.evictAll()
}

object TextRasterizer {
    fun render(content: TextContent, canvas: CanvasSettings, outputHeight: Int): RasterizedText {
        require(outputHeight in 2..8192)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = content.fontSize
            color = content.color.toInt()
            val family = when (content.font) {
                TextFont.SANS -> "sans-serif"
                TextFont.SERIF -> "serif"
                TextFont.MONO -> "monospace"
                TextFont.CONDENSED -> "sans-serif-condensed"
            }
            typeface = Typeface.create(family, when {
                content.bold && content.italic -> Typeface.BOLD_ITALIC
                content.bold -> Typeface.BOLD
                content.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            })
        }
        val shadowExtent = if (content.shadowRadius > 0) 2 * content.shadowRadius + max(abs(content.shadowOffsetX), abs(content.shadowOffsetY)) else 0f
        val guard = ceil(max(content.padding, content.outlineWidth + shadowExtent) + 3f).toInt()
        val maxWidth = (1080f * canvas.aspectRatio * content.maxWidthFraction - guard * 2).toInt().coerceAtLeast(1)
        val layoutWidth = ceil(Layout.getDesiredWidth(content.text, paint).toDouble()).toInt().coerceIn(1, maxWidth)
        val layout = StaticLayout.Builder.obtain(content.text, 0, content.text.length, paint, layoutWidth)
            .setAlignment(when (content.alignment) {
                TextAlignment.START -> Layout.Alignment.ALIGN_NORMAL
                TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                TextAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
            }).setIncludePad(true).setLineSpacing(0f, 1.05f).build()
        val logicalWidth = (layoutWidth + guard * 2).toFloat()
        val logicalHeight = (layout.height + guard * 2).toFloat()
        require(layout.lineCount <= 64 && logicalHeight <= 3240) { "Text is too tall. Reduce the font size or split it into more captions." }
        val scale = outputHeight / 1080f
        val width = ceil(logicalWidth * scale).toInt().coerceAtLeast(1)
        val height = ceil(logicalHeight * scale).toInt().coerceAtLeast(1)
        require(width.toLong() * height <= 8_388_608) { "Text is too large to render. Reduce the font size or text length." }
        val bitmap = createBitmap(width, height)
        try {
            val drawing = Canvas(bitmap)
            drawing.scale(scale, scale)
            if (content.backgroundColor ushr 24 != 0L) {
                val inset = (guard - content.padding).coerceAtLeast(0f)
                drawing.drawRoundRect(inset, inset, logicalWidth - inset, logicalHeight - inset,
                    content.cornerRadius, content.cornerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = content.backgroundColor.toInt() })
            }
            drawing.translate(guard.toFloat(), guard.toFloat())
            if (content.shadowRadius > 0) paint.setShadowLayer(content.shadowRadius, content.shadowOffsetX, content.shadowOffsetY, content.shadowColor.toInt())
            if (content.outlineWidth > 0) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = content.outlineWidth * 2
                paint.strokeJoin = Paint.Join.ROUND
                paint.color = content.outlineColor.toInt()
                layout.draw(drawing)
                paint.clearShadowLayer()
            }
            paint.style = Paint.Style.FILL
            paint.color = content.color.toInt()
            layout.draw(drawing)
            return RasterizedText(bitmap, logicalWidth, logicalHeight)
        } catch (error: Throwable) { bitmap.recycle(); throw error }
    }
}
