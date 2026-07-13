package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
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
 * Unit tests for [SetSpeedUseCase].
 */
class SetSpeedUseCaseTest {

    private lateinit var useCase: SetSpeedUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = SetSpeedUseCase()

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
    fun `set clip speed factor updates speed multiplier`() {
        val result = useCase(project, clipId, 2.0f)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val updatedClip = updatedProject.timeline.tracks.first().clips.first()
        assertEquals(2.0f, updatedClip.speedFactor)
    }

    @Test
    fun `set speed out of bounds returns error`() {
        val result1 = useCase(project, clipId, 0.1f)
        val result2 = useCase(project, clipId, 5.0f)

        assertTrue(result1 is AppResult.Error)
        assertTrue(result2 is AppResult.Error)
    }
}
