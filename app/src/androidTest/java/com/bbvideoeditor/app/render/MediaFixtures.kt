package com.bbvideoeditor.app.render

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** Generated on-device, with no external downloads or dependency on a user's media library. */
internal class MediaFixtures(private val context: Context) {
    private val directory = File(context.cacheDir, "composition-tests").apply { mkdirs() }

    fun image(name: String, color: Int): MediaReference {
        val file = File(directory, "$name.png")
        val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        try { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
        return MediaReference(uri = Uri.fromFile(file).toString(), displayName = name, kind = MediaKind.IMAGE,
            durationUs = 0, width = 320, height = 180, mimeType = "image/png", sizeBytes = file.length())
    }

    fun audio(): MediaReference {
        val file = File(directory, "tone.wav")
        val rate = 44_100
        val samples = rate * 2
        val dataBytes = samples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + dataBytes).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(dataBytes).array()
        file.outputStream().use { output ->
            output.write(header)
            val chunk = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in 0 until samples) {
                if (chunk.remaining() < 2) { output.write(chunk.array(), 0, chunk.position()); chunk.clear() }
                chunk.putShort((sin(sample * 2 * PI * 440 / rate) * 4000).toInt().toShort())
            }
            output.write(chunk.array(), 0, chunk.position())
        }
        return MediaReference(uri = Uri.fromFile(file).toString(), displayName = "Tone", kind = MediaKind.AUDIO,
            durationUs = 2_000_000, hasAudio = true, mimeType = "audio/wav", sizeBytes = file.length())
    }

    fun video(name: String, red: Float, green: Float, blue: Float): MediaReference {
        val file = File(directory, "$name.mp4")
        val codec = MediaCodec.createEncoderByType("video/avc")
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var trackIndex = -1
        val format = MediaFormat.createVideoFormat("video/avc", 320, 180).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 600_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0))
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val configCount = IntArray(1)
        val attributes = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, 0x3142, 1, EGL14.EGL_NONE)
        check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, configCount, 0))
        val config = requireNotNull(configs[0])
        val eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        val eglSurface = EGL14.eglCreateWindowSurface(display, config, inputSurface, intArrayOf(EGL14.EGL_NONE), 0)
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext))
        codec.start()
        val info = MediaCodec.BufferInfo()
        fun drain(eos: Boolean) {
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
            while (true) {
                val index = codec.dequeueOutputBuffer(info, if (eos) 10_000 else 0)
                if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!eos) return
                    check(android.os.SystemClock.elapsedRealtime() < deadline) { "Fixture encoder timed out" }
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (index >= 0) {
                    val buffer = requireNotNull(codec.getOutputBuffer(index))
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                        check(muxerStarted)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, buffer, info)
                    }
                    val ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(index, false)
                    if (ended) return
                }
            }
        }
        try {
            for (frame in 0 until 60) {
                GLES20.glViewport(0, 0, 320, 180)
                GLES20.glClearColor(red, green, blue, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                EGLExt.eglPresentationTimeANDROID(display, eglSurface, frame * 1_000_000_000L / 30)
                check(EGL14.eglSwapBuffers(display, eglSurface))
                drain(false)
            }
            codec.signalEndOfInputStream()
            drain(true)
        } finally {
            codec.stop()
            codec.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, eglSurface)
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
            inputSurface.release()
        }
        val retriever = MediaMetadataRetriever()
        val durationUs = try {
            retriever.setDataSource(file.absolutePath)
            requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toLong() * 1000
        } finally { retriever.release() }
        return MediaReference(uri = Uri.fromFile(file).toString(), displayName = name, kind = MediaKind.VIDEO,
            durationUs = durationUs, width = 320, height = 180, frameRate = 30f, mimeType = "video/mp4", sizeBytes = file.length())
    }
}
