package com.bbvideoeditor.app

import android.app.Application
import com.bbvideoeditor.app.media.MediaImporter
import com.bbvideoeditor.app.media.ThumbnailCache
import com.bbvideoeditor.app.media.WaveformRepository
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.app.render.TextBitmapCache
import com.bbvideoeditor.app.export.ExportCoordinator

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EditorApplication : Application() {
    val projects by lazy { ProjectRepository(this) }
    val importer by lazy { MediaImporter(this) }
    private val thumbnailCache = lazy { ThumbnailCache(this) }
    val thumbnails by thumbnailCache
    private val waveformCache = lazy { WaveformRepository(this) }
    val waveforms by waveformCache
    private val textCache = lazy { TextBitmapCache() }
    val textBitmaps by textCache
    val compiler by lazy { CompositionCompiler(this, textBitmaps = textBitmaps) }
    val exports by lazy { ExportCoordinator(this) }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Drop cache ownership only; UI/GL consumers may still own these bitmaps.
        if (level >= TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_CRITICAL) {
            if (thumbnailCache.isInitialized()) thumbnails.clearMemory()
            if (waveformCache.isInitialized()) waveforms.clearMemory()
            if (textCache.isInitialized()) textBitmaps.clearMemory()
        }
    }
}
