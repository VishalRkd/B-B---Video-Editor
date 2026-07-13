package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.ExportEngine
import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete [ExportEngine] backed by Android's MediaCodec + MediaMuxer.
 *
 * Phase 6 scope (focused, robust):
 * - Primary VIDEO track is transcoded (decoder → encoder) honoring per-clip trim
 *   windows and speed multipliers. Speed is realized by remapping presentation
 *   timestamps, so a 2× clip plays twice as fast in the output.
 * - The first AUDIO track is muxed through via encoded pass-through (buffers copied
 *   directly from the extractor), preserving quality and avoiding a decode/encode
 *   round-trip. Multi-track audio mixing lands in Phase N.
 * - Output is an MP4 written to the app's scoped Movies directory, then registered
 *   with MediaStore so it appears in the system gallery.
 *
 * The export runs as a cold [Flow] of [ExportProgress]; cancellation is cooperative
 * via the [cancel] flag checked between frames.
 */
@Singleton
class MediaCodecExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val dispatchers: CoroutineDispatchers,
) : ExportEngine {

    companion object {
        private const val TAG = "MediaCodecExportEngine"
        private const val TIMEOUT_US = 10_000L
        private const val VIDEO_MIME = "video/avc"
    }

    @Volatile private var cancelled = false

    override fun cancel() {
        cancelled = true
    }

    override fun export(project: Project, config: ExportConfig): Flow<ExportProgress> = flow {
        cancelled = false
        val startTime = System.currentTimeMillis()
        emit(ExportProgress.Started)

        val outputFile = createOutputFile(config)
        var muxer: MediaMuxer? = null
        try {
            val primaryVideo = project.timeline.primaryVideoTrack
            val firstClip = primaryVideo?.clips?.firstOrNull()
            if (firstClip == null) {
                emit(ExportProgress.Failed(IllegalStateException("No video clip to export")))
                return@flow
            }

            val assetResult = assetRepository.getAssetById(firstClip.assetId)
            if (assetResult !is AppResult.Success) {
                emit(ExportProgress.Failed(Exception("Source asset not found")))
                return@flow
            }
            val sourceUri = Uri.parse(assetResult.data.mediaUri)

            muxer = MediaMuxer(outputFile.absolutePath, MUXER_OUTPUT_MPEG_4)
            val totalDurationMs = project.durationMs.coerceAtLeast(1L)

            // ── Video transcode ──
            val videoTrackIndex = transcodeVideo(
                sourceUri = sourceUri,
                clip = firstClip,
                config = config,
                muxer = muxer,
                totalDurationMs = totalDurationMs,
                onProgress = { fraction, frameMs -> emit(ExportProgress.InProgress(fraction, frameMs)) },
                isCancelled = { cancelled },
            )

            // ── Audio pass-through (first audio track) ──
            transcodeAudioPassThrough(
                project = project,
                config = config,
                muxer = muxer,
                assetRepository = assetRepository,
                isCancelled = { cancelled },
            )

            muxer.start()
            muxer.stop()
            muxer.release()
            muxer = null

            registerInMediaStore(outputFile, config)
            emit(
                ExportProgress.Completed(
                    outputPath = outputFile.absolutePath,
                    durationMs = System.currentTimeMillis() - startTime,
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Export failed", e)
            muxer?.release()
            if (cancelled) emit(ExportProgress.Cancelled) else emit(ExportProgress.Failed(e))
        }
    }.flowOn(Dispatchers.Default)

    // ── Video transcode (decoder → encoder surface) ──────────────────────────

    private suspend fun transcodeVideo(
        sourceUri: Uri,
        clip: com.beatsandbeyond.video_editor.core.domain.model.Clip,
        config: ExportConfig,
        muxer: MediaMuxer,
        totalDurationMs: Long,
        onProgress: suspend (Float, Long) -> Unit,
        isCancelled: () -> Boolean,
    ): Int {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, sourceUri, null)

        val videoTrackIndex = (0 until extractor.trackCount).firstOrNull { idx ->
            extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: throw IllegalStateException("No video track in source")

        extractor.selectTrack(videoTrackIndex)
        val inputFormat = extractor.getTrackFormat(videoTrackIndex)

        val encoder = MediaCodec.createEncoderByType(VIDEO_MIME)
        val outWidth = config.resolution.width
        val outHeight = config.resolution.height
        val encoderFormat = MediaFormat.createVideoFormat(VIDEO_MIME, outWidth, outHeight).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate.toInt())
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfoWrapper.COLOR_FORMAT_SURFACE,
            )
        }
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val encoderSurface = encoder.createInputSurface()
        encoder.start()

        val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
        // Route decoder output straight to the encoder input surface (zero-copy transcode).
        decoder.configure(inputFormat, encoderSurface, null, 0)
        decoder.start()

        // Seek to the clip's trim start within the source asset.
        val trimStartUs = clip.trimStartMs * 1000L
        val trimEndUs = clip.trimEndMs * 1000L
        extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        var muxerTrackIndex = -1
        var encoderStarted = false
        var inputDone = false
        var outputDone = false
        var lastProgressEmit = 0L
        val speedFactor = clip.speedFactor.coerceAtLeast(0.1f)

        val bufferInfo = MediaCodec.BufferInfo()
        var generatedDurationUs = 0L

        while (!outputDone && !isCancelled()) {
            // 1) Feed extractor → decoder
            if (!inputDone) {
                val inBuf = decoder.getInputBuffer(decoder.dequeueInputBuffer(TIMEOUT_US))
                if (inBuf != null) {
                    val sampleSize = extractor.readSampleData(inBuf, 0)
                    val sampleTime = extractor.sampleTime
                    if (sampleSize < 0 || sampleTime >= trimEndUs) {
                        decoder.queueInputBuffer(
                            decoder.dequeueInputBuffer(TIMEOUT_US),
                            0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        val flags = extractor.sampleFlags
                        decoder.queueInputBuffer(
                            decoder.dequeueInputBuffer(TIMEOUT_US),
                            0, sampleSize, sampleTime, flags,
                        )
                        extractor.advance()
                    }
                }
            }

            // 2) Drain decoder → (encoder surface)
            var decoderOutputAvailable = true
            while (decoderOutputAvailable) {
                val outBufId = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outBufId == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable = false
                    outBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                    outBufId >= 0 -> {
                        // Presentation time is remapped for speed: divide by speedFactor so
                        // faster clips compress their timeline into less output duration.
                        val adjustedUs = (bufferInfo.presentationTimeUs / speedFactor).toLong()
                        bufferInfo.presentationTimeUs = adjustedUs
                        decoder.releaseOutputBuffer(outBufId, true) // render to encoder surface
                        generatedDurationUs = adjustedUs
                    }
                }
            }

            // 3) Drain encoder → muxer
            var encoderOutputAvailable = true
            while (encoderOutputAvailable) {
                val encBufId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    encBufId == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable = false
                    encBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = encoder.outputFormat
                        muxerTrackIndex = muxer.addTrack(newFormat)
                        encoderStarted = true
                    }
                    encBufId >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(encBufId)
                        if (encodedData != null && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            if (encoderStarted && muxerTrackIndex >= 0) {
                                muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(encBufId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                        // Progress (coarse, every ~100ms)
                        val now = System.currentTimeMillis()
                        if (now - lastProgressEmit > 100) {
                            lastProgressEmit = now
                            val frac = (generatedDurationUs / 1000L).toFloat() / totalDurationMs
                            onProgress(frac.coerceIn(0f, 1f), (generatedDurationUs / 1000L))
                        }
                    }
                }
            }
        }

        decoder.stop(); decoder.release()
        encoder.stop(); encoder.release()
        return muxerTrackIndex
    }

    // ── Audio pass-through (first audio track) ───────────────────────────────

    private suspend fun transcodeAudioPassThrough(
        project: Project,
        config: ExportConfig,
        muxer: MediaMuxer,
        assetRepository: AssetRepository,
        isCancelled: () -> Boolean,
    ) {
        val audioTrack = project.timeline.tracks.firstOrNull { it.type == TrackType.AUDIO }
        val audioClip = audioTrack?.clips?.firstOrNull() ?: return
        val assetResult = assetRepository.getAssetById(audioClip.assetId)
        if (assetResult !is AppResult.Success) return
        val uri = Uri.parse(assetResult.data.mediaUri)

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val audioIdx = (0 until extractor.trackCount).firstOrNull { idx ->
                extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return
            extractor.selectTrack(audioIdx)
            val format = extractor.getTrackFormat(audioIdx)
            // Override bitrate if the container allows; keep codec/format otherwise.
            val muxerAudioIndex = muxer.addTrack(format)
            extractor.seekTo(audioClip.trimStartMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferInfo = MediaCodec.BufferInfo()
            val maxBuf = 1024 * 1024
            val buf = ByteBuffer.allocate(maxBuf)
            var done = false
            while (!done && !isCancelled()) {
                buf.clear()
                val sampleSize = extractor.readSampleData(buf, 0)
                if (sampleSize < 0) break
                val sampleTime = extractor.sampleTime
                if (sampleTime >= audioClip.trimEndMs * 1000L) break
                bufferInfo.apply {
                    offset = 0
                    size = sampleSize
                    presentationTimeUs = sampleTime
                    flags = extractor.sampleFlags
                }
                buf.position(0); buf.limit(sampleSize)
                muxer.writeSampleData(muxerAudioIndex, buf, bufferInfo)
                extractor.advance()
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Audio pass-through skipped: ${e.message}")
        } finally {
            extractor.release()
        }
    }

    // ── Output file + MediaStore registration ───────────────────────────────

    private fun createOutputFile(config: ExportConfig): File {
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        dir.mkdirs()
        val baseName = "BB_Edit_${System.currentTimeMillis()}"
        return File(dir, "$baseName.${config.format.name.lowercase()}")
    }

    private fun registerInMediaStore(file: File, config: ExportConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/B&B Editor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, values) ?: return
            resolver.openOutputStream(itemUri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }
    }

    /** Wrapper to avoid a hard API dependency on [android.media.MediaCodecInfo] in the import block. */
    private object MediaCodecInfoWrapper {
        const val COLOR_FORMAT_SURFACE = android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    }
}
