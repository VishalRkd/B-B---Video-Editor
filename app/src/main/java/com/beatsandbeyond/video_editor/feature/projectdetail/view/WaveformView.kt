package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.AudioWaveform
import kotlin.math.max

/**
 * Custom View that draws an audio waveform from an array of normalized amplitudes.
 *
 * Bars are drawn in the teal accent ([R.color.color_tertiary]) to match the audio
 * track design language. The view downsamples/upsamples the source samples to fit
 * its width, so it works for any clip size.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var amplitudes: FloatArray? = null

    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_tertiary)
        style = Paint.Style.FILL
    }

    private val barWidth = 4f
    private val barGap = 2f

    /** Sets the waveform from a domain [AudioWaveform]. */
    fun setWaveform(waveform: AudioWaveform) {
        amplitudes = waveform.samples
        invalidate()
    }

    /** Sets the waveform from a raw normalized amplitude array ([0.0, 1.0]). */
    fun setAmplitudes(data: FloatArray?) {
        amplitudes = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = amplitudes ?: return
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        val totalBarWidth = barWidth + barGap
        val maxBars = max(1, (w / totalBarWidth).toInt())

        // Downsample or upsample the data to fit the view width.
        val step = data.size.toFloat() / maxBars

        for (i in 0 until maxBars) {
            val dataIndex = (i * step).toInt().coerceAtMost(data.size - 1)
            val amplitude = data[dataIndex].coerceIn(0f, 1f)

            val barHeight = h * amplitude * 0.8f // Use 80% of height max
            val x = i * totalBarWidth
            val top = centerY - (barHeight / 2f)
            val bottom = centerY + (barHeight / 2f)

            canvas.drawRect(x, top, x + barWidth, bottom, waveformPaint)
        }
    }
}
