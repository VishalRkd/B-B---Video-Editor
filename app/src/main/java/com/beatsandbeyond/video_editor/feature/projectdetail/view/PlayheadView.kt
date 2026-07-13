package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.beatsandbeyond.video_editor.R

/**
 * Centered overlay vertical playhead indicator.
 * Draws a premium pointer shield containing the active timecode and a drop-shadowed white line.
 */
class PlayheadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = resources.getDimension(R.dimen.timeline_playhead_width)
        style = Paint.Style.STROKE
        setShadowLayer(4f, 1f, 1f, 0x80000000.toInt())
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1B1F") // Deep dark bubble background
        style = Paint.Style.FILL
    }

    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA") // Electric green timecode text
        textSize = resources.getDimension(R.dimen.media_duration_badge_text_size)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private var playheadX: Float = 0f
    private var timeMs: Long = 0L
    private val bubbleRect = RectF()
    private val path = Path()

    init {
        // Enable software rendering once to support shadows (not per-frame)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        playheadX = w / 2f
    }

    fun setPlayheadTime(ms: Long) {
        timeMs = ms
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()

        val rulerHeight = resources.getDimension(R.dimen.timeline_ruler_height) // 24dp
        val bubbleWidth = resources.getDimension(R.dimen.timeline_track_label_width) * 0.7f // ~56dp
        val bubbleHeight = rulerHeight * 0.8f // ~19dp
        val cornerRadius = 8f

        // Draw vertical playhead line starting from the bottom of the ruler (24dp)
        canvas.drawLine(playheadX, rulerHeight, playheadX, h, linePaint)

        // Setup boundary box for top capsule
        bubbleRect.set(
            playheadX - bubbleWidth / 2f,
            2f,
            playheadX + bubbleWidth / 2f,
            bubbleHeight
        )

        // Construct custom capsule shape with a downward pointer at the center bottom
        path.reset()
        path.moveTo(bubbleRect.left + cornerRadius, bubbleRect.top)
        path.lineTo(bubbleRect.right - cornerRadius, bubbleRect.top)
        path.quadTo(bubbleRect.right, bubbleRect.top, bubbleRect.right, bubbleRect.top + cornerRadius)
        path.lineTo(bubbleRect.right, bubbleRect.bottom - cornerRadius)
        path.quadTo(bubbleRect.right, bubbleRect.bottom, bubbleRect.right - cornerRadius, bubbleRect.bottom)
        
        // Downward pointer tip centered under playhead line
        path.lineTo(playheadX + 6f, bubbleRect.bottom)
        path.lineTo(playheadX, rulerHeight - 2f)
        path.lineTo(playheadX - 6f, bubbleRect.bottom)
        
        path.lineTo(bubbleRect.left + cornerRadius, bubbleRect.bottom)
        path.quadTo(bubbleRect.left, bubbleRect.bottom, bubbleRect.left, bubbleRect.bottom - cornerRadius)
        path.lineTo(bubbleRect.left, bubbleRect.top + cornerRadius)
        path.quadTo(bubbleRect.left, bubbleRect.top, bubbleRect.left + cornerRadius, bubbleRect.top)
        path.close()

        // Draw background shape
        canvas.drawPath(path, bubblePaint)
        
        // Draw white border outline
        canvas.drawPath(path, bubbleBorderPaint)

        // Draw formatted timecode text centered in the bubble
        val formattedTime = formatTime(timeMs)
        val textY = (bubbleRect.top + bubbleRect.bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(formattedTime, playheadX, textY, textPaint)
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }
}
