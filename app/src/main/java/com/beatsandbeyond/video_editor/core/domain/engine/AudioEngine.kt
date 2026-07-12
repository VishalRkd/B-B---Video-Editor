package com.beatsandbeyond.video_editor.core.domain.engine

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.AudioBuffer
import com.beatsandbeyond.video_editor.core.domain.model.AudioTrackData
import kotlinx.coroutines.flow.Flow

/**
 * Engine responsible for all audio operations in the editor.
 *
 * Responsibilities include:
 * - Loading audio tracks from [Asset] references
 * - Mixing multiple audio tracks into a single output stream
 * - Extracting waveform data for timeline visualization
 * - Applying audio effects (volume, fade in/out, EQ — future)
 *
 * Implementations will use AudioTrack, MediaExtractor, and MediaCodec.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement waveform extraction for timeline display.
 * Phase N: Full audio mixing, voice-over recording, and EQ support.
 */
interface AudioEngine {

    /**
     * Loads an [Asset] as an audio track, validating that it contains audio.
     * Returns [AppResult.Error] if the asset has no audio stream.
     */
    suspend fun loadAudioTrack(asset: Asset): AppResult<AudioTrackData>

    /**
     * Mixes a list of [AudioTrackData] into a single interleaved PCM stream.
     * Volumes from each [AudioTrackData] are respected during mixing.
     *
     * @return A [Flow] emitting sequential [AudioBuffer] chunks.
     */
    fun mixTracks(tracks: List<AudioTrackData>): Flow<AudioBuffer>

    /**
     * Extracts waveform amplitude data from an [Asset] for visualization.
     *
     * @param asset             The audio or video asset to extract from.
     * @param samplesPerSecond  Resolution of the waveform (e.g., 100 = one sample per 10ms).
     * @return A [FloatArray] of amplitude values in the range [0.0, 1.0].
     */
    suspend fun extractWaveform(asset: Asset, samplesPerSecond: Int = 100): AppResult<FloatArray>
}
