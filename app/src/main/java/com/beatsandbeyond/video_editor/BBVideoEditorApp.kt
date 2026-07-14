package com.beatsandbeyond.video_editor

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for B&B Video Editor.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * which creates a component that is attached to the Application lifecycle
 * and provides dependencies to all other Android components.
 *
 * Future responsibilities:
 * - Initialize crash reporting (e.g., Firebase Crashlytics)
 * - Initialize performance monitoring
 * - Initialize analytics
 * - Set up global exception handling
 */
@HiltAndroidApp
class BBVideoEditorApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.beatsandbeyond.video_editor.core.domain.model.DomainDebugConfig.isDebug = isDebuggable
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of available RAM
                    .build()
            }
            .build()
    }
}
