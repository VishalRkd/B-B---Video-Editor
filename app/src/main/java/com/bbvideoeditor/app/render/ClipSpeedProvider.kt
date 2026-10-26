package com.bbvideoeditor.app.render

import androidx.media3.common.C
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import com.bbvideoeditor.core.Clip
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Media3 and our model both floor duration / speed. A split may carry an exact integer boundary
 * that differs by a few microseconds. Correct only the last source microseconds to retain that
 * boundary rather than accumulating an error across a long sequence. This is below one audio
 * sample at ordinary sample rates; it is not a user-visible speed ramp.
 */
@UnstableApi
class ClipSpeedProvider(clip: Clip) : SpeedProvider {
    private val speed = clip.speed
    private val correctionStartUs: Long
    private val correctionSpeed: Float

    init {
        val durationUs = clip.sourceRange.durationUs
        val natural = floor(durationUs.toDouble() / speed).toLong()
        if (natural == clip.durationUs) {
            correctionStartUs = Long.MAX_VALUE
            correctionSpeed = speed
        } else {
            // A reasonably sized tail avoids a negative remainder for speeds greater than one.
            val tailUs = min(durationUs, max(32L, kotlin.math.ceil(speed.toDouble() * 16).toLong()))
            correctionStartUs = durationUs - tailUs
            val prefixOutputUs = correctionStartUs.toDouble() / speed
            val tailOutputUs = clip.durationUs.toDouble() + 0.25 - prefixOutputUs
            require(tailOutputUs > 0) { "The split is too short to represent at this speed." }
            correctionSpeed = (tailUs / tailOutputUs).toFloat()
            require(correctionSpeed.isFinite() && correctionSpeed > 0)
            val corrected = floor(prefixOutputUs + tailUs.toDouble() / correctionSpeed).toLong()
            require(corrected == clip.durationUs) { "The split boundary cannot be represented exactly." }
        }
    }

    override fun getSpeed(timeUs: Long): Float = if (timeUs < correctionStartUs) speed else correctionSpeed
    override fun getNextSpeedChangeTimeUs(timeUs: Long): Long =
        if (correctionStartUs != Long.MAX_VALUE && timeUs < correctionStartUs) correctionStartUs else C.TIME_UNSET
}
