package com.beatsandbeyond.video_editor.core.domain.usecase.audio

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
 * Unit tests for [SetClipVolumeUseCase].
 */
class SetClipVolumeUseCaseTest {

    private lateinit var useCase: SetClipVolumeUseCase
    private lateinit var project: Project
    private val clipId = "clip_1"

    @Before
    fun setUp() {
        useCase = SetClipVolumeUseCase()
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
    fun `sets volume on clip`() {
        val result = useCase(project, clipId, 1.5f)
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks.first().clips.first()
        assertEquals(1.5f, clip.volume)
    }

    @Test
    fun `clamps volume above 2 to 2`() {
        val result = useCase(project, clipId, 5f)
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks.first().clips.first()
        assertEquals(2.0f, clip.volume)
    }

    @Test
    fun `clamps negative volume to 0`() {
        val result = useCase(project, clipId, -1f)
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks.first().clips.first()
        assertEquals(0.0f, clip.volume)
    }

    @Test
    fun `unknown clip returns error`() {
        val result = useCase(project, "nope", 1f)
        assertTrue(result is AppResult.Error)
    }
}
