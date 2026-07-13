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
 * Unit tests for [TrimClipUseCase].
 */
class TrimClipUseCaseTest {

    private lateinit var useCase: TrimClipUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = TrimClipUseCase(KotlinTimelineEngine())

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
    fun `trim clip successfully updates trim bounds`() {
        val result = useCase(project, clipId, 2000L, 8000L)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val updatedClip = updatedProject.timeline.tracks.first().clips.first()
        assertEquals(2000L, updatedClip.trimStartMs)
        assertEquals(8000L, updatedClip.trimEndMs)
    }

    @Test
    fun `trim start negative returns error`() {
        val result = useCase(project, clipId, -1000L, 8000L)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `trim end less than start returns error`() {
        val result = useCase(project, clipId, 5000L, 4000L)
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `trim non existent clip returns error`() {
        val result = useCase(project, "invalid_id", 1000L, 5000L)
        assertTrue(result is AppResult.Error)
    }
}
