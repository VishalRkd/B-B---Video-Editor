package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ApplyFilterUseCase].
 */
class ApplyFilterUseCaseTest {

    private lateinit var useCase: ApplyFilterUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = ApplyFilterUseCase()
        val clip = Clip(
            id = clipId, assetId = "asset_1", timelinePositionMs = 0L,
            trimStartMs = 0L, trimEndMs = 10000L,
        )
        val track = Track(id = "track_1", type = TrackType.VIDEO, clips = listOf(clip))
        project = Project(
            id = "project_1", name = "Test", createdAt = 0L, updatedAt = 0L,
            timeline = Timeline(id = "timeline_1", tracks = listOf(track)),
        )
    }

    @Test
    fun `applies filter to clip`() {
        val result = useCase(project, clipId, VideoFilter.Vintage(0.7f))
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks.first().clips.first()
        assertEquals(VideoFilter.Vintage(0.7f), clip.filter)
    }

    @Test
    fun `unknown clip returns error`() {
        val result = useCase(project, "nope", VideoFilter.None)
        assertTrue(result is AppResult.Error)
    }
}
