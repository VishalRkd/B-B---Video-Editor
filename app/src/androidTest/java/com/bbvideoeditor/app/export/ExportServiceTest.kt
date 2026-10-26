package com.bbvideoeditor.app.export

import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.render.MediaFixtures
import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@UnstableApi
@RunWith(AndroidJUnit4::class)
class ExportServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun servicePublishesCompleteMediaStoreOutputAndCleansCancelledRender() {
        val image = MediaFixtures(context).image("service-image", Color.MAGENTA)
        val project = Project(name = "Export service validation", media = mapOf(image.id to image), canvas = CanvasSettings(320,180),
            timeline = Timeline(listOf(Track(name = "Video", type = TrackType.VIDEO,
                clips = listOf(Clip(mediaId = image.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000)))))))
        val app = context.applicationContext as EditorApplication
        ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.runOnMainSync { app.exports.start(project, ExportSettings(resolution = 720, bitrate = 2_000_000)) }
            await { app.exports.state.value.projectId == project.id && !app.exports.state.value.active }
            val completed = app.exports.state.value
            assertEquals(completed.error, ExportPhase.SUCCEEDED, completed.phase)
            val uri = Uri.parse(completed.outputUri)
            try {
                context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.IS_PENDING, MediaStore.Video.Media.SIZE), null, null, null)!!.use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(0, cursor.getInt(0))
                    assertTrue(cursor.getLong(1) > 1000)
                }
            } finally { context.contentResolver.delete(uri, null, null) }
            val longProject = project.copy(id = newId(), timeline = project.timeline.copy(tracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.map { clip -> clip.copy(sourceRange = TimeRange(0, 120_000_000)) })
            }))
            instrumentation.runOnMainSync { app.exports.start(longProject, ExportSettings(resolution = 720, bitrate = 2_000_000)) }
            await { app.exports.state.value.projectId == longProject.id && app.exports.state.value.phase == ExportPhase.RUNNING }
            val jobId = requireNotNull(app.exports.state.value.jobId)
            instrumentation.runOnMainSync { app.exports.cancel() }
            await { app.exports.state.value.phase == ExportPhase.CANCELLED }
            assertFalse(File(app.exports.jobDirectory(jobId), "render.mp4").exists())
            assertFalse(File(app.exports.jobDirectory(jobId), "request.json").exists())
        }
    }

    private fun await(predicate: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 90_000
        while (!predicate()) {
            if (android.os.SystemClock.elapsedRealtime() > deadline) fail("Export service timed out: ${(context.applicationContext as EditorApplication).exports.state.value}")
            Thread.sleep(50)
        }
    }
}
