package com.bbvideoeditor.core

import org.junit.Assert.*
import org.junit.Test

class TimelineMathTest {
    @Test fun timePixelRoundTripWorksAtLongDurationsAndDifferentZooms() {
        val center = 72L * 3600 * MICROS_PER_SECOND
        listOf(0.25, 80.0, 2400.0).forEach { scale ->
            val viewport = TimelineViewport(center, scale)
            listOf(center - 1_000_000, center, center + 1_000_000).forEach { time ->
                assertEquals(time, viewport.xToTime(viewport.timeToX(time, 1080.0), 1080.0))
            }
            assertEquals(540.0, viewport.timeToX(center, 1080.0), 0.00001)
        }
    }

    @Test fun pinchPreservesTimeUnderFocalPosition() {
        val original = TimelineViewport(50_000_000, 120.0)
        val focalTime = original.xToTime(810.0, 1080.0)
        val zoomed = original.zoom(2.5, 810.0, 1080.0)
        assertEquals(focalTime, zoomed.xToTime(810.0, 1080.0))
        assertEquals(300.0, zoomed.pixelsPerSecond, 0.0)
        assertEquals(original.centerUs, original.zoom(2.0, 540.0, 1080.0).centerUs)
        assertEquals(TimelineViewport.MAX_ZOOM, original.zoom(1000.0, 540.0, 1080.0).pixelsPerSecond, 0.0)
    }

    @Test fun panFollowsFingerContinuouslyAndClampsAtZero() {
        val viewport = TimelineViewport(10_000_000, 100.0)
        assertEquals(9_990_000L, viewport.pan(1.0).centerUs)
        assertEquals(11_000_000L, viewport.pan(-100.0).centerUs)
        assertEquals(0L, viewport.pan(10000.0).centerUs)
    }

    @Test fun snappingChoosesNearestEdgeAndHonorsPixelDerivedThreshold() {
        val targets = listOf(0L, 5_000_000L, 10_000_000L)
        assertEquals(SnapResult(5_000_000, 5_000_000), TimelineSnapper.snap(4_900_000, targets, 150_000))
        assertEquals(SnapResult(4_800_000), TimelineSnapper.snap(4_800_000, targets, 150_000))
        assertEquals(SnapResult(3_000_000, 5_000_000), TimelineSnapper.snapMove(3_100_000, 2_000_000, targets, 150_000))
        assertEquals(0L, TimelineSnapper.snapMove(-1000, 2_000_000, targets, 150_000).timeUs)
    }

    @Test fun viewportQueriesExcludeOffscreenClipsAndUseHalfOpenBounds() {
        val track = Track(name = "Video", type = TrackType.VIDEO,
            clips = (0..9999).map { index -> Clip(id = "$index", mediaId = "m", startUs = index * 2_000_000L, sourceRange = TimeRange(0, 1_000_000)) })
        val index = TrackIndex(track)
        assertEquals(listOf("3000", "3001"), index.visibleClips(6_000_000_000, 6_004_000_000).map { it.id })
        assertEquals("3000", index.clipAt(6_000_000_000)!!.id)
        assertNull(index.clipAt(6_001_000_000))
        assertNull(index.clipAt(-1))
        assertTrue(index.visibleClips(0, 0).isEmpty())
    }

    @Test fun availableGapNeverOverwritesExistingClip() {
        val track = Track(name = "Video", type = TrackType.VIDEO, clips = listOf(
            Clip(id = "a", mediaId = "m", startUs = 0, sourceRange = TimeRange(0, 2_000_000)),
            Clip(id = "b", mediaId = "m", startUs = 5_000_000, sourceRange = TimeRange(0, 2_000_000)),
        ))
        assertEquals(2_000_000L, track.availableStart(1_000_000, 2_000_000))
        assertEquals(7_000_000L, track.availableStart(1_000_000, 4_000_000))
        assertEquals(0L, track.availableStart(0, 2_000_000, "a"))
    }

    @Test fun rulerGenerationIsBoundedByVisibleRange() {
        val step = rulerStepUs(100.0)
        assertEquals(1_000_000L, step)
        val ticks = rulerTicks(1_800_000_123, 1_805_000_000, step).toList()
        assertEquals(listOf(1_801_000_000L, 1_802_000_000L, 1_803_000_000L, 1_804_000_000L, 1_805_000_000L), ticks)
        assertEquals("72:00:00", formatTimecode(72L * 3600 * MICROS_PER_SECOND))
        assertEquals("00:01:15", formatTimecode(1_500_000, frameRate = 30, showFrames = true))
    }

    @Test fun keyframesUseSourceTimeWithHoldAndLinearInterpolation() {
        val curve = AnimatedProperty("x", 0f, listOf(Keyframe(1_000_000, 0f), Keyframe(2_000_000, 1f, Interpolation.HOLD), Keyframe(3_000_000, 0f)))
        assertEquals(0f, curve.valueAt(0))
        assertEquals(0.5f, curve.valueAt(1_500_000))
        assertEquals(1f, curve.valueAt(2_500_000))
        assertEquals(0f, curve.valueAt(4_000_000))
    }
}
