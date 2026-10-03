package com.videoeditorpro.android.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import xiaoying.engine.base.IQSessionStateListener
import xiaoying.engine.base.QDisplayContext
import xiaoying.engine.base.QRange
import xiaoying.engine.base.QSession
import xiaoying.engine.base.QSessionState
import xiaoying.engine.base.QSessionStream
import xiaoying.engine.base.QSessionStreamOpenParam
import xiaoying.engine.producer.QProducer
import xiaoying.engine.producer.QProducerProperty
import xiaoying.engine.storyboard.QStoryboard
import xiaoying.utils.QSize
import java.io.File

/**
 * Production video exporter powered by native QProducer.
 * Hooks real JNI encoding frame callbacks and registers completed MP4 into system MediaStore gallery.
 */
class ExportEngine {
    private val TAG = "ExportEngine"

    private var activeProducer: QProducer? = null
    private var activeStream: QSessionStream? = null
    private var isCancelled = false

    fun export(
        context: Context,
        storyboard: QStoryboard,
        outputPath: String,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080,
        bitrateBps: Int = 8_000_000,
        onProgress: (percent: Int) -> Unit,
        onComplete: (success: Boolean, savedUri: Uri?, filePath: String) -> Unit
    ) {
        val eng = EngineCore.getEngine() ?: run {
            onComplete(false, null, "")
            return
        }

        isCancelled = false
        val mainHandler = Handler(Looper.getMainLooper())

        try {
            val producer = QProducer()

            // 1. Hook native IQSessionStateListener to receive exact encoding callbacks
            val stateListener = object : IQSessionStateListener {
                override fun onSessionStatus(state: QSessionState): Int {
                    if (isCancelled) return 0

                    val dur = state.duration.coerceAtLeast(1)
                    val cur = state.currentTime
                    val pct = ((cur.toFloat() / dur.toFloat()) * 100f).toInt().coerceIn(0, 100)

                    Log.d(TAG, "Export status: ${state.status}, cur=$cur, dur=$dur, err=${state.errorCode}, pct=$pct")

                    if (state.errorCode != 0) {
                        Log.e(TAG, "Native export reported error: ${state.errorCode}")
                        mainHandler.post {
                            cleanup()
                            onComplete(false, null, outputPath)
                        }
                        return 0
                    }

                    mainHandler.post { onProgress(pct) }

                    if (state.status == QSession.STATUS_STOPPED || cur >= dur) {
                        mainHandler.post {
                            cleanup()
                            // Register to MediaStore
                            val galleryUri = saveToGallery(context, File(outputPath))
                            onProgress(100)
                            onComplete(true, galleryUri, outputPath)
                        }
                    }
                    return 0
                }
            }

            producer.init(eng, stateListener)

            // 2. Setup export stream
            val exportStream = QSessionStream()
            val param = QSessionStreamOpenParam().apply {
                mFps = 30
                mFrameSize = QSize(targetWidth, targetHeight)
                mRenderTargetSize = QSize(targetWidth, targetHeight)
                mResampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
                mDecoderUsageType = QSessionStream.DECODER_USAGE_TYPE_AUTO
            }

            val openRet = exportStream.open(QSessionStream.SOURCE_TYPE_STORYBOARD, storyboard, param)
            Log.d(TAG, "Export stream open: $openRet")

            // 3. Configure producer properties
            val prop = QProducerProperty().apply {
                destFile = outputPath
                fileFormat = 2 // MP4 container
                videoFormat = 4 // H.264
                audioFormat = 4 // AAC
                videoFrameRate = 30
                videoBitrate = bitrateBps
                encoderType = QProducerProperty.ENCODER_TYPE_AUTO
                range = QRange(0, -1)
            }

            producer.setProperty(QProducer.PROP_PARAM, prop)
            producer.activeStream(exportStream)

            activeProducer = producer
            activeStream = exportStream

            val startRet = producer.start()
            Log.d(TAG, "Producer start return: $startRet")

            if (startRet != 0) {
                cleanup()
                onComplete(false, null, "")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Export failed with exception", e)
            cleanup()
            onComplete(false, null, "")
        }
    }

    fun cancel() {
        isCancelled = true
        cleanup()
    }

    private fun cleanup() {
        try {
            activeProducer?.stop()
            activeProducer?.deactiveStream()
            activeProducer?.unInit()
            activeProducer = null
            activeStream?.close()
            activeStream = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error cleaning up export producer", e)
        }
    }

    /**
     * Publishes the rendered MP4 video file to Android's system MediaStore.
     */
    fun saveToGallery(context: Context, videoFile: File): Uri? {
        if (!videoFile.exists() || videoFile.length() == 0L) return null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VideoEditorPro")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null

            resolver.openOutputStream(uri)?.use { out ->
                videoFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Log.d(TAG, "Saved video to gallery uri: $uri")
            uri
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving to MediaStore", e)
            null
        }
    }
}
