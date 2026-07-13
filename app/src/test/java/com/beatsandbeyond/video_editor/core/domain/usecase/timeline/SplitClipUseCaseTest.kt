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
 * Unit tests for [SplitClipUseCase].
 */
class SplitClipUseCaseTest {

    private lateinit var useCase: SplitClipUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = SplitClipUseCase(KotlinTimelineEngine())

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
    fun `split clip at middle divides it into two clips`() {
        val result = useCase(project, clipId, 4000L)

        assertTrue(result is AppResult.Success)
        val updatedProject = (result as AppResult.Success).data
        val clips = updatedProject.timeline.tracks.first().clips
        assertEquals(2, clips.size)

        val left = clips[0]
        val right = clips[1]

        assertEquals(0L, left.timelinePositionMs)
        assertEquals(0L, left.trimStartMs)
        assertEquals(4000L, left.trimEndMs)

        assertEquals(4000L, right.timelinePositionMs)
        assertEquals(4000L, right.trimStartMs)
        assertEquals(10000L, right.trimEndMs)
    }

    @Test
    fun `split position out of bounds returns error`() {
        val result1 = useCase(project, clipId, 0L)
        val result2 = useCase(project, clipId, 10000L)
        val result3 = useCase(project, clipId, 12000L)

        assertTrue(result1 is AppResult.Error)
        assertTrue(result2 is AppResult.Error)
        assertTrue(result3 is AppResult.Error)
    }
}
