package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.beatsandbeyond.video_editor.R

/**
 * Custom View that draws a scrollable timeline ruler with time labels.
 *
 * Major ticks every 1s (1000ms), minor ticks every 250ms.
 */
class TimelineRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var durationMs: Long = 0L
    private var scrollOffsetMs: Long = 0L
    private var pixelsPerMs: Float = 0.15f   // default: 150px per second
    private var viewportWidth: Int = 0

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_on_surface_variant)
        textSize = resources.getDimension(R.dimen.media_duration_badge_text_size)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }

    private val tickPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.color_outline)
        strokeWidth = 2f
    }

    fun setDurationMs(ms: Long) {
        durationMs = ms
        invalidate()
    }

    fun setScrollOffsetMs(ms: Long) {
        scrollOffsetMs = ms
        invalidate()
    }

    fun setPixelsPerMs(pxPerMs: Float) {
        pixelsPerMs = pxPerMs
        invalidate()
    }

    fun setViewportWidth(w: Int) {
        viewportWidth = w
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val visibleStartMs = scrollOffsetMs
        val viewWidth = if (viewportWidth > 0) viewportWidth else width
        val visibleEndMs = scrollOffsetMs + (viewWidth / pixelsPerMs).toLong()

        // Draw bottom line for ruler
        val totalWidth = durationMs * pixelsPerMs
        canvas.drawLine(0f, h - 1f, totalWidth, h - 1f, tickPaint)

        // Draw major ticks every 1000ms
        var tickMs = (visibleStartMs / 1000L) * 1000L
        while (tickMs <= visibleEndMs && tickMs <= durationMs) {
            val x = tickMs * pixelsPerMs
            canvas.drawLine(x, h * 0.4f, x, h, tickPaint)
            canvas.drawText(formatTime(tickMs), x, h * 0.3f, textPaint)
            tickMs += 1000L
        }

        // Draw minor ticks every 250ms (no labels)
        tickMs = (visibleStartMs / 250L) * 250L
        while (tickMs <= visibleEndMs && tickMs <= durationMs) {
            if (tickMs % 1000L != 0L) {
                val x = tickMs * pixelsPerMs
                canvas.drawLine(x, h * 0.7f, x, h, tickPaint)
            }
            tickMs += 250L
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }
}
