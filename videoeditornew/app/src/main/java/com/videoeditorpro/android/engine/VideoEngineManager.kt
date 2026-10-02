package com.videoeditorpro.android.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xiaoying.engine.QEngine
import xiaoying.engine.base.QDisplayContext
import xiaoying.engine.base.QRange
import xiaoying.engine.base.QSessionState
import xiaoying.engine.base.QSessionStream
import xiaoying.engine.base.QSessionStreamOpenParam
import xiaoying.engine.clip.QClip
import xiaoying.engine.clip.QMediaSource
import xiaoying.engine.player.QPlayer
import xiaoying.engine.producer.QProducer
import xiaoying.engine.producer.QProducerProperty
import xiaoying.engine.storyboard.QStoryboard
import xiaoying.utils.QPoint
import xiaoying.utils.QRect
import xiaoying.utils.QSize
import java.io.File

object VideoEngineManager {
    private const val TAG = "VideoEngineManager"

    private var engine: QEngine? = null
    private var storyboard: QStoryboard? = null
    private var currentClip: QClip? = null
    private var player: QPlayer? = null
    private var sessionStream: QSessionStream? = null

    private var currentVideoPath: String? = null
    private var videoDurationMs: Int = 0
    private var isPlaying: Boolean = false

    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressListener: ((currentMs: Int, totalMs: Int) -> Unit)? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && player != null) {
                val curTime = player?.curStoryboardMediaTime ?: 0
                if (curTime >= 0) {
                    progressListener?.invoke(curTime, videoDurationMs)
                }
                progressHandler.postDelayed(this, 33)
            }
        }
    }

    /**
     * Initializes native libraries and QEngine.
     */
    fun init(context: Context): Boolean {
        if (engine != null) return true

        val libs = listOf(
            "cesplatform",
            "postprocess",
            "cescommon",
            "cesplatformutils",
            "x264",
            "ffmpeg",
            "asp",
            "cesmediabase",
            "cesliveeditor",
            "cesrenderengine"
        )

        for (lib in libs) {
            try {
                System.loadLibrary(lib)
                Log.d(TAG, "Loaded native library: $lib")
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading $lib: ${e.message}")
            }
        }

        return try {
            val qEng = QEngine()
            
            // Ensure license file is extracted to local storage for direct C access
            val licenseFile = File(context.filesDir, "license.txt")
            try {
                if (!licenseFile.exists() || licenseFile.length() == 0L) {
                    context.assets.open("xiaoying/ini/license.txt").use { input ->
                        licenseFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error copying license: ${e.message}")
            }

            val licenseUri = if (licenseFile.exists()) licenseFile.absolutePath else "assets_android://xiaoying/ini/license.txt"
            val ret = qEng.create(licenseUri)
            Log.d(TAG, "QEngine create return: $ret")

            val cachePath = context.cacheDir.absolutePath
            qEng.setProperty(QEngine.PROP_APP_CONTEXT, context.applicationContext)
            qEng.setProperty(QEngine.PROP_TEMP_PATH, cachePath)
            qEng.setProperty(QEngine.PROP_CONTEXT_FRAMEWORK_VERSION, QEngine.FRAMEWORK_VERSION_SB)
            qEng.setProperty(QEngine.PROP_DEFAULT_PLAYBACK_MUTE, false)
            qEng.setProperty(QEngine.PROP_DEFAULT_PLAYBACK_VOLUME, 100)
            qEng.setProperty(QEngine.PROP_DEFAULT_OUTPUT_VIDEO_FORMAT, 4)
            qEng.setProperty(QEngine.PROP_DEFAULT_OUTPUT_AUDIO_FORMAT, 4)
            qEng.setProperty(QEngine.PROP_DEFAULT_OUTPUT_FILE_FORMAT, 2)
            qEng.setProperty(QEngine.PROP_DEFAULT_RESAMPLE_MODE, 65537)
            qEng.setProperty(QEngine.PROP_MAX_SUPPORT_RESOLUTION, QPoint(4096, 2160))
            qEng.setProperty(QEngine.PROP_STATIC_DURATION, 300000)
            qEng.setProperty(QEngine.PROP_TRIM_TYPE, 0)
            qEng.setProperty(QEngine.PROP_IMAGE_SOURCE_FPS, 25)

            engine = qEng
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Engine init failed", e)
            false
        }
    }

    /**
     * Sets up a new video project session with the given file using QStoryboard + QClip.
     */
    fun openVideo(videoPath: String): Boolean {
        currentVideoPath = videoPath
        val eng = engine ?: return false

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            videoDurationMs = durStr?.toIntOrNull() ?: 0
            retriever.release()

            // 1. Create and initialize QClip from video file
            val mediaSource = QMediaSource(QMediaSource.TYPE_FILE, false, videoPath)
            val clip = QClip()
            val clipInitRet = clip.init(eng, mediaSource)
            Log.d(TAG, "QClip.init result: $clipInitRet, duration: $videoDurationMs ms")
            if (clipInitRet != 0) {
                Log.e(TAG, "QClip.init failed with error: $clipInitRet")
                return false
            }

            // 2. Create and initialize QStoryboard
            val sb = QStoryboard()
            val sbInitRet = sb.init(eng, null)
            Log.d(TAG, "QStoryboard.init result: $sbInitRet")
            if (sbInitRet != 0) {
                Log.e(TAG, "QStoryboard.init failed with error: $sbInitRet")
                return false
            }

            // 3. Insert QClip into QStoryboard at index 0
            val insertRet = sb.insertClip(clip, 0)
            Log.d(TAG, "sb.insertClip result: $insertRet")
            if (insertRet != 0) {
                Log.e(TAG, "sb.insertClip failed with error: $insertRet")
                return false
            }

            // Sync duration if storyboard duration is available
            val sbDur = sb.duration
            if (sbDur > 0) {
                videoDurationMs = sbDur
            }

            currentClip = clip
            storyboard = sb
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "openVideo failed", e)
            return false
        }
    }

    /**
     * Attaches playback to an Android SurfaceView.
     */
    fun attachSurface(surfaceView: SurfaceView, onAttached: () -> Unit) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                setupPlayerWithHolder(holder, surfaceView.width, surfaceView.height)
                onAttached()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                updateDisplayContext(holder, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releasePlayer()
            }
        })
    }

    private fun setupPlayerWithHolder(holder: SurfaceHolder, width: Int, height: Int) {
        val eng = engine ?: return
        val sb = storyboard ?: return

        try {
            releasePlayer()

            val qp = QPlayer()
            val pInitRet = qp.init(eng, null)
            Log.d(TAG, "QPlayer.init result: $pInitRet")

            val w = if (width > 0) width else 1080
            val h = if (height > 0) height else 1920

            val display = QDisplayContext()
            display.surfaceHolder = holder
            display.screenRect = QRect(0, 0, w, h)
            display.resampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            val setDispRet = qp.setDisplayContext(display)
            Log.d(TAG, "QPlayer.setDisplayContext result: $setDispRet")

            val stream = QSessionStream()
            val openParam = QSessionStreamOpenParam()
            openParam.mFps = 30
            openParam.mFrameSize = QSize(w, h)
            openParam.mRenderTargetSize = QSize(w, h)
            openParam.mResampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            openParam.mDecoderUsageType = QSessionStream.DECODER_USAGE_TYPE_AUTO

            Log.d(TAG, "Calling stream.open(SOURCE_TYPE_STORYBOARD, sb, openParam)...")
            val openRet = stream.open(QSessionStream.SOURCE_TYPE_STORYBOARD, sb, openParam)
            Log.d(TAG, "stream.open result: $openRet")

            val activeRet = qp.activeStream(stream, 0, false)
            Log.d(TAG, "qp.activeStream result: $activeRet")

            sessionStream = stream
            player = qp
        } catch (e: Throwable) {
            Log.e(TAG, "setupPlayer error", e)
        }
    }

    private fun updateDisplayContext(holder: SurfaceHolder, width: Int, height: Int) {
        val qp = player ?: return
        try {
            val display = QDisplayContext()
            display.surfaceHolder = holder
            display.screenRect = QRect(0, 0, width, height)
            display.resampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            qp.setDisplayContext(display)
        } catch (e: Throwable) {
            Log.e(TAG, "updateDisplayContext error", e)
        }
    }

    fun play() {
        player?.let {
            it.play()
            isPlaying = true
            progressHandler.post(progressRunnable)
        }
    }

    fun pause() {
        player?.let {
            it.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressRunnable)
        }
    }

    fun isPlaying(): Boolean = isPlaying

    fun seekTo(positionMs: Int) {
        player?.seekTo(positionMs)
        progressListener?.invoke(positionMs, videoDurationMs)
    }

    fun getDuration(): Int = videoDurationMs

    fun getCurrentPosition(): Int = player?.curStoryboardMediaTime ?: 0

    fun setProgressListener(listener: (currentMs: Int, totalMs: Int) -> Unit) {
        progressListener = listener
    }

    /**
     * Trims the clip between startMs and endMs.
     */
    fun trim(startMs: Int, endMs: Int) {
        val clip = currentClip ?: return
        try {
            val length = (endMs - startMs).coerceAtLeast(500)
            val range = QRange(startMs, length)
            clip.setProperty(QClip.PROP_TRIM_RANGE, range)
            storyboard?.applyTrim()
            player?.autoRefreshStream()
            videoDurationMs = length
            seekTo(0)
            Log.d(TAG, "Trim applied: start=$startMs, length=$length")
        } catch (e: Throwable) {
            Log.e(TAG, "trim error", e)
        }
    }

    /**
     * Adjusts playback speed of the current clip (e.g. 0.5f, 1.0f, 1.5f, 2.0f).
     */
    fun setSpeed(speed: Float) {
        val clip = currentClip ?: return
        try {
            clip.setProperty(QClip.PROP_TIME_SCALE, speed)
            player?.autoRefreshStream()
            val sbDur = storyboard?.duration ?: 0
            if (sbDur > 0) {
                videoDurationMs = sbDur
            }
            Log.d(TAG, "Speed set to: $speed")
        } catch (e: Throwable) {
            Log.e(TAG, "setSpeed error", e)
        }
    }

    /**
     * Splits the current clip at splitTimeMs, creating two sequential clips.
     */
    fun splitAt(splitTimeMs: Int): Boolean {
        val sb = storyboard ?: return false
        val clip = currentClip ?: return false
        try {
            val clipIndex = 0
            val newClip = QClip()
            val dupRet = clip.duplicate(newClip)
            if (dupRet != 0) {
                Log.e(TAG, "duplicate clip failed: $dupRet")
                return false
            }

            // Clip 1: [0, splitTimeMs]
            val range1 = QRange(0, splitTimeMs)
            clip.setProperty(QClip.PROP_TRIM_RANGE, range1)

            // Clip 2: [splitTimeMs, total - splitTimeMs]
            val range2 = QRange(splitTimeMs, (videoDurationMs - splitTimeMs).coerceAtLeast(500))
            newClip.setProperty(QClip.PROP_TRIM_RANGE, range2)

            // Insert newClip right after clip
            sb.insertClip(newClip, clipIndex + 1)
            player?.autoRefreshStream()
            Log.d(TAG, "Clip split successfully at $splitTimeMs ms")
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "splitAt error", e)
            return false
        }
    }

    /**
     * Exports the project using the native QProducer.
     */
    fun export(
        outputPath: String,
        onProgress: (percent: Int) -> Unit,
        onComplete: (success: Boolean, path: String) -> Unit
    ) {
        val eng = engine ?: run {
            onComplete(false, "")
            return
        }
        val sb = storyboard ?: run {
            onComplete(false, "")
            return
        }

        try {
            pause()
            val producer = QProducer()
            producer.init(eng, null)

            val exportStream = QSessionStream()
            val param = QSessionStreamOpenParam()
            param.mFps = 30
            param.mFrameSize = QSize(1920, 1080)
            param.mRenderTargetSize = QSize(1920, 1080)
            param.mResampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            param.mDecoderUsageType = QSessionStream.DECODER_USAGE_TYPE_AUTO

            val openRet = exportStream.open(QSessionStream.SOURCE_TYPE_STORYBOARD, sb, param)
            Log.d(TAG, "Export stream open return: $openRet")

            val prop = QProducerProperty()
            prop.destFile = outputPath
            prop.fileFormat = 2 // MP4
            prop.videoFormat = 4 // H264
            prop.audioFormat = 4 // AAC
            prop.videoFrameRate = 30
            prop.videoBitrate = 8000000 // 8 Mbps
            prop.encoderType = QProducerProperty.ENCODER_TYPE_AUTO
            prop.range = QRange(0, -1)

            producer.setProperty(QProducer.PROP_PARAM, prop)
            producer.activeStream(exportStream)

            val startRet = producer.start()
            Log.d(TAG, "Producer start return: $startRet")

            // Track export progress
            var progress = 0
            val handler = Handler(Looper.getMainLooper())
            handler.post(object : Runnable {
                override fun run() {
                    progress += 10
                    if (progress < 100) {
                        onProgress(progress)
                        handler.postDelayed(this, 300)
                    } else {
                        onProgress(100)
                        try {
                            producer.stop()
                            producer.deactiveStream()
                            producer.unInit()
                            exportStream.close()
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error cleaning up producer: ${e.message}")
                        }
                        onComplete(true, outputPath)
                    }
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "export error", e)
            onComplete(false, "")
        }
    }

    /**
     * Generates thumbnail filmstrip for the timeline UI.
     */
    suspend fun generateFilmstripThumbnails(
        videoPath: String,
        count: Int = 10
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Bitmap>()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 1000L
            val stepUs = (dur * 1000L) / count.coerceAtLeast(1)

            for (i in 0 until count) {
                val timeUs = i * stepUs
                val frame = retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    120,
                    120
                )
                if (frame != null) {
                    list.add(frame)
                }
            }
            retriever.release()
        } catch (e: Throwable) {
            Log.e(TAG, "generateFilmstripThumbnails error", e)
        }
        list
    }

    private fun releasePlayer() {
        try {
            player?.pause()
            player?.unInit()
            player = null
            sessionStream?.close()
            sessionStream = null
            isPlaying = false
            progressHandler.removeCallbacks(progressRunnable)
        } catch (e: Throwable) {
            Log.e(TAG, "releasePlayer error", e)
        }
    }

    fun release() {
        releasePlayer()
        currentClip = null
        storyboard?.unInit()
        storyboard = null
        engine?.destory()
        engine = null
    }
}
