package com.bbvideoeditor.app

import android.app.Application
import com.bbvideoeditor.app.media.MediaImporter
import com.bbvideoeditor.app.media.ThumbnailCache
import com.bbvideoeditor.app.media.WaveformRepository
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.app.render.TextBitmapCache
import com.bbvideoeditor.app.export.ExportCoordinator

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class,androidx.media3.common.util.ExperimentalApi::class)
class EditorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.bbvideoeditor.app.export.ExportCapabilities.configure(this)
        // Configure the shared backend before either preview OR export creates a renderer.
        // Backend behavior must not depend on which subsystem happens to initialize first.
        androidx.media3.common.Flags.disableFlag(androidx.media3.common.Flags.FLAG_PER_STREAM_MEDIA_PROGRESSION)
        androidx.media3.common.Flags.enableFlag(androidx.media3.common.Flags.FLAG_PROCESSED_STREAM_CHANGED_AT_START)
    }
    val resourceBudget by lazy { com.bbvideoeditor.app.playback.EngineResourceBudget.inspect(this) }
    val previewPolicy by lazy { com.bbvideoeditor.app.playback.PreviewResourcePolicy(this, resourceBudget) }
    val projects by lazy { ProjectRepository(this) }
    val assets by lazy { com.bbvideoeditor.app.media.OwnedAssetStore(this,projects) }
    val importer by lazy { MediaImporter(this) }
    private val thumbnailCache = lazy { ThumbnailCache(this) }
    val thumbnails by thumbnailCache
    private val waveformCache = lazy { WaveformRepository(this) }
    val waveforms by waveformCache
    private val textCache = lazy { TextBitmapCache(resourceBudget.textBytes) }
    val textBitmaps by textCache
    val compiler by lazy { CompositionCompiler(this, textBitmaps = textBitmaps) }
    val optimizedMedia by lazy { com.bbvideoeditor.app.media.OptimizedPreviewMedia(this,compiler,resourceBudget) }
    val exports by lazy { ExportCoordinator(this) }

    override fun onLowMemory() {
        super.onLowMemory()
        clearDerivedMemory()
    }

    private fun clearDerivedMemory() {
        if (thumbnailCache.isInitialized()) thumbnails.clearMemory()
        if (waveformCache.isInitialized()) waveforms.clearMemory()
        if (textCache.isInitialized()) textBitmaps.clearMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Drop cache ownership only; UI/GL consumers may still own these bitmaps.
        if (level >= TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_CRITICAL) {
            clearDerivedMemory()
        }
    }
}
