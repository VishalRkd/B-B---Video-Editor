package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.editor.EditorViewModel
import com.bbvideoeditor.app.editor.TextCanvasLayer
import com.bbvideoeditor.app.export.ExportSettings
import com.bbvideoeditor.app.export.Media3RenderEngine
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@UnstableApi
@RunWith(AndroidJUnit4::class)
class TextRenderIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun timedStyledRotatedLayersMatchCanvasPixelsInTheEncodedMp4() = runBlocking {
        val first = Clip(startUs = 250_000, sourceRange = TimeRange(0, 500_000),
            text = TextContent("Frame\nStudio", font = TextFont.SERIF, fontSize = 140f, bold = true,
                color = 0xFFFFFFFF, outlineWidth = 3f, shadowRadius = 4f, backgroundColor = 0xCC2165C0),
            transform = Transform(translationX = .18f, translationY = .25f, rotationDegrees = 18f, scaleX = 1.2f, scaleY = 1.2f, opacity = .8f))
        val second = Clip(startUs = 900_000, sourceRange = TimeRange(0, 500_000),
            text = TextContent("SECOND", font = TextFont.MONO, fontSize = 110f, italic = true, color = 0xFFFF5252),
            transform = Transform(translationY = -.6f, rotationDegrees = -12f))
        val upper = Clip(startUs = 600_000, sourceRange = TimeRange(0, 600_000),
            text = TextContent("TOP", fontSize = 140f, color = 0xFF000000, backgroundColor = 0xFFFFD54F),
            transform = Transform(translationX = .3f, translationY = .2f))
        val project = Project(canvas = CanvasSettings(1920, 1080), timeline = Timeline(listOf(
            Track(name = "Captions", type = TrackType.TEXT, clips = listOf(first, second)),
            Track(name = "Title", type = TrackType.TEXT, clips = listOf(upper)),
        )))
        val output = File(context.cacheDir, "text-parity.mp4")
        output.delete()
        val result = withTimeout(120_000) {
            Media3RenderEngine(context, CompositionCompiler(context)).render(project,
                ExportSettings(resolution = 720, frameRate = 30, bitrate = 6_000_000), output)
        }
        assertTrue(result.videoFrames in 41..43)
        val cache = TextBitmapCache()
        project.timeline.clips.forEach { cache.get(it.text!!, project.canvas, 1080) }
        val editor = EditorViewModel(project)
        compose.setContent { Box(Modifier.fillMaxWidth().aspectRatio(project.canvas.aspectRatio).background(androidx.compose.ui.graphics.Color.Black)) {
            TextCanvasLayer(editor, cache, Modifier.fillMaxSize())
        } }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(output.absolutePath)
            for (time in listOf(100_000L, 400_000L, 700_000L, 850_000L, 1_050_000L, 1_300_000L)) {
                compose.runOnIdle { editor.seek(time); editor.endScrub() }
                compose.waitForIdle()
                val captured = compose.onNodeWithTag("text-canvas").captureToImage().asAndroidBitmap()
                val exported = requireNotNull(retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST))
                val expected = Bitmap.createScaledBitmap(captured, exported.width, exported.height, true)
                try { assertFramesMatch(expected, exported, time) }
                catch (failure: AssertionError) {
                    File(context.cacheDir, "text-expected-$time.png").outputStream().use { expected.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    File(context.cacheDir, "text-exported-$time.png").outputStream().use { exported.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    throw failure
                }
                finally { expected.recycle(); exported.recycle(); if (!captured.isRecycled) captured.recycle() }
            }
        } finally { retriever.release() }
    }

    @Test fun rasterizerWrapsUnicodeAndKeepsLogicalLayoutAtEveryOutputResolution() {
        val canvas = CanvasSettings(1080, 1920)
        val content = TextContent("Hello नमस्ते\nمرحبا 世界", fontSize = 80f, maxWidthFraction = .7f,
            outlineWidth = 3f, shadowRadius = 6f, backgroundColor = 0xAA000000)
        val cache = TextBitmapCache()
        val small = cache.get(content, canvas, 360)
        val large = cache.get(content, canvas, 1920)
        assertEquals(small.logicalWidth, large.logicalWidth, 0f)
        assertEquals(small.logicalHeight, large.logicalHeight, 0f)
        assertTrue(small.logicalHeight > content.fontSize * 2)
        assertSame(small, cache.get(content, canvas, 360))
        assertTrue(large.bitmap.width > small.bitmap.width * 5)
        assertThrows(IllegalArgumentException::class.java) {
            TextRasterizer.render(TextContent(("W\n").repeat(100), fontSize = 240f), canvas, 1080)
        }
    }

    @Test fun portraitCaptionOverVideoPreservesCanvasPlacementAndAudio() = runBlocking {
        val fixtures = MediaFixtures(context)
        val video = fixtures.video("portrait-text-red", 1f, 0f, 0f)
        val audio = fixtures.audio()
        val caption = Clip(startUs = 200_000, sourceRange = TimeRange(0, 600_000),
            text = TextContent("Portrait caption", fontSize = 60f, backgroundColor = 0xFFFFD54F, color = 0xFF000000),
            transform = Transform(translationY = -.72f))
        val project = Project(canvas = CanvasSettings(1080, 1920), media = listOf(video, audio).associateBy { it.id }, timeline = Timeline(listOf(
            Track(name = "Video", type = TrackType.VIDEO, clips = listOf(Clip(mediaId = video.id, startUs = 0, sourceRange = TimeRange(200_000, 1_000_000)))),
            Track(name = "Audio", type = TrackType.AUDIO, clips = listOf(Clip(mediaId = audio.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000)))),
            Track(name = "Caption", type = TrackType.TEXT, clips = listOf(caption)),
        )))
        val output = File(context.cacheDir, "portrait-caption.mp4")
        output.delete()
        val result = withTimeout(120_000) { Media3RenderEngine(context, CompositionCompiler(context)).render(project,
            ExportSettings(resolution = 720, frameRate = 30, bitrate = 4_000_000), output) }
        assertEquals(720, result.width)
        assertEquals(1280, result.height)
        assertTrue(result.audioChannels > 0)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(output.absolutePath)
            for ((time, present) in listOf(100_000L to false, 500_000L to true, 900_000L to false)) {
                val bitmap = requireNotNull(retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST))
                try {
                    val center = bitmap.getPixel(360, 640)
                    assertTrue(Color.red(center) > 180 && Color.green(center) < 60)
                    var gold = 0
                    var sumY = 0L
                    for (y in 900 until 1250 step 2) for (x in 50 until 670 step 2) {
                        val pixel = bitmap.getPixel(x, y)
                        if (Color.red(pixel) > 180 && Color.green(pixel) > 130 && Color.blue(pixel) < 130) { gold++; sumY += y }
                    }
                    if (present) {
                        assertTrue("Caption not rendered over portrait canvas", gold > 300)
                        assertTrue("Caption position differs from project coordinates", abs(sumY.toDouble() / gold - 1280 * .86) < 5)
                    } else assertEquals("Caption leaked outside its time range at $time", 0, gold)
                } finally { bitmap.recycle() }
            }
        } finally { retriever.release() }
    }

    private fun assertFramesMatch(expected: Bitmap, actual: Bitmap, time: Long) {
        var union = 0
        var intersection = 0
        var difference = 0L
        var pixels = 0
        var foregroundCount = 0
        var matchingEdges = 0
        for (y in 0 until actual.height step 2) for (x in 0 until actual.width step 2) {
            val a = actual.getPixel(x, y)
            val e = expected.getPixel(x, y)
            val foregroundA = maxOf(Color.red(a), Color.green(a), Color.blue(a)) > 30
            val foregroundE = maxOf(Color.red(e), Color.green(e), Color.blue(e)) > 30
            if (foregroundA || foregroundE) union++
            if (foregroundA && foregroundE) intersection++
            if (foregroundA) { foregroundCount++; if (nearForeground(expected, x, y)) matchingEdges++ }
            if (foregroundE) { foregroundCount++; if (nearForeground(actual, x, y)) matchingEdges++ }
            difference += abs(Color.red(a) - Color.red(e)) + abs(Color.green(a) - Color.green(e)) + abs(Color.blue(a) - Color.blue(e))
            pixels++
        }
        if (time == 100_000L) assertEquals("Text appeared before its timeline start", 0, union)
        else {
            // H.264 chroma subsampling and font rasterization at different resolutions change
            // thin glyph edges by 1-2 pixels. Check both bulk coverage and symmetric edge distance.
            assertTrue("Canvas/export geometry at $time us: intersection=$intersection union=$union", union > 100 && intersection.toDouble() / union > .8)
            assertTrue("Canvas/export edges displaced at $time us: $matchingEdges / $foregroundCount", matchingEdges.toDouble() / foregroundCount > .99)
        }
        assertTrue("Canvas/export pixel mismatch at $time us: mean=${difference.toDouble() / pixels / 3}", difference.toDouble() / pixels / 3 < 5.0)
    }

    private fun nearForeground(bitmap: Bitmap, x: Int, y: Int): Boolean {
        for (dy in -2..2) for (dx in -2..2) {
            val px = x + dx; val py = y + dy
            if (px !in 0 until bitmap.width || py !in 0 until bitmap.height) continue
            val color = bitmap.getPixel(px, py)
            if (maxOf(Color.red(color), Color.green(color), Color.blue(color)) > 30) return true
        }
        return false
    }
}
