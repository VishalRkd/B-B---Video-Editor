package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@UnstableApi
@RunWith(AndroidJUnit4::class)
class CompositionIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun compilerRetainsExactTimelineAndRejectsUnknownFeatures() = runBlocking {
        val media = MediaFixtures(context).image("compiler-red", Color.RED)
        val clip = Clip(mediaId = media.id, startUs = 250_000, sourceRange = TimeRange(0, 1_000_000))
        val project = Project(canvas = CanvasSettings(320, 180, 30),
            media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "Main", type = TrackType.VIDEO, clips = listOf(clip)))))
        val compiled = CompositionCompiler(context).compile(project)
        assertEquals(project.durationUs, compiled.durationUs)
        assertEquals(3, compiled.composition.sequences.size) // Clock, visual, and silent audio.
        assertEquals(2, compiled.composition.sequences[1].editedMediaItems.size) // Leading gap plus image.
        val unsupported = project.copy(timeline = project.timeline.copy(tracks = listOf(
            project.timeline.tracks[0].copy(clips = listOf(clip.copy(effects = listOf(EffectSpec(type = "unknown-filter"))))),
        )))
        try { CompositionCompiler(context).compile(unsupported); fail("Unknown edits must not silently disappear") }
        catch (expected: UnsupportedRenderFeature) { assertTrue(expected.message.orEmpty().contains("unknown-filter")) }
    }

    @Test fun actualVideoPreviewFollowsTrimOrderOverlayAudioAndLifecycle() {
        val fixtures = MediaFixtures(context)
        val red = fixtures.video("preview-red", 1f, 0f, 0f)
        val blue = fixtures.video("preview-blue", 0f, 0f, 1f)
        val white = fixtures.image("preview-overlay", Color.WHITE)
        val audio = fixtures.audio()
        val first = Clip(mediaId = red.id, startUs = 0, sourceRange = TimeRange(500_000, 1_000_000))
        val second = Clip(mediaId = blue.id, startUs = 1_000_000, sourceRange = TimeRange(250_000, 1_000_000))
        val overlay = Clip(mediaId = white.id, startUs = 250_000, sourceRange = TimeRange(0, 500_000),
            transform = Transform(scaleX = .25f, scaleY = .25f, translationX = .5f, translationY = .5f))
        val project = Project(canvas = CanvasSettings(320, 180, 30), media = listOf(red, blue, white, audio).associateBy { it.id },
            timeline = Timeline(listOf(
                Track(name = "Main", type = TrackType.VIDEO, clips = listOf(first, second)),
                Track(name = "Overlay", type = TrackType.OVERLAY, clips = listOf(overlay)),
                Track(name = "Sound", type = TrackType.AUDIO, clips = listOf(Clip(mediaId = audio.id, startUs = 200_000,
                    sourceRange = TimeRange(100_000, 1_600_000), volume = .25f))),
            )))
        val engine = onMain { PreviewEngine(context, CompositionCompiler(context)) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: MainActivity? = null
            var view: PlayerView? = null
            var binding: Job? = null
            scenario.onActivity { current ->
                activity = current
                view = PlayerView(current).apply { useController = false }
                current.setContentView(view)
                binding = CoroutineScope(Dispatchers.Main.immediate).launch { engine.state.collect { view!!.player = it.player } }
                engine.updateProject(project)
            }
            try {
                await("composition creation") { engine.state.value.player != null }
                onMain { view!!.player = engine.state.value.player }
                await("video prepared") { onMain { engine.state.value.player?.playbackState == Player.STATE_READY } }
                assertNull(engine.state.value.error)
                onMain { engine.seek(400_000) }
                awaitColor(view!!, Color.RED)
                awaitColor(view!!, Color.WHITE, .75f, .25f)
                onMain { engine.seek(1_400_000) }
                awaitColor(view!!, Color.BLUE)
                onMain { engine.seek(300_000); engine.play() }
                await("clock crosses a clip boundary with audio") { engine.state.value.positionUs > 1_300_000 && engine.state.value.playing }
                awaitColor(view!!, Color.BLUE)
                assertNull(engine.state.value.error)
                onMain { engine.pause() }

                // Replace the shared project: reordered clips must change the preview immediately.
                val reordered = ReorderClip(second.id, 0).apply(project)
                onMain { engine.updateProject(reordered); engine.seek(300_000) }
                await("reordered composition") { !engine.state.value.buffering && engine.state.value.error == null }
                awaitColor(view!!, Color.BLUE)
                onMain { engine.suspendPlayback() }
                assertNull(engine.state.value.player)
                assertFalse(engine.state.value.playing)
                onMain { engine.resume() }
                await("resumed composition") { engine.state.value.player != null }
                onMain { view!!.player = engine.state.value.player }
                await("resumed decoder") { onMain { engine.state.value.player?.playbackState == Player.STATE_READY } }
                awaitColor(view!!, Color.BLUE)
            } finally {
                onMain { binding?.cancel(); view?.player = null; engine.close() }
            }
        }
    }

    private fun awaitColor(view: PlayerView, expected: Int, x: Float = .5f, y: Float = .5f) {
        var last = Color.TRANSPARENT
        await("preview color ${Integer.toHexString(expected)}") {
            val bitmap = screenshot(view) ?: return@await false
            try { last = bitmap.getPixel((bitmap.width * x).toInt(), (bitmap.height * y).toInt()) }
            finally { bitmap.recycle() }
            val redCorrect = kotlin.math.abs(Color.red(last) - Color.red(expected)) < 65
            val greenCorrect = kotlin.math.abs(Color.green(last) - Color.green(expected)) < 65
            val blueCorrect = kotlin.math.abs(Color.blue(last) - Color.blue(expected)) < 65
            redCorrect && greenCorrect && blueCorrect
        }
    }

    private fun screenshot(view: PlayerView): Bitmap? {
        val surface = onMain { view.videoSurfaceView as SurfaceView }
        val dimensions = onMain { surface.width to surface.height }
        if (dimensions.first <= 0 || dimensions.second <= 0 || !surface.holder.surface.isValid) return null
        val bitmap = Bitmap.createBitmap(dimensions.first, dimensions.second, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var result = -1
        PixelCopy.request(surface, bitmap, { result = it; latch.countDown() }, Handler(Looper.getMainLooper()))
        assertTrue("PixelCopy timeout", latch.await(5, TimeUnit.SECONDS))
        if (result == PixelCopy.ERROR_SOURCE_NO_DATA || result == PixelCopy.ERROR_SOURCE_INVALID) { bitmap.recycle(); return null }
        assertEquals(PixelCopy.SUCCESS, result)
        return bitmap
    }

    private fun await(label: String, predicate: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 20_000
        while (!predicate()) {
            if (android.os.SystemClock.elapsedRealtime() >= deadline) fail("Timed out waiting for $label")
            Thread.sleep(60)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
}
