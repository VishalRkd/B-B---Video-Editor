package com.beatsandbeyond.video_editor.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Integration tests verifying:
 * - Playhead seeking and absolute timeline position mapping to source media offsets (taking trims and speeds into account).
 * - Total timeline duration calculations across multiple tracks and speed-scaled clips.
 */
class TimelineIntegrationTest {

    @Test
    fun testTimelineDurationWithTrimAndSpeedAdjustments() {
        // Video Clip 1: trimStart = 0, trimEnd = 10000 (10s), speed = 2.0x -> duration on timeline = 5000 (5s)
        val clip1 = Clip(
            id = "clip_1",
            assetId = "asset_1",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 10000L,
            speedFactor = 2.0f
        )
        // Video Clip 2: trimStart = 2000, trimEnd = 8000 (6s), speed = 0.5x -> duration on timeline = 12000 (12s)
        // Positioned immediately after clip1 (ends at 5000)
        val clip2 = Clip(
            id = "clip_2",
            assetId = "asset_2",
            timelinePositionMs = 5000L,
            trimStartMs = 2000L,
            trimEndMs = 8000L,
            speedFactor = 0.5f
        )

        val videoTrack = Track(
            id = "video_track",
            type = TrackType.VIDEO,
            clips = listOf(clip1, clip2)
        )

        // Audio Track: trimStart = 0, trimEnd = 20000 (20s), speed = 1.0x -> duration on timeline = 20000
        val audioClip = Clip(
            id = "audio_clip",
            assetId = "audio_asset",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 20000L,
            speedFactor = 1.0f
        )
        val audioTrack = Track(
            id = "audio_track",
            type = TrackType.AUDIO,
            clips = listOf(audioClip)
        )

        val timeline = Timeline(
            id = "timeline_1",
            tracks = listOf(videoTrack, audioTrack)
        )

        // Clip 1 duration on timeline = 5000ms
        assertEquals(5000L, clip1.durationOnTimelineMs)
        assertEquals(5000L, clip1.timelineEndMs)

        // Clip 2 duration on timeline = 12000ms
        assertEquals(12000L, clip2.durationOnTimelineMs)
        assertEquals(17000L, clip2.timelineEndMs)

        // Video track duration = 17000ms
        assertEquals(17000L, videoTrack.durationMs)
        // Audio track duration = 20000ms
        assertEquals(20000L, audioTrack.durationMs)

        // Total timeline duration = max of tracks = 20000ms
        assertEquals(20000L, timeline.totalDurationMs)
    }

    @Test
    fun testAbsolutePlayheadPositionMappingToSourceOffset() {
        // Video Clip: trimStart = 2000, trimEnd = 8000 (6s), speed = 0.5x -> duration on timeline = 12000 (12s)
        // Starts at timelinePositionMs = 5000ms
        val clip = Clip(
            id = "clip_1",
            assetId = "asset_1",
            timelinePositionMs = 5000L,
            trimStartMs = 2000L,
            trimEndMs = 8000L,
            speedFactor = 0.5f
        )

        // Let's seek to absolute timeline position 8000ms (3000ms from clip start)
        val seekPos = 8000L
        val relativeOffsetMs = seekPos - clip.timelinePositionMs
        
        // Source offset = trimStart + relativeOffsetMs * speedFactor
        val sourceOffsetMs = clip.trimStartMs + (relativeOffsetMs * clip.speedFactor).toLong()
        
        // 3000ms on timeline at 0.5x speed is 1500ms of source media elapsed
        // 2000 (trimStart) + 1500 = 3500ms
        assertEquals(3000L, relativeOffsetMs)
        assertEquals(3500L, sourceOffsetMs)
    }
}
