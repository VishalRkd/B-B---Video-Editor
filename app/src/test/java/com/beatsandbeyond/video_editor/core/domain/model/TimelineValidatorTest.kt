package com.beatsandbeyond.video_editor.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineValidatorTest {

    @Test
    fun `valid timeline has zero validation errors`() {
        val clip = Clip(
            id = "clip_1",
            assetId = "asset_1",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 5000L,
            speedFactor = 1.0f,
            volume = 1.0f
        )
        val track = Track(
            id = "track_1",
            type = TrackType.VIDEO,
            clips = listOf(clip)
        )
        val timeline = Timeline(
            id = "timeline_1",
            tracks = listOf(track)
        )

        val errors = TimelineValidator.validate(timeline) { 10000L }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `detects duplicate clip ids`() {
        val clip1 = Clip("c1", "a1", 0L, 0L, 2000L)
        val clip2 = Clip("c1", "a2", 3000L, 0L, 2000L) // duplicate ID c1
        val track = Track("track_1", TrackType.VIDEO, listOf(clip1, clip2))
        val timeline = Timeline("timeline_1", listOf(track))

        val errors = TimelineValidator.validate(timeline)
        assertEquals(1, errors.size)
        assertTrue(errors.first() is TimelineValidationError.DuplicateClipId)
    }

    @Test
    fun `detects invalid trim points`() {
        val clipErr = Clip("c1", "a1", 0L, 5000L, 3000L) // trimEndMs <= trimStartMs
        val track = Track("track_1", TrackType.VIDEO, listOf(clipErr))
        val timeline = Timeline("timeline_1", listOf(track))

        val errors = TimelineValidator.validate(timeline)
        assertEquals(1, errors.size)
        assertTrue(errors.first() is TimelineValidationError.InvalidTrimPoints)
    }

    @Test
    fun `detects trim exceeding asset duration`() {
        val clipErr = Clip("c1", "a1", 0L, 0L, 12000L)
        val track = Track("track_1", TrackType.VIDEO, listOf(clipErr))
        val timeline = Timeline("timeline_1", listOf(track))

        val errors = TimelineValidator.validate(timeline) { 10000L } // asset duration is 10000
        assertEquals(1, errors.size)
        assertTrue(errors.first() is TimelineValidationError.TrimExceedsAssetDuration)
    }

    @Test
    fun `detects out of bounds speed factor`() {
        val clipErr = Clip("c1", "a1", 0L, 0L, 2000L, speedFactor = 5.0f)
        val track = Track("track_1", TrackType.VIDEO, listOf(clipErr))
        val timeline = Timeline("timeline_1", listOf(track))

        val errors = TimelineValidator.validate(timeline)
        assertEquals(1, errors.size)
        assertTrue(errors.first() is TimelineValidationError.InvalidSpeedFactor)
    }

    @Test
    fun `detects clips overlap on the same track`() {
        val clip1 = Clip("c1", "a1", 0L, 0L, 2000L) // ends at 2000
        val clip2 = Clip("c2", "a2", 1500L, 0L, 2000L) // starts at 1500, overlaps!
        val track = Track("track_1", TrackType.VIDEO, listOf(clip1, clip2))
        val timeline = Timeline("timeline_1", listOf(track))

        val errors = TimelineValidator.validate(timeline)
        assertEquals(1, errors.size)
        assertTrue(errors.first() is TimelineValidationError.ClipsOverlap)
    }
}
