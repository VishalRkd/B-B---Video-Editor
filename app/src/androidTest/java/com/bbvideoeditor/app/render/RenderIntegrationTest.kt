package com.bbvideoeditor.app.render

import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.export.ExportSettings
import com.bbvideoeditor.app.export.Media3RenderEngine
import com.bbvideoeditor.app.media.MediaImporter
import com.bbvideoeditor.app.media.WaveformRepository
import android.net.Uri
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@UnstableApi
@RunWith(AndroidJUnit4::class)
class RenderIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun exportedMp4PreservesTrimsSpeedGapsLayersAndAudioTimestamps() = runBlocking {
        val fixtures = MediaFixtures(context)
        val red = fixtures.video("render-red", 1f, 0f, 0f)
        val blue = fixtures.video("render-blue", 0f, 0f, 1f)
        val white = fixtures.image("render-white", Color.WHITE)
        val audio = fixtures.audio()
        val project = Project(name = "Render validation", canvas = CanvasSettings(320, 180, 30),
            media = listOf(red, blue, white, audio).associateBy { it.id }, timeline = Timeline(listOf(
                Track(name = "Video", type = TrackType.VIDEO, clips = listOf(
                    Clip(mediaId = red.id, startUs = 0, sourceRange = TimeRange(400_000, 1_000_000), speed = 2f),
                    Clip(mediaId = blue.id, startUs = 800_000, sourceRange = TimeRange(250_000, 500_000), speed = .5f),
                )),
                Track(name = "Overlay", type = TrackType.OVERLAY, clips = listOf(
                    Clip(mediaId = white.id, startUs = 250_000, sourceRange = TimeRange(0, 200_000),
                        transform = Transform(scaleX = .25f, scaleY = .25f, translationX = .5f, translationY = .5f)),
                )),
                Track(name = "Audio", type = TrackType.AUDIO, clips = listOf(
                    Clip(mediaId = audio.id, startUs = 0, sourceRange = TimeRange(0, 1_800_000), volume = .5f),
                )),
            )))
        val output = File(context.cacheDir, "validated-editor.mp4")
        if (output.exists()) check(output.delete())
        val result = withTimeout(120_000) {
            Media3RenderEngine(context, CompositionCompiler(context)).render(project,
                ExportSettings(resolution = 720, frameRate = 30, bitrate = 3_000_000), output)
        }
        assertTrue(output.length() > 1000)
        assertEquals(1280, result.width)
        assertEquals(720, result.height)
        assertEquals("video/avc", result.codec)
        assertTrue("Output duration ${result.durationMs}", kotlin.math.abs(result.durationMs - 1800L) < 100)
        assertTrue("Output frame count ${result.videoFrames}", result.videoFrames in 52..55)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(output.absolutePath)
            assertColor(retriever, 100_000, .5f, .5f, Color.RED)
            assertColor(retriever, 650_000, .5f, .5f, Color.BLACK)
            assertColor(retriever, 1_100_000, .5f, .5f, Color.BLUE)
            assertColor(retriever, 350_000, .75f, .25f, Color.WHITE)
        } finally { retriever.release() }
        val videoRange = packetRange(output, "video/")
        val audioRange = packetRange(output, "audio/")
        assertTrue("Video begins at ${videoRange.first}", videoRange.first in 0..40_000)
        assertTrue("Audio begins at ${audioRange.first}", audioRange.first in -40_000..40_000)
        assertTrue("A/V tails diverged: $videoRange $audioRange", kotlin.math.abs(videoRange.second - audioRange.second) < 70_000)
        val exportedMedia = MediaImporter(context).importMedia(listOf(Uri.fromFile(output))).media.single()
        val peaks = WaveformRepository(context).waveform(exportedMedia, 100)
        val maximum = peaks.maxOrNull() ?: 0f
        assertTrue("Half-volume audio was not applied: peak=$maximum", maximum in .04f.. .09f)
    }

    private fun assertColor(retriever: MediaMetadataRetriever, timeUs: Long, x: Float, y: Float, expected: Int) {
        val bitmap = requireNotNull(retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST))
        try {
            val actual = bitmap.getPixel((bitmap.width * x).toInt(), (bitmap.height * y).toInt())
            assertTrue("Color at $timeUs ($x,$y) expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}",
                kotlin.math.abs(Color.red(actual) - Color.red(expected)) < 65 &&
                    kotlin.math.abs(Color.green(actual) - Color.green(expected)) < 65 &&
                    kotlin.math.abs(Color.blue(actual) - Color.blue(expected)) < 65)
        } finally { bitmap.recycle() }
    }

    private fun packetRange(file: File, prefix: String): Pair<Long, Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix) == true
            } ?: error("Export has no $prefix track")
            extractor.selectTrack(track)
            var first = Long.MIN_VALUE
            var previous = Long.MIN_VALUE
            while (extractor.sampleTime != -1L) {
                val time = extractor.sampleTime
                if (first == Long.MIN_VALUE) first = time
                assertTrue("Non-monotonic $prefix timestamp $time after $previous", time >= previous)
                previous = time
                if (!extractor.advance()) break
            }
            check(first != Long.MIN_VALUE)
            return first to previous
        } finally { extractor.release() }
    }
}
