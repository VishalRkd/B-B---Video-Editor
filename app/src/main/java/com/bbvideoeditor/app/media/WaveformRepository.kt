package com.bbvideoeditor.app.media

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import android.util.AtomicFile
import android.util.LruCache
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/** Decodes audio incrementally into a bounded peak envelope; no full PCM file is retained. */
class WaveformRepository(context: Context) {
    private val context = context.applicationContext
    private val directory = File(this.context.cacheDir, "audio_waveforms_v1")
    private val memory = LruCache<String, FloatArray>(64)
    private val decoderSlot = Semaphore(1)
    private val locks = Array(16) { Mutex() }

    /** Empty means absent/unsupported audio, never a fabricated waveform. */
    suspend fun waveform(media: MediaReference, buckets: Int = 160): FloatArray {
        require(buckets in 8..4096) { "Waveform size must be between 8 and 4096" }
        if (media.kind == MediaKind.IMAGE || !media.hasAudio || media.durationUs <= 0) return floatArrayOf()
        val key = cacheKey("${media.uri}|${media.sizeBytes}|${media.durationUs}|$buckets")
        memory.get(key)?.let { return it.copyOf() }
        return withContext(Dispatchers.IO) {
            locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
                memory.get(key)?.let { return@withLock it.copyOf() }
                try {
                    directory.mkdirs()
                    val file = File(directory, "$key.wave")
                    readCache(file, buckets)?.let {
                        file.setLastModified(System.currentTimeMillis())
                        memory.put(key, it)
                        return@withLock it.copyOf()
                    }
                    val envelope = decoderSlot.withPermit { decode(media, buckets) }
                    coroutineContext.ensureActive()
                    if (envelope.isNotEmpty()) {
                        memory.put(key, envelope)
                        runCatching { writeCache(file, envelope); trimDisk() }
                    }
                    envelope.copyOf()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    floatArrayOf()
                }
            }
        }
    }

    fun clearMemory() = memory.evictAll()

    private suspend fun decode(media: MediaReference, buckets: Int): FloatArray {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var started = false
        try {
            extractor.setDataSource(context, Uri.parse(media.uri), null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return floatArrayOf()
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return floatArrayOf()
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()
            started = true
            val peaks = FloatArray(buckets)
            val info = MediaCodec.BufferInfo()
            var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var inputEnded = false
            var outputEnded = false
            var decodedFrames = 0L
            var lastProgressMs = SystemClock.elapsedRealtime()
            while (!outputEnded) {
                coroutineContext.ensureActive()
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val input = requireNotNull(decoder.getInputBuffer(inputIndex))
                        input.clear()
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0), 0)
                            extractor.advance()
                        }
                        lastProgressMs = SystemClock.elapsedRealtime()
                    }
                }
                val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = decoder.outputFormat
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        pcmEncoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        require(channels > 0 && sampleRate > 0) { "Invalid decoded audio format" }
                    }
                    outputIndex >= 0 -> {
                        try {
                            val output = decoder.getOutputBuffer(outputIndex)
                            if (output != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                output.position(info.offset)
                                output.limit(info.offset + info.size)
                                val pcm = output.slice().order(ByteOrder.LITTLE_ENDIAN)
                                decodedFrames += consumePcm(pcm, pcmEncoding, channels, sampleRate, info.presentationTimeUs, media.durationUs, peaks)
                            }
                            outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally {
                            decoder.releaseOutputBuffer(outputIndex, false)
                        }
                        lastProgressMs = SystemClock.elapsedRealtime()
                    }
                }
                if (SystemClock.elapsedRealtime() - lastProgressMs > 10_000L) throw IOException("Audio decoder stopped making progress")
            }
            return if (decodedFrames > 0) peaks else floatArrayOf()
        } finally {
            if (started) runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }

    private fun readCache(file: File, count: Int): FloatArray? = runCatching {
        if (!file.exists() || file.length() != 8L + count * 4L) return@runCatching null
        DataInputStream(AtomicFile(file).openRead()).use { input ->
            if (input.readInt() != CACHE_VERSION || input.readInt() != count) return@use null
            FloatArray(count) { input.readFloat().also { require(it.isFinite() && it in 0f..1f) } }
        }
    }.getOrNull()

    private fun writeCache(file: File, values: FloatArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            val output = DataOutputStream(stream)
            output.writeInt(CACHE_VERSION)
            output.writeInt(values.size)
            values.forEach(output::writeFloat)
            output.flush()
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun trimDisk() {
        val files = directory.listFiles { file -> file.extension == "wave" }.orEmpty().sortedBy { it.lastModified() }
        var bytes = files.sumOf { it.length() }
        var count = files.size
        for (file in files) {
            if (bytes <= 16L * 1024 * 1024 && count <= 1024) break
            val length = file.length()
            if (file.delete()) { bytes -= length; count-- }
        }
    }

    private companion object { const val CACHE_VERSION = 1 }
}

/** Kept independent of codec state so timestamp/channel handling can be tested directly. */
fun consumePcm(
    pcm: ByteBuffer,
    encoding: Int,
    channels: Int,
    sampleRate: Int,
    presentationTimeUs: Long,
    durationUs: Long,
    peaks: FloatArray,
): Long {
    require(channels > 0 && sampleRate > 0 && durationUs > 0 && peaks.isNotEmpty())
    val bytesPerSample = when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> 1
        AudioFormat.ENCODING_PCM_16BIT -> 2
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
        AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 4
        else -> throw IOException("Unsupported decoded PCM encoding")
    }
    var frame = 0L
    while (pcm.remaining() >= channels * bytesPerSample) {
        var peak = 0f
        repeat(channels) {
            val sample = when (encoding) {
                AudioFormat.ENCODING_PCM_8BIT -> ((pcm.get().toInt() and 255) - 128) / 128f
                AudioFormat.ENCODING_PCM_16BIT -> pcm.short / 32768f
                AudioFormat.ENCODING_PCM_24BIT_PACKED -> {
                    val value = (pcm.get().toInt() and 255) or ((pcm.get().toInt() and 255) shl 8) or (pcm.get().toInt() shl 16)
                    value / 8388608f
                }
                AudioFormat.ENCODING_PCM_32BIT -> (pcm.int / 2147483648.0).toFloat()
                else -> pcm.float
            }
            if (sample.isFinite()) peak = maxOf(peak, abs(sample).coerceAtMost(1f))
        }
        val timeUs = presentationTimeUs + frame * 1_000_000L / sampleRate
        if (timeUs in 0 until durationUs) {
            val bucket = (timeUs.toDouble() / durationUs * peaks.size).toInt().coerceIn(0, peaks.lastIndex)
            peaks[bucket] = maxOf(peaks[bucket], peak)
        }
        frame++
    }
    return frame
}
