package com.videoeditorpro.android.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import xiaoying.engine.base.QDisplayContext
import xiaoying.engine.base.QSessionStream
import xiaoying.engine.base.QSessionStreamOpenParam
import xiaoying.engine.player.QPlayer
import xiaoying.engine.storyboard.QStoryboard
import xiaoying.utils.QRect
import xiaoying.utils.QSize

/**
 * Manages video playback, OpenGL SurfaceView presentation, seeking, and playhead progress.
 */
class PlaybackController {
    private val TAG = "PlaybackController"

    private var player: QPlayer? = null
    private var sessionStream: QSessionStream? = null
    private var currentStoryboard: QStoryboard? = null

    private var isPlaying: Boolean = false
    private var durationMs: Int = 0

    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressListener: ((currentMs: Int, totalMs: Int) -> Unit)? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            if (isPlaying && player != null) {
                val curTime = player?.curStoryboardMediaTime ?: 0
                if (curTime >= 0) {
                    progressListener?.invoke(curTime, durationMs)
                }
                progressHandler.postDelayed(this, 33) // ~30 fps update
            }
        }
    }

    fun attachSurface(surfaceView: SurfaceView, storyboard: QStoryboard, onAttached: () -> Unit) {
        currentStoryboard = storyboard
        durationMs = storyboard.duration

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                setupPlayer(holder, surfaceView.width, surfaceView.height)
                onAttached()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                updateDisplay(holder, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releasePlayer()
            }
        })
    }

    private fun setupPlayer(holder: SurfaceHolder, width: Int, height: Int) {
        val eng = EngineCore.getEngine() ?: return
        val sb = currentStoryboard ?: return

        try {
            releasePlayer()

            val qp = QPlayer()
            val ret = qp.init(eng, null)
            Log.d(TAG, "QPlayer.init: $ret")

            val w = if (width > 0) width else 1080
            val h = if (height > 0) height else 1920

            val display = QDisplayContext().apply {
                surfaceHolder = holder
                screenRect = QRect(0, 0, w, h)
                resampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            }
            qp.setDisplayContext(display)

            val stream = QSessionStream()
            val openParam = QSessionStreamOpenParam().apply {
                mFps = 30
                mFrameSize = QSize(w, h)
                mRenderTargetSize = QSize(w, h)
                mResampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
                mDecoderUsageType = QSessionStream.DECODER_USAGE_TYPE_AUTO
            }

            val openRet = stream.open(QSessionStream.SOURCE_TYPE_STORYBOARD, sb, openParam)
            Log.d(TAG, "stream.open: $openRet")

            qp.activeStream(stream, 0, false)

            sessionStream = stream
            player = qp
        } catch (e: Throwable) {
            Log.e(TAG, "setupPlayer error", e)
        }
    }

    private fun updateDisplay(holder: SurfaceHolder, width: Int, height: Int) {
        val qp = player ?: return
        try {
            val display = QDisplayContext().apply {
                surfaceHolder = holder
                screenRect = QRect(0, 0, width, height)
                resampleMode = QDisplayContext.RESAMPLE_MODE_UPSCALE_FITIN
            }
            qp.setDisplayContext(display)
        } catch (e: Throwable) {
            Log.e(TAG, "updateDisplay error", e)
        }
    }

    fun play() {
        player?.let {
            it.play()
            isPlaying = true
            progressHandler.post(progressTicker)
        }
    }

    fun pause() {
        player?.let {
            it.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressTicker)
        }
    }

    fun isPlaying(): Boolean = isPlaying

    fun seekTo(positionMs: Int) {
        player?.seekTo(positionMs)
        progressListener?.invoke(positionMs, durationMs)
    }

    fun getCurrentPosition(): Int = player?.curStoryboardMediaTime ?: 0

    fun refreshStream() {
        try {
            player?.autoRefreshStream()
            durationMs = currentStoryboard?.duration ?: durationMs
        } catch (e: Throwable) {
            Log.e(TAG, "refreshStream error", e)
        }
    }

    fun setProgressListener(listener: (currentMs: Int, totalMs: Int) -> Unit) {
        progressListener = listener
    }

    fun releasePlayer() {
        try {
            pause()
            player?.unInit()
            player = null
            sessionStream?.close()
            sessionStream = null
        } catch (e: Throwable) {
            Log.e(TAG, "releasePlayer error", e)
        }
    }
}
