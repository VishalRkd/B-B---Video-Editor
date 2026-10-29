package com.bbvideoeditor.app.editor

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bbvideoeditor.app.media.ThumbnailProvider
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.core.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToLong

@RunWith(AndroidJUnit4::class)
class TimelineInvalidationTest {
    @get:Rule val compose = createComposeRule()
    private val counters = TimelineRenderCounters()
    private val pending = CompletableDeferred<Bitmap?>()
    private fun open(): EditorViewModel {
        val media = MediaReference(uri = "content://test/thumbnail-scope", displayName = "scope", kind = MediaKind.VIDEO, durationUs = 30_000_000)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 20_000_000))
        val editor = EditorViewModel(Project(media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "Video", type = TrackType.VIDEO, clips = listOf(clip))))))
        editor.seek(10_123_456); editor.endScrub()
        val provider = ThumbnailProvider { _, _, _, _ -> pending.await() }
        compose.setContent { CompositionLocalProvider(LocalTimelineRenderCounters provides counters) {
            EditorTheme { EditorScreen(editor, provider, onBack = {}, onImport = {}) }
        } }
        compose.waitForIdle()
        return editor
    }

    @Test fun viewportMovesDrawWithoutRecomposingUnchangedClipContent() {
        val editor = open()
        val before = compose.runOnIdle { longArrayOf(counters.timelineCommits, counters.trackCommits, counters.clipCommits, counters.rulerDraws) }
        repeat(30) { step ->
            compose.runOnIdle { editor.seek(10_123_456 + step * 10_000L); editor.endScrub() }
            compose.waitForIdle()
        }
        compose.runOnIdle {
            assertTrue("Viewport did not commit frame updates", counters.timelineCommits > before[0])
            assertEquals(before[1], counters.trackCommits)
            assertEquals(before[2], counters.clipCommits)
            assertTrue("Transport stopped drawing", counters.rulerDraws > before[3])
            assertEquals(10_413_456L, editor.state.value.positionUs)
        }
        compose.onNodeWithContentDescription("Clip scope", substring = true).assertIsDisplayed()
    }

    @Test fun thumbnailCompletionDrawsRealPixelsWithoutRecomposingTheClipOrStrip() {
        open()
        val before = compose.runOnIdle { longArrayOf(counters.trackCommits, counters.clipCommits, counters.thumbnailCommits, counters.thumbnailDraws) }
        pending.complete(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.MAGENTA) })
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(before[0], counters.trackCommits)
            assertEquals(before[1], counters.clipCommits)
            assertEquals(before[2], counters.thumbnailCommits)
            assertTrue(counters.thumbnailDraws > before[3])
        }
        val pixels = compose.onNodeWithContentDescription("Clip scope", substring = true).captureToImage().toPixelMap()
        // The fixed playhead covers the viewport center; sample the image beside it.
        val color = pixels[pixels.width / 3, pixels.height * 3 / 4]
        assertTrue("Completed thumbnail is absent from the real clip drawing: $color", color.red > .9f && color.blue > .9f && color.green < .1f)
    }

    @Test fun coordinateDeliveredOnlyOnPointerUpBecomesTheExactReleaseIntent() {
        val editor = open()
        var release = -1L
        editor.onScrubbingChanged = { if (!it) release = editor.state.value.positionUs }
        val timeline = compose.onNodeWithTag("timeline")
        timeline.performTouchInput {
            down(Offset(width * .7f, 12f))
            moveTo(Offset(width * .7f - 80, 12f), 100)
        }
        val beforeUp = compose.runOnIdle { editor.state.value.positionUs }
        timeline.performTouchInput {
            advanceEventTime(220) // Separate final position from fling velocity history.
            updatePointerTo(0, Offset(width * .7f - 120, 12f))
            up()
        }
        compose.runOnIdle {
            val expected = beforeUp + (40.0 / (72.0 * compose.density.density) * 1_000_000).roundToLong()
            assertEquals(expected, editor.state.value.positionUs)
            assertEquals(expected, release)
            assertFalse(editor.state.value.scrubbing)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun batchedPointerHistoryPreservesReleaseVelocityAndFling() {
        open()
        compose.onNodeWithTag("timeline").performTouchInput {
            val x = width * .7f
            down(Offset(x, 12f))
            moveTo(Offset(x - 40, 12f), 80)
            updatePointerTo(0, Offset(x - 120, 12f))
            moveWithHistory(listOf(-60L, -40L, -20L),
                listOf(Offset(x - 60, 12f), Offset(x - 80, 12f), Offset(x - 100, 12f)), 80)
            up()
        }
        compose.runOnIdle {
            assertTrue("Batched movement lost its release velocity: ${counters.lastReleaseVelocity}",
                counters.lastReleaseVelocity < -100f)
            assertEquals(1L, counters.flingStarts)
            assertTrue(counters.flingFrames >= 2)
        }
    }
}
