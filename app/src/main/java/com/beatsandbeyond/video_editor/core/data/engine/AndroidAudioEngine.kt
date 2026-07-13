package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.media.MediaCodec
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
        val durationSec = (asset.durationMs / 1000).toInt().coerceAtLeast(1)
        val totalSamples = (durationSec * samplesPerSecond).coerceAtLeast(1)
        val bucketDurationUs = 1_000_000L / samplesPerSecond

        val bucketSumSq = FloatArray(totalSamples)
        val bucketCount = IntArray(totalSamples)

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(context, android.net.Uri.parse(asset.mediaUri), null)
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return@withContext AppResult.Error(Exception("No audio track found in asset: ${asset.id}"))
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("MIME type missing")
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val TIMEOUT_US = 5000L

            while (!outputDone) {
                if (!inputDone) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inputBufferIndex)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0, sampleSize, extractor.sampleTime, 0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                var outputAvailable = true
                while (outputAvailable && !outputDone) {
                    val outBufIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> outputAvailable = false
                        outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                        outBufIndex >= 0 -> {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }

                            if (bufferInfo.size > 0) {
                                val byteBuffer = decoder.getOutputBuffer(outBufIndex)
                                if (byteBuffer != null) {
                                    byteBuffer.position(bufferInfo.offset)
                                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    
                                    val shortBuffer = byteBuffer.asShortBuffer()
                                    val sampleCountInBuf = shortBuffer.remaining()
                                    val startPtsUs = bufferInfo.presentationTimeUs

                                    for (j in 0 until sampleCountInBuf) {
                                        val sample = shortBuffer.get()
                                        val normSample = sample.toFloat() / 32768.0f

                                        val sampleTimeUs = startPtsUs + (j * 1_000_000L) / (sampleRate * channelCount)
                                        val bucketIndex = (sampleTimeUs / bucketDurationUs).toInt()

                                        if (bucketIndex in 0 until totalSamples) {
                                            bucketSumSq[bucketIndex] += normSample * normSample
                                            bucketCount[bucketIndex]++
                                        }
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(outBufIndex, false)
                        }
                    }
                }
            }

            val rmsArray = FloatArray(totalSamples)
            var maxRms = 0f
            for (i in 0 until totalSamples) {
                val count = bucketCount[i]
                if (count > 0) {
                    val rms = kotlin.math.sqrt(bucketSumSq[i] / count)
                    rmsArray[i] = rms
                    if (rms > maxRms) {
                        maxRms = rms
                    }
                } else {
                    rmsArray[i] = 0f
                }
            }

            val finalWaveform = FloatArray(totalSamples)
            if (maxRms > 0f) {
                for (i in 0 until totalSamples) {
                    finalWaveform[i] = (rmsArray[i] / maxRms).coerceIn(0f, 1f)
                }
            } else {
                for (i in 0 until totalSamples) {
                    finalWaveform[i] = 0.05f
                }
            }

            AppResult.Success(finalWaveform)

        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract waveform: ${asset.id}", e)
            var hasSomeData = false
            val finalWaveform = FloatArray(totalSamples)
            val rmsArray = FloatArray(totalSamples)
            var maxRms = 0f
            for (i in 0 until totalSamples) {
                val count = bucketCount[i]
                if (count > 0) {
                    hasSomeData = true
                    val rms = kotlin.math.sqrt(bucketSumSq[i] / count)
                    rmsArray[i] = rms
                    if (rms > maxRms) {
                        maxRms = rms
                    }
                }
            }

            if (hasSomeData && maxRms > 0f) {
                for (i in 0 until totalSamples) {
                    finalWaveform[i] = (rmsArray[i] / maxRms).coerceIn(0f, 1f)
                }
                AppResult.Success(finalWaveform)
            } else {
                AppResult.Error(e)
            }
        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
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
