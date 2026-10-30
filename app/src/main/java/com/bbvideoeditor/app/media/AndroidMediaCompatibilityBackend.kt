package com.bbvideoeditor.app.media

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

internal fun MediaFormat.optionalInt(key: String): Int? = runCatching { if (containsKey(key)) getInteger(key) else null }.getOrNull()
internal fun MediaFormat.optionalLong(key: String): Long? = runCatching { if (containsKey(key)) getLong(key) else null }.getOrNull()
internal fun MediaFormat.optionalRate(): Float? = runCatching { getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
    .recoverCatching { getFloat(MediaFormat.KEY_FRAME_RATE) }.getOrNull()?.takeIf { it.isFinite() && it > 0 }
internal fun MediaFormat.enrich(media: MediaReference): MediaReference = media.copy(
    codecProfile = optionalInt(MediaFormat.KEY_PROFILE)?.takeIf { it >= 0 }, codecLevel = optionalInt(MediaFormat.KEY_LEVEL)?.takeIf { it >= 0 },
    colorStandard = optionalInt(MediaFormat.KEY_COLOR_STANDARD), colorRange = optionalInt(MediaFormat.KEY_COLOR_RANGE),
    // Only explicit platform metadata is evidence; Main10 profile alone does not prove 10-bit samples.
    bitDepth = (optionalInt("bit-depth-luma") ?: optionalInt("bit-depth"))?.takeIf { it in 8..16 })

@androidx.annotation.OptIn(UnstableApi::class)
class AndroidMediaCompatibilityBackend(context: Context, private val budget: com.bbvideoeditor.app.playback.EngineResourceBudget) : CompatibilityBackend {
    private val context = context.applicationContext
    override val deviceBackend = compatibilityHash("${Build.FINGERPRINT}|${Build.VERSION.SDK_INT}|${Build.MANUFACTURER}|${Build.MODEL}|media3-1.12.0-alpha02|compat-4")
    val liveCodecs = AtomicInteger()
    val liveExtractors = AtomicInteger()
    val liveReaders = AtomicInteger()
    private data class Track(val descriptor: SourceFormatDescriptor, val format: MediaFormat, val index: Int)

    private fun open(source: MediaReference, extractor: MediaExtractor): Track {
        extractor.setDataSource(context, Uri.parse(source.uri), null)
        var video: MediaFormat? = null; var index = -1; var duration = 0L; var hasAudio = false
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i); val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/") && video == null) { video = format; index = i }
            if (mime.startsWith("audio/")) hasAudio = true
            duration = maxOf(duration, format.optionalLong(MediaFormat.KEY_DURATION) ?: 0L)
        }
        val format = video ?: throw sourceFailure(source, MediaFailureKind.MALFORMED, "No video track")
        val width = format.optionalInt(MediaFormat.KEY_WIDTH) ?: 0; val height = format.optionalInt(MediaFormat.KEY_HEIGHT) ?: 0
        if (width <= 0 || height <= 0 || duration <= 0) throw sourceFailure(source, MediaFailureKind.MALFORMED, "Invalid video dimensions/duration")
        val rate = format.optionalRate()
        val uri = Uri.parse(source.uri)
        var size = source.sizeBytes
        val version = if (uri.scheme == "file") {
            val file = File(requireNotNull(uri.path)); size = file.length(); "${file.lastModified()}:$size"
        } else runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use {
                if (!it.moveToFirst()) return@use "unknown"
                val sizeColumn = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !it.isNull(sizeColumn)) size = it.getLong(sizeColumn)
                val modified = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (modified >= 0 && !it.isNull(modified)) "${it.getLong(modified)}:$size" else "unknown:$size"
            } ?: "unknown:$size"
        }.getOrDefault("unknown:$size")
        val digest = MessageDigest.getInstance("SHA-256")
        for (key in listOf("csd-0", "csd-1", "csd-2", MediaFormat.KEY_HDR_STATIC_INFO)) {
            val bytes = runCatching { format.getByteBuffer(key)?.duplicate() }.getOrNull() ?: continue
            val bounded = ByteArray(minOf(bytes.remaining(), 65_536)); bytes.get(bounded); digest.update(bounded)
        }
        extractor.selectTrack(index)
        val encrypted = if (extractor.drmInitData != null || extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) true else null
        val media = format.enrich(source.copy(width = width, height = height, durationUs = duration,
            rotationDegrees = format.optionalInt(MediaFormat.KEY_ROTATION) ?: source.rotationDegrees,
            frameRate = rate ?: 0f, frameRateProvenance = if (rate != null) FrameRateProvenance.TRACK_DECLARED else FrameRateProvenance.UNKNOWN,
            codecMimeType = format.getString(MediaFormat.KEY_MIME).orEmpty(), colorTransfer = format.optionalInt(MediaFormat.KEY_COLOR_TRANSFER) ?: 0,
            sizeBytes = size, hasAudio = hasAudio))
        return Track(SourceFormatDescriptor(media, version, digest.digest().joinToString("") { "%02x".format(it) },
            encrypted, format.containsKey(MediaFormat.KEY_HDR_STATIC_INFO)), format, index)
    }

    override suspend fun inspect(media: MediaReference): SourceEvidence = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            val track = open(media, extractor); val format = track.format; val source = track.descriptor.media
            val mime = source.codecMimeType
            var mimeQueryErrors = 0
            val infos = try { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.toList().filter { info ->
                runCatching { !info.isEncoder && info.supportedTypes.any { type -> type.equals(mime, true) } }
                    .getOrElse { mimeQueryErrors++; false }
            } }
            catch (e: Exception) { return@withContext SourceEvidence(track.descriptor, emptyList(), DeclaredSupport.QUERY_ERROR, queryError = e.javaClass.simpleName) }
            // Match the pinned renderer's public candidate order using the extracted CSD/color/codec Format.
            val preferred = runCatching {
                val full = MediaFormatUtil.createFormatFromMediaFormat(format)
                MediaCodecUtil.getDecoderInfosSortedByFullFormatSupport(context,
                    MediaCodecUtil.getDecoderInfosSoftMatch(MediaCodecSelector.DEFAULT, full, false, false), full).firstOrNull()?.name
            }.getOrNull()
            val candidates = infos.map { info ->
                ensureActive()
                try {
                    val caps = info.getCapabilitiesForType(mime); val video = caps.videoCapabilities
                    val size = video?.isSizeSupported(source.width, source.height)
                    val rate = source.frameRate.takeIf { it > 0 }?.let { video?.areSizeAndRateSupported(source.width, source.height, it.toDouble()) }
                    val full = caps.isFormatSupported(format)
                    val support = when { !full || size == false || rate == false -> DeclaredSupport.UNSUPPORTED
                        size == null || rate == null -> DeclaredSupport.UNKNOWN; else -> DeclaredSupport.SUPPORTED }
                    DecoderCandidate(info.name, support, size, rate, full,
                        if (Build.VERSION.SDK_INT >= 29) info.isHardwareAccelerated else null,
                        if (Build.VERSION.SDK_INT >= 29) info.isSoftwareOnly else null,
                        if (Build.VERSION.SDK_INT >= 29) info.isVendor else null,
                        caps.profileLevels.take(32).map { it.profile to it.level })
                } catch (e: Exception) { DecoderCandidate(info.name, DeclaredSupport.QUERY_ERROR, queryError = e.javaClass.simpleName) }
            }.sortedBy { if (it.name == preferred) 0 else 1 }
            val declared = when {
                candidates.isEmpty() -> if(mimeQueryErrors>0) DeclaredSupport.QUERY_ERROR else DeclaredSupport.NO_CANDIDATE
                candidates.any { it.support == DeclaredSupport.SUPPORTED } -> DeclaredSupport.SUPPORTED
                candidates.any { it.support == DeclaredSupport.QUERY_ERROR } -> DeclaredSupport.QUERY_ERROR
                candidates.any { it.support == DeclaredSupport.UNKNOWN } -> DeclaredSupport.UNKNOWN
                else -> DeclaredSupport.UNSUPPORTED
            }
            SourceEvidence(track.descriptor, candidates, declared, preferred)
        } catch (e: MediaCompatibilityException) { throw e }
        catch (e: SecurityException) { throw sourceFailure(media, MediaFailureKind.ACCESS, e.javaClass.simpleName, MediaRetry.RECHECK, e) }
        catch (e: IOException) { throw sourceFailure(media, MediaFailureKind.ACCESS, e.javaClass.simpleName, MediaRetry.RECHECK, e) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw sourceFailure(media, MediaFailureKind.MALFORMED, e.javaClass.simpleName, cause = e) }
        finally { extractor.release() }
    }

    override suspend fun probe(source: SourceFormatDescriptor, candidate: DecoderCandidate): DecoderObservation = withContext(Dispatchers.IO) {
        val media = source.media
        // Functional single-decoder verification has a smaller working set than retained layered preview.
        if (media.width.toLong() * media.height * 6 > budget.decoderWorkingBytes)
            return@withContext DecoderObservation(candidate.name, ProbeStatus.RESOURCE_LIMIT, detail = "Single-decoder probe working budget")
        var codec: MediaCodec? = null; var reader: ImageReader? = null; var extractor: MediaExtractor? = null
        var started = false; var opened = false; var samples = 0
        try {
            extractor = MediaExtractor(); liveExtractors.incrementAndGet()
            val track = open(media, extractor); opened = true
            if (track.descriptor.fingerprint(deviceBackend) != source.fingerprint(deviceBackend))
                return@withContext DecoderObservation(candidate.name, ProbeStatus.ACCESS_FAILED, detail = "Source changed during preparation")
            if (source.encrypted == true) return@withContext DecoderObservation(candidate.name, ProbeStatus.CONFIGURATION_FAILED, detail = "Protected source")
            reader = if (Build.VERSION.SDK_INT >= 29) ImageReader.newInstance(media.width, media.height, ImageFormat.PRIVATE, 2, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
                else ImageReader.newInstance(media.width, media.height, ImageFormat.PRIVATE, 2)
            liveReaders.incrementAndGet()
            codec = MediaCodec.createByCodecName(candidate.name); liveCodecs.incrementAndGet()
            codec.configure(track.format, reader.surface, null, 0); codec.start(); started = true
            val until = SystemClock.elapsedRealtime() + 4_000
            val info = MediaCodec.BufferInfo(); var bytes = 0L; var eos = false; var outputTime: Long? = null
            while (SystemClock.elapsedRealtime() < until) {
                ensureActive()
                if (!eos) {
                    val slot = codec.dequeueInputBuffer(10_000)
                    if (slot >= 0) {
                        val input = requireNotNull(codec.getInputBuffer(slot)); input.clear()
                        val size = if (samples < 96 && bytes < 8L * 1024 * 1024) extractor.readSampleData(input, 0) else -1
                        if (size < 0) { codec.queueInputBuffer(slot, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); eos = true }
                        else { codec.queueInputBuffer(slot, 0, size, extractor.sampleTime.coerceAtLeast(0), 0); samples++; bytes += size; extractor.advance() }
                    }
                }
                val out = codec.dequeueOutputBuffer(info, 10_000)
                if (out >= 0) {
                    val frame = samples > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == 0
                    if (frame) outputTime = info.presentationTimeUs
                    codec.releaseOutputBuffer(out, frame)
                }
                reader.acquireLatestImage()?.use { image ->
                    if (outputTime != null && image.width > 0 && image.height > 0)
                        return@withContext DecoderObservation(candidate.name, ProbeStatus.OUTPUT, samples, outputTime)
                }
                if (out >= 0 && info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                    return@withContext DecoderObservation(candidate.name, ProbeStatus.NO_OUTPUT, samples, detail = "EOS without decoded surface output")
            }
            DecoderObservation(candidate.name, ProbeStatus.TIMEOUT, samples, detail = "Four-second output deadline")
        } catch (e: CancellationException) { throw e }
        catch (e: SecurityException) { DecoderObservation(candidate.name, ProbeStatus.ACCESS_FAILED, samples, cause = e) }
        catch (e: IOException) { DecoderObservation(candidate.name, if(opened) ProbeStatus.CONFIGURATION_FAILED else ProbeStatus.ACCESS_FAILED, samples, cause = e) }
        catch (e: MediaCodec.CodecException) {
            DecoderObservation(candidate.name, if (e.isTransient || e.isRecoverable || e.errorCode == MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE)
                ProbeStatus.RESOURCE_LIMIT else if (started) ProbeStatus.DECODE_FAILED else ProbeStatus.CONFIGURATION_FAILED,
                samples, detail = e.diagnosticInfo, cause = e)
        } catch (e: Exception) { DecoderObservation(candidate.name, if (started) ProbeStatus.DECODE_FAILED else ProbeStatus.CONFIGURATION_FAILED,
            samples, detail = e.javaClass.simpleName, cause = e) }
        finally {
            try {
                codec?.let { try { if (started) runCatching { it.stop() }; it.release() } finally { liveCodecs.decrementAndGet() } }
            } finally {
                try { reader?.let { try { it.close() } finally { liveReaders.decrementAndGet() } } }
                finally { extractor?.let { try { it.release() } finally { liveExtractors.decrementAndGet() } } }
            }
        }
    }
}
