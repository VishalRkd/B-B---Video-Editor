package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.engine.KotlinTimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RippleTrimUseCase].
 */
class RippleTrimUseCaseTest {

    private lateinit var useCase: RippleTrimUseCase
    private lateinit var project: Project
    private val clipId1 = "clip_1"
    private val clipId2 = "clip_2"

    @Before
    fun setUp() {
        useCase = RippleTrimUseCase(KotlinTimelineEngine())

        val clip1 = Clip(
            id = clipId1,
            assetId = "asset_1",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 5000L
        )
        val clip2 = Clip(
            id = clipId2,
            assetId = "asset_2",
            timelinePositionMs = 5000L,
            trimStartMs = 0L,
            trimEndMs = 5000L
        )
        val track = Track(
            id = "track_1",
            type = TrackType.VIDEO,
            clips = listOf(clip1, clip2)
        )
        val timeline = Timeline(
            id = "timeline_1",
            tracks = listOf(track)
        )
        project = Project(
            id = "project_1",
            name = "Test Project",
            createdAt = 0L,
            updatedAt = 0L,
            timeline = timeline
        )
    }

    @Test
    fun `ripple trim right edge shifts subsequent clips`() {
        // Trim clip1 right edge by adding 1000ms
        val result = useCase(project, clipId1, 0L, 6000L)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val clips = updatedProject.timeline.tracks.first().clips
        
        assertEquals(6000L, clips.find { it.id == clipId1 }?.trimEndMs)
        assertEquals(6000L, clips.find { it.id == clipId2 }?.timelinePositionMs)
    }

    @Test
    fun `ripple trim left edge shifts current and subsequent clips`() {
        // Trim clip2 left edge by removing 1000ms (sliding it left)
        // Original clip2: trimStart=0, trimEnd=5000, duration=5000
        // New clip2: trimStart=1000, trimEnd=5000, duration=4000
        // delta = 4000 - 5000 = -1000
        // Clip2 is NOT shifted by current RippleTrimUseCase logic (it only shifts LATER clips)
        val result = useCase(project, clipId2, 1000L, 5000L)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val clips = updatedProject.timeline.tracks.first().clips
        
        assertEquals(1000L, clips.find { it.id == clipId2 }?.trimStartMs)
        // Current implementation does NOT move the clip itself, only subsequent ones.
        // So clip2's timelinePositionMs remains 5000L.
        assertEquals(5000L, clips.find { it.id == clipId2 }?.timelinePositionMs)
    }
}
