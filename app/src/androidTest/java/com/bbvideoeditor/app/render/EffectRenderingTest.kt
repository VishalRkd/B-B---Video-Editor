package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.export.ExportSettings
import com.bbvideoeditor.app.export.Media3RenderEngine
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Real GL preview pixels versus decoded H.264 pixels, never a CPU stand-in for the renderer. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class EffectRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun effect(type: String, amount: Float) = EffectSpec(type = type, parameters = mapOf("amount" to amount))

    @Test fun colorEffectsChangePixelsPreserveOrderAndMatchPreviewAcrossSplitsAndGaps() = runBlocking {
        val fixtures = MediaFixtures(context)
        val gray = fixtures.image("effect-gray", Color.rgb(90, 90, 90))
        val red = fixtures.video("effect-red", .8f, .1f, .1f)
        val chains = listOf(emptyList(), listOf(effect("brightness", .35f)), listOf(effect("contrast", -1f)),
            listOf(effect("saturation", -100f)), listOf(effect("brightness", .3f), effect("contrast", -.5f)),
            listOf(effect("contrast", -.5f), effect("brightness", .3f)), emptyList())
        val clips = chains.mapIndexed { i, chain -> Clip(id = "effect-$i", mediaId = if (i in 2..3 || i == 6) red.id else gray.id,
            startUs = i * 1_000_000L, sourceRange = TimeRange(100_000, 800_000), effects = chain) }
        var project = Project(canvas = CanvasSettings(320, 180, 30), media = listOf(gray, red).associateBy { it.id },
            timeline = Timeline(listOf(Track(name = "color", type = TrackType.VIDEO, clips = clips))))
        project = SplitClip("effect-1", 1_400_000, "effect-split").apply(project)
        val times = listOf(300_000L, 1_200_000L, 1_600_000L, 2_300_000L, 3_300_000L, 4_300_000L, 5_300_000L, 900_000L, 1_900_000L, 6_300_000L)
        val colors = exportColors(project, times)
        assertTrue("Brightness did not brighten pixels: $colors", Color.red(colors[1]) > Color.red(colors[0]) + 35)
        assertNear(colors[1], colors[2], 6) // Split preserves the exact chain.
        assertGray(colors[3]); assertGray(colors[4])
        assertNear(Color.rgb(128, 128, 128), colors[3], 8) // Contrast -1 is middle gray.
        assertTrue("Control fixture must be chromatic", Color.red(colors[9]) - Color.green(colors[9]) > 100)
        assertTrue("Saturation did not remove color", abs(Color.red(colors[4]) - Color.green(colors[4])) < 8)
        assertTrue("Effects were reordered or ignored: $colors", abs(Color.red(colors[5]) - Color.red(colors[6])) > 15)
        assertNear(Color.BLACK, colors[7], 6); assertNear(Color.BLACK, colors[8], 6)
        assertFalse(CompositionCompiler(context).compile(project, preferSingleVisualInput = true).requiresCompositor)
        assertPreviewMatches(project, times, colors)
    }

    @Test fun sourceTimedOpacityRuntimeMatchesExportWithTrimSpeedAndOverlayGaps() = runBlocking {
        val fixtures = MediaFixtures(context)
        val blue = fixtures.image("animated-base", Color.BLUE)
        val red = fixtures.image("animated-layer", Color.RED)
        val overlay = Clip(mediaId = red.id, startUs = 300_000, sourceRange = TimeRange(500_000, 2_000_000), speed = 2f,
            effects = listOf(effect("brightness", -.1f)), properties = listOf(AnimatedProperty("opacity", 1f,
                listOf(Keyframe(500_000, 0f), Keyframe(1_500_000, 1f), Keyframe(2_000_000, 1f, Interpolation.HOLD), Keyframe(2_400_000, 0f)))))
        val project = Project(canvas = CanvasSettings(320, 180, 30), media = listOf(blue, red).associateBy { it.id },
            timeline = Timeline(listOf(
                Track(name = "base", type = TrackType.VIDEO, clips = listOf(Clip(mediaId = blue.id, startUs = 0, sourceRange = TimeRange(0, 1_600_000)))),
                Track(name = "animated", type = TrackType.OVERLAY, clips = listOf(overlay)))))
        val times = listOf(100_000L, 350_000L, 550_000L, 850_000L, 1_150_000L, 1_450_000L)
        val colors = exportColors(project, times)
        assertNear(Color.BLUE, colors.first(), 12); assertNear(Color.BLUE, colors.last(), 12)
        assertTrue("Opacity did not interpolate: $colors", Color.red(colors[2]) > Color.red(colors[1]) + 20)
        assertTrue("Opacity never reached the outgoing layer: $colors", Color.red(colors[3]) > 180 && Color.blue(colors[3]) < 30)
        assertNear(colors[3], colors[4], 8) // HOLD is still opaque.
        assertPreviewMatches(project, times, colors)
    }

    @Test fun capabilityFailuresMatchBothCompilationModesBeforeMediaAccess() = runBlocking {
        val media = MediaReference(uri = "file:/missing", displayName = "missing", kind = MediaKind.VIDEO, durationUs = 1_000_000)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000))
        val compiler = CompositionCompiler(context)
        listOf(clip.copy(effects = listOf(effect("unknown", 0f))),
            clip.copy(effects = listOf(effect("brightness", 2f))),
            clip.copy(effects = listOf(EffectSpec(type = "contrast", properties = listOf(AnimatedProperty("amount", .5f))))),
            clip.copy(properties = listOf(AnimatedProperty("unsupported", 1f)))).forEach { invalid ->
            val project = Project(media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "v", type = TrackType.VIDEO, clips = listOf(invalid)))))
            val problems = listOf(false, true).map { preview ->
                try { compiler.compile(project, preferSingleVisualInput = preview); fail("Unsupported feature was ignored"); null }
                catch (expected: RenderCapabilityException) { expected.problem }
            }
            assertEquals(problems.first(), problems.last())
        }
    }

    private suspend fun exportColors(project: Project, times: List<Long>): List<Int> {
        val file = File(context.cacheDir, "effect-validation-${newId()}.mp4")
        val retriever = MediaMetadataRetriever()
        try {
            withTimeout(120_000) { Media3RenderEngine(context, CompositionCompiler(context)).render(project,
                ExportSettings(resolution = 720, bitrate = 4_000_000), file) }
            retriever.setDataSource(file.absolutePath)
            return times.map { time ->
                val bitmap = requireNotNull(retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST))
                try { bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) } finally { bitmap.recycle() }
            }
        } finally { retriever.release(); file.delete() }
    }

    private fun assertPreviewMatches(project: Project, times: List<Long>, colors: List<Int>) {
        val engine = main { PreviewEngine(context, CompositionCompiler(context)) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var view: PlayerView? = null
            var binding: Job? = null
            try {
                scenario.onActivity { activity ->
                    view = PlayerView(activity).apply { useController = false }
                    activity.setContentView(view)
                    binding = CoroutineScope(Dispatchers.Main.immediate).launch { engine.state.collect { view!!.player = it.player } }
                    engine.updateProject(project)
                }
                await("ready") { assertNull(engine.state.value.error); main { engine.state.value.player?.playbackState == Player.STATE_READY } }
                // Forward then backward: exercises both graph paths and the retained layered seek workaround.
                (times.indices.toList() + times.indices.reversed()).forEach { index ->
                    main { engine.seek(times[index]) }
                    var lastColor = Color.TRANSPARENT
                    try {
                        await("preview/export parity at ${times[index]}, expected ${colors[index]}") {
                            assertNull(engine.state.value.error)
                            lastColor = centerPixel(view!!) ?: return@await false
                            near(lastColor, colors[index], 28) && !engine.state.value.buffering
                        }
                    } catch (error: AssertionError) {
                        throw AssertionError("${error.message}; actual=${Integer.toHexString(lastColor)}, expected=${Integer.toHexString(colors[index])}; all exported=$colors; state=${engine.state.value}; events=${main { engine.diagnostics.events() }}", error)
                    }
                    assertNear(colors[index], lastColor, 28)
                }
            } finally { main { binding?.cancel(); view?.player = null; engine.close() } }
        }
    }

    private fun centerPixel(view: PlayerView): Int? {
        val surface = main { view.videoSurfaceView as SurfaceView }
        val size = main { surface.width to surface.height }
        if (size.first <= 0 || size.second <= 0 || !surface.holder.surface.isValid) return null
        val bitmap = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        var result = -1
        try {
            PixelCopy.request(surface, bitmap, { result = it; latch.countDown() }, Handler(Looper.getMainLooper()))
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            if (result == PixelCopy.ERROR_SOURCE_NO_DATA || result == PixelCopy.ERROR_SOURCE_INVALID) return null
            assertEquals(PixelCopy.SUCCESS, result)
            return bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        } finally { bitmap.recycle() }
    }
    private fun assertGray(color: Int) { assertTrue("Not gray: $color", abs(Color.red(color) - Color.green(color)) < 8 && abs(Color.green(color) - Color.blue(color)) < 8) }
    private fun near(a: Int, b: Int, tolerance: Int) = abs(Color.red(a) - Color.red(b)) <= tolerance && abs(Color.green(a) - Color.green(b)) <= tolerance && abs(Color.blue(a) - Color.blue(b)) <= tolerance
    private fun assertNear(expected: Int, actual: Int, tolerance: Int) { assertTrue("Expected ${Integer.toHexString(expected)}, got ${Integer.toHexString(actual)}", near(expected, actual, tolerance)) }
    private fun await(label: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (!predicate()) { if (SystemClock.elapsedRealtime() > deadline) fail("Timed out: $label"); Thread.sleep(35) }
    }
    private fun <T> main(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST") return result as T
    }
}
