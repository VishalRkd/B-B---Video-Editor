package com.beatsandbeyond.video_editor.core.domain.model

/**
 * An in-memory audio track produced by [AudioEngine] from an [Asset].
 *
 * Note: this is the app domain type, NOT [android.media.AudioTrack].
 *
 * @param assetId    ID of the [Asset] this audio track was extracted from.
 * @param sampleRate Sample rate in Hz (e.g., 44100, 48000).
 * @param channels   Number of audio channels (1 = mono, 2 = stereo).
 * @param durationMs Duration of the audio in milliseconds.
 */
data class AudioTrackData(
    val assetId: String,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
)

/**
 * A buffer of raw PCM audio samples produced during mixing.
 *
 * @param samples     Raw PCM float samples in the range [-1.0, 1.0].
 * @param sampleRate  Sample rate in Hz.
 * @param channels    Number of channels.
 * @param positionMs  Timeline position this buffer starts at, in milliseconds.
 */
data class AudioBuffer(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int,
    val positionMs: Long,
) {
    // FloatArray requires custom equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioBuffer) return false
        return samples.contentEquals(other.samples)
            && sampleRate == other.sampleRate
            && channels == other.channels
            && positionMs == other.positionMs
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + positionMs.hashCode()
        return result
    }
}
