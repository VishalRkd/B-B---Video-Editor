package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.AudioEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.AudioBuffer
import com.beatsandbeyond.video_editor.core.domain.model.AudioTrackData
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Concrete implementation of [AudioEngine] using Android's MediaCodec and MediaExtractor.
 */
@Singleton
class AndroidAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : AudioEngine {

    companion object {
        private const val TAG = "AndroidAudioEngine"
    }

    override suspend fun loadAudioTrack(asset: Asset): AppResult<AudioTrackData> = withContext(dispatchers.io) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, android.net.Uri.parse(asset.mediaUri), null)
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return@withContext AppResult.Error(Exception("No audio track found in asset: ${asset.id}"))
            }

            val format = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)

            AppResult.Success(
                AudioTrackData(
                    assetId = asset.id,
                    sampleRate = sampleRate,
                    channels = channelCount,
                    durationMs = durationUs / 1000
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load audio track: ${asset.id}", e)
            AppResult.Error(e)
        } finally {
            extractor.release()
        }
    }

    override fun mixTracks(tracks: List<AudioTrackData>): Flow<AudioBuffer> {
        // Phase 4/N: Full audio mixing implementation
        return emptyFlow()
    }

    override suspend fun extractWaveform(asset: Asset, samplesPerSecond: Int): AppResult<FloatArray> = withContext(dispatchers.io) {
        // Phase 4 waveform extraction.
        // A production app would decode PCM via MediaCodec and downsample. Here we synthesize a
        // deterministic, natural-looking waveform seeded by the asset id so it is stable across
        // re-extractions (no flicker) and bounded to [0.1, 0.9].
        try {
            val durationSec = (asset.durationMs / 1000).toInt().coerceAtLeast(1)
            val totalSamples = (durationSec * samplesPerSecond).coerceAtLeast(1)
            val random = java.util.Random(asset.id.hashCode().toLong())

            val waveform = FloatArray(totalSamples)
            var currentAmplitude = 0.5f

            for (i in 0 until totalSamples) {
                // Brownian walk for a natural-looking envelope, with a slow sine swell.
                currentAmplitude += (random.nextFloat() - 0.5f) * 0.2f
                val swell = 0.15f * kotlin.math.sin(i.toDouble() / totalSamples * Math.PI * 4).toFloat()
                currentAmplitude = (currentAmplitude + swell).coerceIn(0.1f, 0.9f)
                waveform[i] = currentAmplitude
            }

            AppResult.Success(waveform)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract waveform: ${asset.id}", e)
            AppResult.Error(e)
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
}
