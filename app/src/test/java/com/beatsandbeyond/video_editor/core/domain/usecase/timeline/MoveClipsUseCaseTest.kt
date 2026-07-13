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
 * Unit tests for [MoveClipsUseCase].
 */
class MoveClipsUseCaseTest {

    private lateinit var useCase: MoveClipsUseCase
    private lateinit var project: Project
    private val clipId1 = "clip_1"
    private val clipId2 = "clip_2"

    @Before
    fun setUp() {
        useCase = MoveClipsUseCase(KotlinTimelineEngine())

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
    fun `move multiple clips shifts all target clips`() {
        val deltaMs = 2000L
        val result = useCase(project, setOf(clipId1, clipId2), deltaMs)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val clips = updatedProject.timeline.tracks.first().clips
        
        assertEquals(2000L, clips.find { it.id == clipId1 }?.timelinePositionMs)
        assertEquals(7000L, clips.find { it.id == clipId2 }?.timelinePositionMs)
    }

    @Test
    fun `move into a stationary clip is rejected`() {
        val deltaMs = -1000L
        // Moving clip2 back by 1000ms lands it at 4000, overlapping clip1 (ends at 5000).
        val result = useCase(project, setOf(clipId2), deltaMs)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `move multiple clips with negative delta shifts back`() {
        val deltaMs = -1000L
        // Move BOTH clips back together — spacing preserved, no overlap.
        val result = useCase(project, setOf(clipId1, clipId2), deltaMs)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val clips = updatedProject.timeline.tracks.first().clips

        assertEquals(0L, clips.find { it.id == clipId1 }?.timelinePositionMs)
        assertEquals(4000L, clips.find { it.id == clipId2 }?.timelinePositionMs)
    }
}
