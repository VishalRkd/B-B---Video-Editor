package com.bbvideoeditor.app.render

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@UnstableApi
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
@RunWith(AndroidJUnit4::class)
class InteractivePreviewAdmissionTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rapidInteractiveLayeredRequestsFinishAtExactSourcePixelsOnOneExecutor() {
        val fixtures = TemporalFixtures(context)
        val base = fixtures.video(fps = 30, source = 1)
        val overlay = fixtures.video(fps = 24, source = 2)
        val project = Project(canvas = CanvasSettings(320, 180), media = listOf(base, overlay).associateBy { it.id }, timeline = Timeline(listOf(
            Track(name = "base", type = TrackType.VIDEO, clips = listOf(Clip(mediaId = base.id, startUs = 0, sourceRange = TimeRange(200_000, 2_400_000)))),
            Track(name = "overlay", type = TrackType.OVERLAY, clips = listOf(Clip(mediaId = overlay.id, startUs = 0, sourceRange = TimeRange(400_000, 2_400_000),
                transform = Transform(scaleX = .25f, scaleY = .25f, translationX = .65f)))))))
        PreviewTestSession(project).use { session ->
            session.await("initial output") { session.engine.state.value.submittedPositionUs != null }
            session.main { session.engine.setScrubbing(true) }
            repeat(120) { index -> session.main { session.engine.seek(index * 37_000L % 2_300_000) }; Thread.sleep(8) }
            session.main { session.engine.seek(850_000); session.engine.setScrubbing(false) }
            session.await("final pointer-up receipt") {
                val state = session.engine.state.value
                check(state.error == null) { "$state; ${session.main { session.engine.diagnostics.events() }}" }
                state.submittedGeneration == state.requestedGeneration && !state.buffering && state.appliedPositionUs == 850_000L
            }
            session.await("both exact source identities after interactive release", 15_000) {
                val frame = session.frame() ?: return@await false
                try {
                    val first = fixtures.decode(frame)
                    val second = Bitmap.createBitmap(frame, (frame.width * .7f).roundToInt(), (frame.height * .375f).roundToInt(),
                        (frame.width * .25f).roundToInt(), (frame.height * .25f).roundToInt())
                    val last = try { fixtures.decode(second) } finally { second.recycle() }
                    first?.source == 1 && abs(first.frame - 31) <= 1 && last?.source == 2 && abs(last.frame - 30) <= 1
                } finally { frame.recycle() }
            }
            session.main {
                val metrics = session.engine.diagnostics.snapshot()
                assertEquals(1L, metrics.playerCreates); assertEquals(0L, metrics.playerReleases)
                assertEquals(1L, metrics.compositionInstalls); assertEquals(1L, metrics.prepares)
                assertTrue("Intermediate interactive work was not coalesced: $metrics", metrics.nativeSeeks < 100)
                assertFalse((session.engine.state.value.player as CompositionPlayer).isScrubbingModeEnabled)
            }
        }
    }

    @Test fun pauseDuringScrubRemainsPausedAfterNativeFinalFlushAndPlayStillWorks() {
        val source = MediaFixtures(context).video("interactive-play-intent", .2f, .6f, .4f)
        val clip = Clip(mediaId = source.id, startUs = 0, sourceRange = TimeRange(0, 1_800_000))
        val project = Project(canvas = CanvasSettings(320, 180), media = mapOf(source.id to source),
            timeline = Timeline(listOf(Track(name = "V", type = TrackType.VIDEO, clips = listOf(clip)))))
        PreviewTestSession(project).use { session ->
            session.await("initial output") { session.engine.state.value.submittedPositionUs != null }
            repeat(3) {
                session.main { session.engine.play() }
                session.await("natural playback") { session.engine.state.value.playing }
                session.main { session.engine.pause(); session.engine.setScrubbing(true) }
                repeat(12) { step -> session.main { session.engine.seek(200_000 + step * 30_000L) }; Thread.sleep(16) }
                session.main { session.engine.pause(); session.engine.seek(850_000); session.engine.setScrubbing(false) }
                session.await("paused exact final output") {
                    val state = session.engine.state.value
                    state.error == null && !state.buffering && state.submittedGeneration == state.requestedGeneration
                }
                Thread.sleep(150)
                session.main {
                    val state = session.engine.state.value
                    assertFalse(state.playWhenReady); assertFalse(state.playing)
                    assertFalse(requireNotNull(state.player).playWhenReady)
                    assertFalse((state.player as CompositionPlayer).isScrubbingModeEnabled)
                }
            }
            assertEquals(1L, session.main { session.engine.diagnostics.snapshot().playerCreates })
        }
    }
}
