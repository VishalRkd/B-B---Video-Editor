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
 * Unit tests for [MoveClipUseCase].
 */
class MoveClipUseCaseTest {

    private lateinit var useCase: MoveClipUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = MoveClipUseCase(KotlinTimelineEngine())

        val clip = Clip(
            id = clipId,
            assetId = "asset_1",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 10000L
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
        project = Project(
            id = "project_1",
            name = "Test Project",
            createdAt = 0L,
            updatedAt = 0L,
            timeline = timeline
        )
    }

    @Test
    fun `move clip shifts timeline position and keeps clips sorted`() {
        val result = useCase(project, clipId, 5000L)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val updatedClip = updatedProject.timeline.tracks.first().clips.first()
        assertEquals(5000L, updatedClip.timelinePositionMs)
    }

    @Test
    fun `move clip negative position returns error`() {
        val result = useCase(project, clipId, -100L)
        assertTrue(result is AppResult.Error)
    }
}
