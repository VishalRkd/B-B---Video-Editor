package com.videoeditorpro.android.engine

import android.content.Context
import android.util.Log
import xiaoying.engine.QEngine
import xiaoying.engine.base.QAssetTemplateAdapter
import xiaoying.utils.QPoint
import java.io.File

/**
 * Handles native library loading and QEngine lifecycle management.
 */
object EngineCore {
    private const val TAG = "EngineCore"

    private var engine: QEngine? = null

    private val NATIVE_LIBS = listOf(
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

    fun initialize(context: Context): Boolean {
        if (engine != null) return true

        // 1. Load native C++ libraries
        for (lib in NATIVE_LIBS) {
            try {
                System.loadLibrary(lib)
                Log.d(TAG, "Loaded native library: $lib")
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading $lib: ${e.message}")
            }
        }

        // 2. Extract offline license file from assets
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
            Log.w(TAG, "Error extracting license file: ${e.message}")
        }

        // 3. Initialize QEngine instance
        return try {
            val qEng = QEngine()
            val licenseUri = if (licenseFile.exists()) licenseFile.absolutePath else "assets_android://xiaoying/ini/license.txt"
            val ret = qEng.create(licenseUri)
            Log.d(TAG, "QEngine created with result: $ret")

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

            // Register template adapter to resolve transitions, filters, and asset binaries (.xyt)
            val templateAdapter = QAssetTemplateAdapter(context)
            qEng.setProperty(QEngine.PROP_TEMPLATE_ADAPTER_CALLBACK, templateAdapter)

            engine = qEng
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize QEngine", e)
            false
        }
    }

    fun getEngine(): QEngine? = engine

    fun isInitialized(): Boolean = engine != null

    fun release() {
        try {
            engine?.destory()
            engine = null
            Log.d(TAG, "QEngine released")
        } catch (e: Throwable) {
            Log.e(TAG, "Error destroying QEngine", e)
        }
    }
}
