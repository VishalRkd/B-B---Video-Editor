package com.bbvideoeditor.app.playback

import org.junit.Assert.*
import org.junit.Test
import com.bbvideoeditor.core.*

class PreviewCadenceTest {
    private fun plan(kind: MediaKind, animated: Boolean = false): RenderPlan {
        val media = MediaReference(uri = "file:/sample", displayName = "sample", kind = kind, durationUs = 2_000_000, frameRate = 30f)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000),
            properties = if (animated) listOf(AnimatedProperty("opacity", 1f, listOf(Keyframe(0, 0f), Keyframe(1_000_000, 1f)))) else emptyList())
        return RenderPlan.derive(Project(media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "V", type = TrackType.VIDEO,
            clips = listOf(clip, clip.copy(id = newId(), startUs = 2_000_000)))))))
    }
    @Test fun stillsAndGapsCanReusePixelsButCrossingABoundaryCannot() {
        val image = plan(MediaKind.IMAGE)
        assertTrue(samePausedContent(image, 100_000, 900_000, true))
        assertTrue(samePausedContent(image, 1_100_000, 1_200_000, true))
        assertFalse(samePausedContent(image, 900_000, 1_100_000, true))
    }
    @Test fun averageVideoFrameRateNeverInventsDecodedSampleEquivalence() {
        val video = plan(MediaKind.VIDEO)
        assertTrue(samePausedContent(video, 100_001, 100_999, true))
        assertFalse(samePausedContent(video, 100_000, 110_000, true))
    }
    @Test fun sourceTimeAnimationIsNeverSuppressedAsAStaticSample() {
        assertFalse(samePausedContent(plan(MediaKind.IMAGE, true), 100_000, 110_000, true))
        assertFalse(samePausedContent(plan(MediaKind.VIDEO, true), 100_001, 100_999, true))
    }
    @Test fun slowMotionLeavesOneDisplayIntervalAfterNativeCost() {
        val cadence = PreviewCadence(60.0)
        cadence.offer(1_000, 1_000_000_000, 16_666)
        cadence.applied(1_000_000_000, 12_000_000)
        assertEquals(12_000_000L, cadence.waitNs(1_016_666_666, true))
        assertEquals(0L, cadence.waitNs(1_028_666_666, true))
    }
    @Test fun fastMotionCoalescesIntermediateSourceFramesButFinalBypassesTheGate() {
        val cadence = PreviewCadence(60.0)
        cadence.offer(0, 1_000_000_000, 33_333)
        cadence.offer(100_000, 1_016_666_666, 33_333)
        assertTrue(cadence.fast)
        cadence.applied(1_016_666_666, 10_000_000)
        assertTrue(cadence.waitNs(1_033_333_332, true) > 40_000_000)
        assertEquals(0L, cadence.waitNs(1_033_333_332, false))
        cadence.endGesture(); assertFalse(cadence.fast)
    }
    @Test fun ninetyAndOneHundredTwentyHzUseTheirActualDisplayIntervals() {
        val ninety = PreviewCadence(90.0); val fast = PreviewCadence(120.0)
        assertEquals(11_111_111, ninety.displayPeriodNs)
        assertEquals(8_333_333, fast.displayPeriodNs)
        fast.offer(0, 1_000_000_000, 8_333)
        fast.applied(1_000_000_000, 1_000_000)
        assertEquals(0, fast.waitNs(1_010_000_000, true))
        fast.refreshRate(60.0); assertEquals(16_666_666, fast.displayPeriodNs)
    }
    @Test fun slowVideoScrubDoesNotRequestMoreSamplesThanItsUsefulCadence() {
        val cadence = PreviewCadence(120.0)
        cadence.offer(0, 1_000_000_000, 33_333)
        cadence.applied(1_000_000_000, 1_000_000)
        assertTrue(cadence.waitNs(1_010_000_000, true) > 20_000_000)
        assertEquals(0L, cadence.waitNs(1_033_333_000, true))
        assertEquals(0L, cadence.waitNs(1_010_000_000, false))
    }
    @Test fun lifecycleResetDropsOldNativeCostAndGestureVelocity() {
        val cadence = PreviewCadence()
        cadence.applied(1_000_000_000, 100_000_000)
        cadence.reset()
        assertEquals(0, cadence.waitNs(1_001_000_000, true))
        cadence.refreshRate(Double.NaN); assertEquals(16_666_666, cadence.displayPeriodNs)
    }
}
