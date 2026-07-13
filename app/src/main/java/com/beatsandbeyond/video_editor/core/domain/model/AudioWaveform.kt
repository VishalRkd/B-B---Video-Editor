package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Normalized waveform amplitude data for a single audio (or video-with-audio) asset.
 *
 * [samples] are values in the range [0.0, 1.0], one per column when rendered at the
 * requested resolution. They are resolution-independent: the [WaveformView] downsamples
 * or upsamples to fit the on-screen clip width.
 *
 * @param samples     Normalized amplitude values in [0.0, 1.0].
 * @param durationMs  Source asset duration in milliseconds (used to map columns to time).
 */
data class AudioWaveform(
    val samples: FloatArray,
    val durationMs: Long,
) {
    init {
        require(samples.isNotEmpty()) { "Waveform samples must not be empty" }
        require(durationMs > 0L) { "Waveform duration must be positive" }
    }

    /** Number of samples per millisecond (resolution of the extraction). */
    val samplesPerMs: Float get() = samples.size.toFloat() / durationMs

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioWaveform) return false
        return durationMs == other.durationMs && samples.contentEquals(other.samples)
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}
