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
    val thumbnails by lazy { ThumbnailCache(this) }
    val waveforms by lazy { WaveformRepository(this) }
    val textBitmaps by lazy { TextBitmapCache() }
    val compiler by lazy { CompositionCompiler(this, textBitmaps = textBitmaps) }
    val exports by lazy { ExportCoordinator(this) }
}
