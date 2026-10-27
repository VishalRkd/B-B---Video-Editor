package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.graphics.Color
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
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.core.*
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
class TextPlaybackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun project(): Project = Project(canvas = CanvasSettings(1280, 720), timeline = Timeline(listOf(
        Track(name = "Captions", type = TrackType.TEXT, clips = listOf(
            Clip(id = "red", startUs = 200_000, sourceRange = TimeRange(0, 700_000), text = TextContent("FIRST", fontSize = 180f, backgroundColor = 0xFFFF0000)),
            Clip(id = "blue", startUs = 2_000_000, sourceRange = TimeRange(0, 1_200_000), text = TextContent("SECOND", fontSize = 180f, backgroundColor = 0xFF0000FF)),
        )),
    )))

    @Test fun nativeTextPreviewTraversesCaptionBoundariesAndRepeatedPausedSeeks() {
        val project = project()
        val engine = main { PreviewEngine(context, CompositionCompiler(context)) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var view: PlayerView
            var binding: Job? = null
            try {
                scenario.onActivity { activity ->
                    view = PlayerView(activity).apply { useController = false }
                    activity.setContentView(view)
                    binding = CoroutineScope(Dispatchers.Main.immediate).launch { engine.state.collect { view.player = it.player } }
                    engine.updateProject(project)
                }
                await { assertNull(engine.state.value.error); main { engine.state.value.player?.playbackState == Player.STATE_READY } }
                val player = main { engine.state.value.player }
                for ((time, color) in listOf(500_000L to Color.RED, 2_500_000L to Color.BLUE, 1_400_000L to Color.BLACK, 500_000L to Color.RED)) {
                    main { engine.seek(time) }
                    await {
                        assertNull(engine.state.value.error)
                        assertSame(player, engine.state.value.player)
                        assertFalse(engine.state.value.playing)
                        frameColor(view) == color
                    }
                }
                main { engine.seek(0); engine.play() }
                val samples = mutableSetOf<Int>()
                await {
                    assertNull(engine.state.value.error)
                    assertSame(player, engine.state.value.player)
                    val before = engine.state.value.positionUs
                    val color = frameColor(view)
                    val after = engine.state.value.positionUs
                    val expected = when {
                        before >= 350_000 && after <= 750_000 -> Color.RED
                        before >= 1_100_000 && after <= 1_800_000 -> Color.BLACK
                        before >= 2_200_000 && after <= 3_000_000 -> Color.BLUE
                        else -> null
                    }
                    if (expected != null && color != null) {
                        assertEquals("Wrong caption at $before..$after", expected, color)
                        samples += expected
                    }
                    main { engine.state.value.player?.playbackState == Player.STATE_ENDED }
                }
                assertEquals(setOf(Color.RED, Color.BLUE, Color.BLACK), samples)
            } finally { main { binding?.cancel(); view.player = null; engine.close() } }
        }
    }

    @Test fun canvasTextEditsDoNotRestartTheNativeDecoderOrMoveThePlayhead() {
        val project = project()
        val engine = main { PreviewEngine(context, CompositionCompiler(context), renderTextInPlayer = false) }
        try {
            main { engine.updateProject(project) }
            await { assertNull(engine.state.value.error); main { engine.state.value.player?.playbackState == Player.STATE_READY } }
            main { engine.seek(500_000) }
            val player = main { engine.state.value.player }
            main {
                var edited = UpdateTextClip("red", TextContent("Changed", fontSize = 90f), 200_000, 700_000).apply(project)
                repeat(30) { step ->
                    edited = UpdateClip("red", transform = Transform(translationX = step / 50f)).apply(edited)
                    engine.updateProject(edited)
                    assertSame(player, engine.state.value.player)
                    assertEquals(500_000L, engine.state.value.positionUs)
                    assertNull(engine.state.value.error)
                }
            }
            // Wait beyond the compiler debounce so a mistakenly queued rebuild cannot pass unnoticed.
            Thread.sleep(500)
            assertSame(player, engine.state.value.player)
        } finally { main { engine.close() } }
    }

    private fun frameColor(view: PlayerView): Int? {
        val surface = main { view.videoSurfaceView as SurfaceView }
        val dimensions = main { surface.width to surface.height }
        if (dimensions.first <= 0 || dimensions.second <= 0 || !surface.holder.surface.isValid) return null
        // Sampling flat caption colors does not need a full-resolution screen allocation.
        val width = dimensions.first.coerceAtMost(540)
        val height = (dimensions.second.toLong() * width / dimensions.first).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val completed = CountDownLatch(1)
        var status = -1
        PixelCopy.request(surface, bitmap, { status = it; completed.countDown() }, Handler(Looper.getMainLooper()))
        assertTrue(completed.await(5, TimeUnit.SECONDS))
        try {
            if (status != PixelCopy.SUCCESS) return null
            var red = 0; var blue = 0
            for (y in 0 until bitmap.height step 5) for (x in 0 until bitmap.width step 5) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.red(pixel) > 180 && Color.blue(pixel) < 70) red++
                if (Color.blue(pixel) > 180 && Color.red(pixel) < 70) blue++
            }
            return if (red > 20) Color.RED else if (blue > 20) Color.BLUE else Color.BLACK
        } finally { bitmap.recycle() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (!condition()) { assertTrue("Preview timed out", SystemClock.elapsedRealtime() < deadline); Thread.sleep(20) }
    }
    private fun <T> main(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
}
