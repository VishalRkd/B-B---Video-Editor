package com.beatsandbeyond.video_editor.core.domain.usecase.audio

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddAudioTrackUseCase].
 */
class AddAudioTrackUseCaseTest {

    private lateinit var useCase: AddAudioTrackUseCase
    private lateinit var project: Project
    private lateinit var audioAsset: Asset

    @Before
    fun setUp() {
        useCase = AddAudioTrackUseCase()
        audioAsset = Asset(
            id = "audio_1",
            mediaUri = "content://audio/1",
            mediaType = MediaType.AUDIO,
            displayName = "song.mp3",
            durationMs = 30000L,
            width = 0,
            height = 0,
            sizeBytes = 1024L,
            importedAt = 0L,
        )
        val timeline = Timeline(id = "timeline_1", tracks = emptyList())
        project = Project(
            id = "project_1",
            name = "Test",
            createdAt = 0L,
            updatedAt = 0L,
            timeline = timeline,
        )
    }

    @Test
    fun `adds audio track when none exists`() {
        val result = useCase(project, audioAsset)
        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        val audioTrack = updated.timeline.tracks.first { it.type == TrackType.AUDIO }
        assertEquals(1, audioTrack.clips.size)
        assertEquals("audio_1", audioTrack.clips.first().assetId)
    }

    @Test
    fun `appends to existing audio track at end`() {
        val existing = Track(
            id = "audio_track",
            type = TrackType.AUDIO,
            clips = listOf(
                com.beatsandbeyond.video_editor.core.domain.model.Clip(
                    id = "c1", assetId = "other", timelinePositionMs = 0L,
                    trimStartMs = 0L, trimEndMs = 10000L,
                )
            ),
        )
        val timeline = Timeline(id = "t", tracks = listOf(existing))
        val proj = project.copy(timeline = timeline)

        val result = useCase(proj, audioAsset)
        assertTrue(result is AppResult.Success)
        val audioTrack = (result as AppResult.Success).data.timeline.tracks.first { it.type == TrackType.AUDIO }
        assertEquals(2, audioTrack.clips.size)
        // New clip appended after the existing 10s clip.
        assertEquals(10000L, audioTrack.clips.last().timelinePositionMs)
    }

    @Test
    fun `rejects asset with no duration`() {
        val bad = audioAsset.copy(durationMs = 0L)
        val result = useCase(project, bad)
        assertTrue(result is AppResult.Error)
    }
}
