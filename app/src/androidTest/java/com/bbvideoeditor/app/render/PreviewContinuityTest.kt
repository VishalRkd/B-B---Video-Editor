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
import com.bbvideoeditor.app.playback.PreviewState
import com.bbvideoeditor.core.CanvasSettings
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.TimeRange
import com.bbvideoeditor.core.Timeline
import com.bbvideoeditor.core.Track
import com.bbvideoeditor.core.TrackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Checks the frames and transport throughout natural playback, including every visual boundary. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class PreviewContinuityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun naturalClipAndGapBoundariesAndPausedSeeksKeepOnePlayerAndCorrectFrames() {
        val fixtures = MediaFixtures(context)
        val red = fixtures.video("continuity-red", 1f, 0f, 0f)
        val blue = fixtures.video("continuity-blue", 0f, 0f, 1f)
        val audio = fixtures.audio()
        val project = Project(
            canvas = CanvasSettings(320, 180, 30),
            media = listOf(red, blue, audio).associateBy { it.id },
            timeline = Timeline(listOf(
                Track(name = "Video", type = TrackType.VIDEO, clips = listOf(
                    Clip(mediaId = red.id, startUs = 0, sourceRange = TimeRange(300_000, 1_600_000)),
                    Clip(mediaId = blue.id, startUs = 2_400_000, sourceRange = TimeRange(200_000, 1_600_000)),
                )),
                Track(name = "Audio", type = TrackType.AUDIO, clips = listOf(
                    Clip(mediaId = audio.id, startUs = 0, sourceRange = TimeRange(0, 2_000_000), volume = .25f),
                )),
            )),
        )
        // Windows stay several frames away from boundaries, allowing for PixelCopy/display delay.
        val windows = listOf(
            ColorWindow("first trimmed video", 450_000, 1_450_000, Color.RED, Color.RED),
            ColorWindow("empty video gap", 1_750_000, 2_250_000, Color.BLACK, Color.BLACK),
            ColorWindow("second trimmed video", 2_550_000, 3_750_000, Color.BLUE, Color.BLUE),
        )
        assertContinuousPreview(project, windows, listOf(
            700_000L to Color.RED,
            2_800_000L to Color.BLUE,
            2_000_000L to Color.BLACK,
            700_000L to Color.RED,
        ))
    }

    @Test fun adjacentSpeedAdjustedVideosAndImageBoundariesKeepOnePlayerAcrossRepeatedSeeks() {
        val fixtures = MediaFixtures(context)
        val red = fixtures.video("continuity-speed-red", 1f, 0f, 0f)
        val blue = fixtures.video("continuity-speed-blue", 0f, 0f, 1f)
        val white = fixtures.image("continuity-white", Color.WHITE)
        val project = Project(canvas = CanvasSettings(320, 180, 30),
            media = listOf(red, blue, white).associateBy { it.id },
            timeline = Timeline(listOf(Track(name = "Video", type = TrackType.VIDEO, clips = listOf(
                Clip(mediaId = red.id, startUs = 0, sourceRange = TimeRange(200_000, 1_600_000), speed = 2f),
                Clip(mediaId = blue.id, startUs = 800_000, sourceRange = TimeRange(200_000, 800_000), speed = .5f),
                Clip(mediaId = white.id, startUs = 2_400_000, sourceRange = TimeRange(0, 600_000)),
                Clip(mediaId = red.id, startUs = 3_000_000, sourceRange = TimeRange(400_000, 1_000_000)),
            )))))
        // The cut times distinguish sped-up/slowed-down clip durations from unmodified media.
        val windows = listOf(
            ColorWindow("double-speed red", 200_000, 650_000, Color.RED, Color.RED),
            ColorWindow("adjacent half-speed blue", 950_000, 2_250_000, Color.BLUE, Color.BLUE),
            ColorWindow("video to image", 2_550_000, 2_850_000, Color.WHITE, Color.WHITE),
            ColorWindow("image to video", 3_150_000, 3_750_000, Color.RED, Color.RED),
        )
        val seekCycle = listOf(1_500_000L to Color.BLUE, 2_700_000L to Color.WHITE,
            3_500_000L to Color.RED, 400_000L to Color.RED)
        assertContinuousPreview(project, windows, seekCycle + seekCycle)
    }

    private fun assertContinuousPreview(project: Project, windows: List<ColorWindow>,
                                        pausedSeeks: List<Pair<Long, Int>>) {
        val engine = onMain { PreviewEngine(context, CompositionCompiler(context)) }
        val trace = PlaybackTrace(project.durationUs)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var view: PlayerView? = null
            var binding: Job? = null
            try {
                scenario.onActivity { activity ->
                    view = PlayerView(activity).apply { useController = false }
                    activity.setContentView(view)
                    binding = CoroutineScope(Dispatchers.Main.immediate).launch {
                        engine.state.collect { state ->
                            view!!.player = state.player
                            trace.record(state)
                        }
                    }
                    engine.updateProject(project)
                }
                await("initial composition and decoder") {
                    assertNull("Preview failed during startup", engine.state.value.error)
                    onMain { engine.state.value.player?.playbackState == Player.STATE_READY }
                }
                await("initial red frame before playback") {
                    screenshot(view!!)?.let { frame ->
                        try { matches(pixel(frame, .5f, .5f), Color.RED) }
                        finally { frame.recycle() }
                    } ?: false
                }

                val initialPlayer = onMain { requireNotNull(engine.state.value.player) }
                // No seek is performed until playback finishes: every item crossing is natural.
                onMain { engine.play() }
                await("playback start") { onMain { trace.startedAtMs != null } }
                val deadlineMs = SystemClock.elapsedRealtime() + 12_000
                while (!onMain { engine.state.value.player?.playbackState == Player.STATE_ENDED }) {
                    assertNull("Preview failed during natural playback", engine.state.value.error)
                    assertTrue("Playback did not reach the end: ${onMain { trace.describe() }}",
                        SystemClock.elapsedRealtime() < deadlineMs)
                    val beforeUs = engine.state.value.positionUs
                    val frame = screenshot(view!!)
                    val afterUs = engine.state.value.positionUs
                    if (frame != null) {
                        try {
                            windows.filter { beforeUs >= it.startUs && afterUs <= it.endUs && afterUs >= beforeUs }
                                .forEach { window ->
                                    val center = pixel(frame, .5f, .5f)
                                    val corner = pixel(frame, .75f, .25f)
                                    assertTrue("${window.name} at $beforeUs..$afterUs us: center expected " +
                                        "${hex(window.center)}, got ${hex(center)}; ${onMain { trace.describe() }}",
                                        matches(center, window.center))
                                    assertTrue("${window.name} at $beforeUs..$afterUs us: corner expected " +
                                        "${hex(window.corner)}, got ${hex(corner)}; ${onMain { trace.describe() }}",
                                        matches(corner, window.corner))
                                    window.samples++
                                }
                        } finally { frame.recycle() }
                    }
                    Thread.sleep(20)
                }
                val elapsedMs = onMain { SystemClock.elapsedRealtime() - requireNotNull(trace.startedAtMs) }
                val timelineMs = onMain { (project.durationUs - trace.startPositionUs) / 1000 }
                val diagnostics = onMain { trace.describe() }
                windows.forEach { window ->
                    assertTrue("No stable frame sampled for ${window.name} (${window.startUs}..${window.endUs} us); $diagnostics",
                        window.samples > 0)
                }
                onMain {
                    assertTrue("Player detached or replaced at a natural boundary: $diagnostics", trace.playerChanges.isEmpty())
                    assertEquals("Playback rebuffered after startup: $diagnostics", 0, trace.playbackBufferingEvents)
                    assertTrue("Clock moved backwards: $diagnostics", trace.clockRegressions.isEmpty())
                    assertTrue("Playback paused before the end: $diagnostics", trace.earlyStops.isEmpty())
                }
                assertTrue("Playback took ${elapsedMs}ms for ${timelineMs}ms of timeline; $diagnostics",
                    elapsedMs <= timelineMs + 2_000)
                assertTrue("Playback ran too fast: ${elapsedMs}ms for ${timelineMs}ms; $diagnostics",
                    elapsedMs >= timelineMs - 350)
                assertFalse("Player should stop at the project end", engine.state.value.playing)
                assertNull(engine.state.value.error)

                onMain { trace.recording = false }
                pausedSeeks.forEach { (positionUs, color) ->
                    assertPausedSeek(engine, view!!, initialPlayer, positionUs, color)
                }
            } finally {
                onMain { binding?.cancel(); view?.player = null; engine.close() }
            }
        }
    }

    private data class ColorWindow(
        val name: String,
        val startUs: Long,
        val endUs: Long,
        val center: Int,
        val corner: Int,
        var samples: Int = 0,
    )

    /** All access occurs on the main thread; observes every emission, not just screenshot samples. */
    private class PlaybackTrace(private val durationUs: Long) {
        var recording = true
        var startedAtMs: Long? = null
        var startPositionUs = 0L
        var startupBufferingEvents = 0
        var playbackBufferingEvents = 0
        val playerChanges = mutableListOf<String>()
        val clockRegressions = mutableListOf<String>()
        val earlyStops = mutableListOf<Long>()
        private var expectedPlayer: Player? = null
        private var lastPlayer: Player? = null
        private var lastPositionUs = 0L
        private var wasBuffering = false
        private var wasPlaying = false

        fun record(state: PreviewState) {
            if (!recording) return
            if (startedAtMs == null && state.playing && state.player != null) {
                startedAtMs = SystemClock.elapsedRealtime()
                startPositionUs = state.positionUs
                expectedPlayer = state.player
                lastPlayer = state.player
                lastPositionUs = state.positionUs
            }
            if (state.buffering && !wasBuffering) {
                if (startedAtMs == null) startupBufferingEvents++ else playbackBufferingEvents++
            }
            if (startedAtMs != null) {
                if (state.player !== expectedPlayer && state.player !== lastPlayer) {
                    playerChanges += "${state.positionUs}us:${state.player?.let(System::identityHashCode) ?: "null"}"
                }
                if (state.positionUs + 2_000 < lastPositionUs) {
                    clockRegressions += "$lastPositionUs -> ${state.positionUs}"
                }
                if (wasPlaying && !state.playing && state.positionUs < durationUs - 100_000) {
                    earlyStops += state.positionUs
                }
                lastPositionUs = state.positionUs
                lastPlayer = state.player
            }
            wasBuffering = state.buffering
            wasPlaying = state.playing
        }

        fun describe() = "position=${lastPositionUs}us, startupBuffering=$startupBufferingEvents, " +
            "playbackBuffering=$playbackBufferingEvents, playerChanges=$playerChanges, " +
            "clockRegressions=$clockRegressions, earlyStops=$earlyStops"
    }

    private fun assertPausedSeek(engine: PreviewEngine, view: PlayerView, expectedPlayer: Player,
                                 positionUs: Long, expectedColor: Int) {
        onMain { engine.seek(positionUs) }
        await("paused seek to $positionUs us, expected ${hex(expectedColor)}") {
            val state = engine.state.value
            assertNull("Preview failed seeking to $positionUs us", state.error)
            assertTrue("Paused seek to $positionUs us replaced or detached the player", state.player === expectedPlayer)
            assertFalse("Paused seek to $positionUs us unexpectedly started playback", state.playing)
            val frame = screenshot(view) ?: return@await false
            try {
                abs(state.positionUs - positionUs) <= 2_000 && matches(pixel(frame, .5f, .5f), expectedColor)
            } finally { frame.recycle() }
        }
    }

    private fun screenshot(view: PlayerView): Bitmap? {
        val surface = onMain { view.videoSurfaceView as SurfaceView }
        val dimensions = onMain { surface.width to surface.height }
        if (dimensions.first <= 0 || dimensions.second <= 0 || !surface.holder.surface.isValid) return null
        val bitmap = Bitmap.createBitmap(dimensions.first, dimensions.second, Bitmap.Config.ARGB_8888)
        val completion = CountDownLatch(1)
        var result = -1
        PixelCopy.request(surface, bitmap, { result = it; completion.countDown() }, Handler(Looper.getMainLooper()))
        assertTrue("PixelCopy timed out during continuity capture", completion.await(5, TimeUnit.SECONDS))
        if (result == PixelCopy.ERROR_SOURCE_NO_DATA || result == PixelCopy.ERROR_SOURCE_INVALID) {
            bitmap.recycle()
            return null
        }
        assertEquals("PixelCopy failed during continuity capture", PixelCopy.SUCCESS, result)
        return bitmap
    }

    private fun pixel(bitmap: Bitmap, x: Float, y: Float): Int {
        val width = minOf(bitmap.width.toFloat(), bitmap.height * 16f / 9f)
        val height = width * 9f / 16f
        return bitmap.getPixel(
            ((bitmap.width - width) / 2f + width * x).toInt(),
            ((bitmap.height - height) / 2f + height * y).toInt(),
        )
    }

    private fun matches(actual: Int, expected: Int) =
        abs(Color.red(actual) - Color.red(expected)) < 65 &&
            abs(Color.green(actual) - Color.green(expected)) < 65 &&
            abs(Color.blue(actual) - Color.blue(expected)) < 65

    private fun hex(color: Int) = Integer.toHexString(color)

    private fun await(label: String, predicate: () -> Boolean) {
        val deadlineMs = SystemClock.elapsedRealtime() + 20_000
        while (!predicate()) {
            if (SystemClock.elapsedRealtime() >= deadlineMs) fail("Timed out waiting for $label")
            Thread.sleep(20)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
}
