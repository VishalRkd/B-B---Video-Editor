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
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Effect
import com.beatsandbeyond.video_editor.core.domain.model.EffectType
import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private val activeCancellationFlags = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicBoolean>()

    override fun cancel(projectId: String) {
        activeCancellationFlags[projectId]?.set(true)
        Logger.d(TAG, "Requested cancellation for project: $projectId")
    }

    override fun export(project: Project, config: ExportConfig): Flow<ExportProgress> = flow {
        val cancellationFlag = java.util.concurrent.atomic.AtomicBoolean(false)
        activeCancellationFlags[project.id] = cancellationFlag
        val startTime = System.currentTimeMillis()
        emit(ExportProgress.Started)

        val outputFile = createOutputFile(config)
        var muxer: MediaMuxer? = null
        try {
            val primaryVideo = project.timeline.primaryVideoTrack
            val clips = primaryVideo?.clips ?: emptyList()
            if (clips.isEmpty()) {
                emit(ExportProgress.Failed(IllegalStateException("No video clips to export")))
                return@flow
            }

            val videoTrack = project.timeline.primaryVideoTrack
            val videoClips = videoTrack?.clips ?: emptyList()

            val audioTrack = project.timeline.tracks.firstOrNull { it.type == TrackType.AUDIO }
            val audioClips = audioTrack?.clips ?: emptyList()

            muxer = MediaMuxer(outputFile.absolutePath, MUXER_OUTPUT_MPEG_4)
            val totalDurationMs = project.durationMs.coerceAtLeast(1L)

            // Check if any clip actually has audio
            var hasAudio = false
            for (clip in videoClips) {
                val assetResult = assetRepository.getAssetById(clip.assetId)
                if (assetResult is AppResult.Success) {
                    if (hasAudioTrack(Uri.parse(assetResult.data.mediaUri))) {
                        hasAudio = true
                        break
                    }
                }
            }
            if (!hasAudio) {
                for (clip in audioClips) {
                    val assetResult = assetRepository.getAssetById(clip.assetId)
                    if (assetResult is AppResult.Success) {
                        if (hasAudioTrack(Uri.parse(assetResult.data.mediaUri))) {
                            hasAudio = true
                            break
                        }
                    }
                }
            }

            var audioMuxerTrackIndex = -1
            if (hasAudio) {
                val audioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                    setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                }
                audioMuxerTrackIndex = muxer.addTrack(audioFormat)
            }

            // ── Video transcode ──
            val videoTrackIndex = transcodeVideoClips(
                timeline = project.timeline,
                config = config,
                muxer = muxer,
                totalDurationMs = totalDurationMs,
                hasAudio = audioMuxerTrackIndex >= 0,
                onProgress = { fraction, frameMs -> emit(ExportProgress.InProgress(fraction, frameMs)) },
                isCancelled = { cancellationFlag.get() },
            )

            // ── Audio decoding, mixing, and encoding ──
            if (audioMuxerTrackIndex >= 0) {
                emit(ExportProgress.InProgress(0.9f, totalDurationMs))
                
                val totalSamples = ((totalDurationMs * 44100) / 1000).toInt() * 2
                val masterPcm = ShortArray(totalSamples)
                
                for (clip in videoClips) {
                    if (cancellationFlag.get()) break
                    val assetResult = assetRepository.getAssetById(clip.assetId)
                    if (assetResult is AppResult.Success) {
                        val uri = Uri.parse(assetResult.data.mediaUri)
                        val pcm = decodeAudioToPcm(
                            uri, clip.trimStartMs, clip.trimEndMs,
                            clip.speedFactor, clip.volume, 44100, 2
                        )
                        if (pcm != null) {
                            mixPcmIntoMaster(masterPcm, pcm, clip.timelinePositionMs)
                        }
                    }
                }
                
                for (clip in audioClips) {
                    if (cancellationFlag.get()) break
                    val assetResult = assetRepository.getAssetById(clip.assetId)
                    if (assetResult is AppResult.Success) {
                        val uri = Uri.parse(assetResult.data.mediaUri)
                        val pcm = decodeAudioToPcm(
                            uri, clip.trimStartMs, clip.trimEndMs,
                            clip.speedFactor, clip.volume, 44100, 2
                        )
                        if (pcm != null) {
                            mixPcmIntoMaster(masterPcm, pcm, clip.timelinePositionMs)
                        }
                    }
                }
                
                encodeAndMuxAudio(masterPcm, muxer, audioMuxerTrackIndex) { cancellationFlag.get() }
            }

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
            if (cancellationFlag.get()) emit(ExportProgress.Cancelled) else emit(ExportProgress.Failed(e))
        } finally {
            activeCancellationFlags.remove(project.id)
        }
    }.flowOn(Dispatchers.Default)

    // ── Video transcode (decoder → encoder surface) ──────────────────────────

    private fun loadOverlayBitmap(uriString: String): android.graphics.Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to load overlay image: $uriString")
            null
        }
    }

    private fun renderTextToBitmap(
        text: String,
        textColor: Int,
        textSize: Float,
        positionX: Float,
        positionY: Float,
        width: Int,
        height: Int
    ): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val x = positionX * width
        val y = positionY * height - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, y, paint)
        return bitmap
    }

    private fun getAudioTrackFormat(sourceUri: Uri): MediaFormat? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, sourceUri, null)
            val audioIdx = (0 until extractor.trackCount).firstOrNull { idx ->
                extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            audioIdx?.let { extractor.getTrackFormat(it) }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to extract audio track format: ${e.message}")
            null
        } finally {
            extractor.release()
        }
    }

    private suspend fun transcodeVideoClips(
        timeline: Timeline,
        config: ExportConfig,
        muxer: MediaMuxer,
        totalDurationMs: Long,
        hasAudio: Boolean,
        onProgress: suspend (Float, Long) -> Unit,
        isCancelled: () -> Boolean,
    ): Int {
        val clips = timeline.primaryVideoTrack?.clips ?: emptyList()
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

        var renderer: GLFilterRenderer? = null
        try {
            renderer = GLFilterRenderer(encoderSurface, outWidth, outHeight)
            var muxerTrackIndex = -1
            var muxerStarted = false
            var runningPresentationTimeOffsetUs = 0L
            var lastProgressEmit = 0L

            val bufferInfo = MediaCodec.BufferInfo()

            suspend fun drainEncoder() {
                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val encBufId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        encBufId == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable = false
                        encBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = encoder.outputFormat
                            muxerTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        encBufId >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(encBufId)
                            if (encodedData != null && bufferInfo.size > 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                if (muxerStarted && muxerTrackIndex >= 0) {
                                    muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(encBufId, false)
                            
                            val now = System.currentTimeMillis()
                            if (now - lastProgressEmit > 100) {
                                lastProgressEmit = now
                                val frameMs = bufferInfo.presentationTimeUs / 1000L
                                val frac = frameMs.toFloat() / totalDurationMs
                                onProgress(frac.coerceIn(0f, 1f), frameMs)
                            }
                        }
                    }
                }
            }

            for (clip in clips) {
                if (isCancelled()) break

                val assetResult = assetRepository.getAssetById(clip.assetId)
                if (assetResult !is AppResult.Success) {
                    Logger.w(TAG, "Asset not found for clip: ${clip.id}")
                    continue
                }
                val sourceUri = Uri.parse(assetResult.data.mediaUri)

                val speedFactor = clip.speedFactor.coerceAtLeast(0.1f)
                val extractor = MediaExtractor()
                var decoder: MediaCodec? = null

                try {
                    extractor.setDataSource(context, sourceUri, null)
                    val videoIdx = (0 until extractor.trackCount).firstOrNull { idx ->
                        extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                    } ?: continue

                    extractor.selectTrack(videoIdx)
                    val inputFormat = extractor.getTrackFormat(videoIdx)

                    decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
                    decoder.configure(inputFormat, renderer.decoderSurface, null, 0)
                    decoder.start()

                    val trimStartUs = clip.trimStartMs * 1000L
                    val trimEndUs = clip.trimEndMs * 1000L
                    extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                    var inputDone = false
                    var decoderOutputDone = false

                    while (!decoderOutputDone && !isCancelled()) {
                        if (!inputDone) {
                            val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                            if (inputBufferIndex >= 0) {
                                val inBuf = decoder.getInputBuffer(inputBufferIndex)
                                if (inBuf != null) {
                                    val sampleSize = extractor.readSampleData(inBuf, 0)
                                    val sampleTime = extractor.sampleTime
                                    if (sampleSize < 0 || sampleTime >= trimEndUs) {
                                        decoder.queueInputBuffer(
                                            inputBufferIndex,
                                            0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                        )
                                        inputDone = true
                                    } else {
                                        val flags = extractor.sampleFlags
                                        decoder.queueInputBuffer(
                                            inputBufferIndex,
                                            0, sampleSize, sampleTime, flags,
                                        )
                                        extractor.advance()
                                    }
                                }
                            }
                        }

                        var decoderOutputAvailable = true
                        while (decoderOutputAvailable && !decoderOutputDone) {
                            val outBufId = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                            when {
                                outBufId == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable = false
                                outBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                                outBufId >= 0 -> {
                                    val presentationTimeUs = bufferInfo.presentationTimeUs
                                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                        decoderOutputDone = true
                                        decoder.releaseOutputBuffer(outBufId, false)
                                    } else if (presentationTimeUs < trimStartUs) {
                                        decoder.releaseOutputBuffer(outBufId, false)
                                    } else {
                                        val relativeUs = presentationTimeUs - trimStartUs
                                        val speedAdjustedUs = (relativeUs / speedFactor).toLong()
                                        val outputPtsUs = runningPresentationTimeOffsetUs + speedAdjustedUs

                                        val frameTimelineTimeMs = outputPtsUs / 1000L
                                        val activeEffects = mutableListOf<Effect>()
                                        activeEffects.addAll(clip.effects)
                                        val otherTracks = timeline.tracks.filter { it.type == TrackType.EFFECT || it.type == TrackType.ADJUSTMENT }
                                        for (track in otherTracks) {
                                            for (otherClip in track.clips) {
                                                if (frameTimelineTimeMs >= otherClip.timelinePositionMs && frameTimelineTimeMs <= otherClip.timelineEndMs) {
                                                    activeEffects.addAll(otherClip.effects)
                                                }
                                             }
                                         }
                                         var brightnessVal = 1.0f
                                        var contrastVal = 1.0f
                                        var saturationVal = 1.0f
                                        var vintageVal = 0.0f
                                        var vignetteVal = 0.0f

                                        when (val f = clip.filter) {
                                            is VideoFilter.Brightness -> brightnessVal = f.value + 1.0f
                                            is VideoFilter.Contrast -> contrastVal = f.value
                                            is VideoFilter.Saturation -> saturationVal = f.value
                                            is VideoFilter.Vintage -> vintageVal = f.intensity
                                            is VideoFilter.Vignette -> vignetteVal = f.radius
                                            else -> {}
                                        }

                                        for (effect in activeEffects) {
                                            when (effect.type) {
                                                EffectType.BRIGHTNESS -> brightnessVal *= (effect.parameters["intensity"] ?: 1.0f)
                                                EffectType.CONTRAST -> contrastVal *= (effect.parameters["intensity"] ?: 1.0f)
                                                EffectType.SATURATION -> saturationVal *= (effect.parameters["intensity"] ?: 1.0f)
                                                else -> {}
                                            }
                                        }
                                        renderer.brightness = brightnessVal
                                        renderer.contrast = contrastVal
                                        renderer.saturation = saturationVal
                                        renderer.vintage = vintageVal
                                        renderer.vignette = vignetteVal

                                        decoder.releaseOutputBuffer(outBufId, true)
                                        renderer.awaitFrame(2500L)
                                        renderer.drawFrame()

                                        // Draw Overlay tracks
                                        val activeOverlayTracks = timeline.tracks.filter { it.type == TrackType.OVERLAY }
                                        for (track in activeOverlayTracks) {
                                            for (overlayClip in track.clips) {
                                                if (frameTimelineTimeMs >= overlayClip.timelinePositionMs && frameTimelineTimeMs <= overlayClip.timelineEndMs) {
                                                    val overlayAssetResult = assetRepository.getAssetById(overlayClip.assetId)
                                                    if (overlayAssetResult is AppResult.Success) {
                                                        val overlayBitmap = loadOverlayBitmap(overlayAssetResult.data.mediaUri)
                                                        if (overlayBitmap != null) {
                                                             renderer.drawOverlay(overlayBitmap, 0f, 0f, 1f, 1f)
                                                             overlayBitmap.recycle()
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Draw Text tracks
                                        val activeTextTracks = timeline.tracks.filter { it.type == TrackType.TEXT }
                                        for (track in activeTextTracks) {
                                            for (textClip in track.clips) {
                                                if (frameTimelineTimeMs >= textClip.timelinePositionMs && frameTimelineTimeMs <= textClip.timelineEndMs) {
                                                    val style = textClip.textStyle
                                                    if (style != null) {
                                                        val textBitmap = renderTextToBitmap(
                                                            text = style.text,
                                                            textColor = style.color,
                                                            textSize = style.fontSize,
                                                            positionX = style.positionX,
                                                            positionY = style.positionY,
                                                            width = outWidth,
                                                            height = outHeight
                                                        )
                                                        renderer.drawOverlay(textBitmap, 0f, 0f, 2f, 2f)
                                                        textBitmap.recycle()
                                                    }
                                                }
                                            }
                                        }

                                        renderer.swapBuffers()
                                        renderer.setPresentationTime(outputPtsUs * 1000L)
                                        decoderOutputAvailable = false
                                    }
                                }
                            }
                        }

                        drainEncoder()
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error transcoding clip ${clip.id}", e)
                    throw e
                } finally {
                    decoder?.stop()
                    decoder?.release()
                    extractor.release()
                }

                val clipDurationUs = ((clip.trimEndMs - clip.trimStartMs) * 1000L / speedFactor).toLong()
                runningPresentationTimeOffsetUs += clipDurationUs
            }

            if (!isCancelled()) {
                encoder.signalEndOfInputStream()
            }

            var encoderOutputDone = false
            while (!encoderOutputDone && !isCancelled()) {
                val encBufId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    encBufId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        delay(10)
                    }
                    encBufId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = encoder.outputFormat
                        muxerTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    encBufId >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(encBufId)
                        if (encodedData != null && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            if (muxerStarted && muxerTrackIndex >= 0) {
                                muxer.writeSampleData(muxerTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(encBufId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderOutputDone = true
                        }
                        
                        val now = System.currentTimeMillis()
                        if (now - lastProgressEmit > 100) {
                            lastProgressEmit = now
                            val frameMs = bufferInfo.presentationTimeUs / 1000L
                            val frac = frameMs.toFloat() / totalDurationMs
                            onProgress(frac.coerceIn(0f, 1f), frameMs)
                        }
                    }
                }
            }
            return muxerTrackIndex
        } finally {
            renderer?.release()
            encoder.stop()
            encoder.release()
        }
    }

    // ── Audio pass-through (all audio clips) ─────────────────────────────────

    private fun hasAudioTrack(uri: Uri): Boolean {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            return findAudioTrack(extractor) >= 0
        } catch (e: Exception) {
            return false
        } finally {
            extractor.release()
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        return (0 until extractor.trackCount).firstOrNull { idx ->
            extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: -1
    }

    private fun decodeAudioToPcm(
        uri: Uri,
        trimStartMs: Long,
        trimEndMs: Long,
        speedFactor: Float,
        volume: Float,
        targetSampleRate: Int,
        targetChannels: Int
    ): ShortArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val audioIdx = findAudioTrack(extractor)
            if (audioIdx < 0) return null
            extractor.selectTrack(audioIdx)
            val format = extractor.getTrackFormat(audioIdx)
            
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            
            val trimStartUs = trimStartMs * 1000L
            val trimEndUs = trimEndMs * 1000L
            extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            
            val bufferInfo = MediaCodec.BufferInfo()
            val pcmBytes = java.io.ByteArrayOutputStream()
            
            var inputDone = false
            var outputDone = false
            
            while (!outputDone) {
                if (!inputDone) {
                    val inputBufIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufIdx >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIdx)!!
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        val sampleTime = extractor.sampleTime
                        if (sampleSize < 0 || sampleTime >= trimEndUs) {
                            decoder.queueInputBuffer(inputBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputBufIdx, 0, sampleSize, sampleTime, extractor.sampleFlags)
                            extractor.advance()
                        }
                    }
                }
                
                var outAvailable = true
                while (outAvailable && !outputDone) {
                    val outBufIdx = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outBufIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> outAvailable = false
                        outBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                        outBufIdx >= 0 -> {
                            val outBuf = decoder.getOutputBuffer(outBufIdx)!!
                            if (bufferInfo.size > 0 && bufferInfo.presentationTimeUs >= trimStartUs) {
                                val chunk = ByteArray(bufferInfo.size)
                                outBuf.position(bufferInfo.offset)
                                outBuf.get(chunk)
                                pcmBytes.write(chunk)
                            }
                            decoder.releaseOutputBuffer(outBufIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                    }
                }
            }
            decoder.stop()
            decoder.release()
            
            val rawBytes = pcmBytes.toByteArray()
            val rawShorts = ShortArray(rawBytes.size / 2)
            ByteBuffer.wrap(rawBytes)
                .order(java.nio.ByteOrder.nativeOrder())
                .asShortBuffer()
                .get(rawShorts)
                
            return processAndResamplePcm(
                rawShorts, sampleRate, channelCount,
                targetSampleRate, targetChannels, speedFactor, volume
            )
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to decode audio to PCM for URI $uri: ${e.message}")
            return null
        } finally {
            extractor.release()
        }
    }

    private fun processAndResamplePcm(
        shorts: ShortArray,
        srcSampleRate: Int,
        srcChannels: Int,
        dstSampleRate: Int,
        dstChannels: Int,
        speedFactor: Float,
        volume: Float
    ): ShortArray {
        val speedAdjustedRate = srcSampleRate * speedFactor
        val ratio = speedAdjustedRate / dstSampleRate
        
        val srcFrames = shorts.size / srcChannels
        val dstFrames = (srcFrames / ratio).toInt().coerceAtLeast(1)
        val dstShorts = ShortArray(dstFrames * dstChannels)
        
        for (i in 0 until dstFrames) {
            val srcFrameIdx = (i * ratio).toInt().coerceIn(0, srcFrames - 1)
            
            val val1: Short
            val val2: Short
            if (srcChannels == 1) {
                if (srcFrameIdx < shorts.size) {
                    val1 = shorts[srcFrameIdx]
                    val2 = val1
                } else {
                    val1 = 0
                    val2 = 0
                }
            } else {
                if (srcFrameIdx * 2 + 1 < shorts.size) {
                    val1 = shorts[srcFrameIdx * 2]
                    val2 = shorts[srcFrameIdx * 2 + 1]
                } else {
                    val1 = 0
                    val2 = 0
                }
            }
            
            val scaledVal1 = (val1 * volume).toInt().coerceIn(-32768, 32767).toShort()
            val scaledVal2 = (val2 * volume).toInt().coerceIn(-32768, 32767).toShort()
            
            if (dstChannels == 1) {
                dstShorts[i] = ((scaledVal1.toInt() + scaledVal2.toInt()) / 2).toShort()
            } else {
                dstShorts[i * 2] = scaledVal1
                dstShorts[i * 2 + 1] = scaledVal2
            }
        }
        return dstShorts
    }

    private fun mixPcmIntoMaster(master: ShortArray, src: ShortArray, startMs: Long) {
        val destStartSample = ((startMs * 44100) / 1000).toInt() * 2
        val lengthToMix = Math.min(src.size, master.size - destStartSample)
        for (i in 0 until lengthToMix) {
            if (destStartSample + i in master.indices) {
                val sum = master[destStartSample + i] + src[i]
                master[destStartSample + i] = sum.coerceIn(-32768, 32767).toShort()
            }
        }
    }

    private fun encodeAndMuxAudio(
        pcm: ShortArray,
        muxer: MediaMuxer,
        audioTrackIdx: Int,
        isCancelled: () -> Boolean
    ) {
        val encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        
        var pcmOffset = 0
        val pcmBuffer = ByteBuffer.allocate(pcm.size * 2).apply {
            order(java.nio.ByteOrder.nativeOrder())
            asShortBuffer().put(pcm)
        }
        
        var presentationTimeUs = 0L
        
        while (!outputDone && !isCancelled()) {
            if (!inputDone) {
                val inputBufIdx = encoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufIdx >= 0) {
                    val inputBuf = encoder.getInputBuffer(inputBufIdx)!!
                    inputBuf.clear()
                    
                    val remainingBytes = pcmBuffer.capacity() - pcmOffset
                    val bytesToCopy = Math.min(inputBuf.capacity(), remainingBytes)
                    
                    if (bytesToCopy <= 0) {
                        encoder.queueInputBuffer(inputBufIdx, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        pcmBuffer.position(pcmOffset)
                        val slice = pcmBuffer.slice().limit(bytesToCopy) as ByteBuffer
                        inputBuf.put(slice)
                        
                        encoder.queueInputBuffer(inputBufIdx, 0, bytesToCopy, presentationTimeUs, 0)
                        
                        val framesCopied = bytesToCopy / 4
                        presentationTimeUs += (framesCopied * 1000000L) / 44100
                        pcmOffset += bytesToCopy
                    }
                }
            }
            
            var outAvailable = true
            while (outAvailable && !outputDone) {
                val outBufIdx = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outBufIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> outAvailable = false
                    outBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                    outBufIdx >= 0 -> {
                        val outBuf = encoder.getOutputBuffer(outBufIdx)!!
                        if (bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrackIdx, outBuf, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outBufIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        }
        try {
            encoder.stop()
        } catch (e: Exception) {}
        encoder.release()
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
        } else {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                @Suppress("DEPRECATION")
                put(MediaStore.Video.Media.DATA, file.absolutePath)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            resolver.insert(collection, values)
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Logger.d(TAG, "Legacy MediaStore registration for path $path: $uri")
            }
        }
    }

    /** Wrapper to avoid a hard API dependency on [android.media.MediaCodecInfo] in the import block. */
    private object MediaCodecInfoWrapper {
        const val COLOR_FORMAT_SURFACE = android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    }
}
