package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddOverlayClipUseCase].
 */
class AddOverlayClipUseCaseTest {

    private lateinit var useCase: AddOverlayClipUseCase
    private lateinit var project: Project
    private lateinit var imageAsset: Asset

    @Before
    fun setUp() {
        useCase = AddOverlayClipUseCase(com.beatsandbeyond.video_editor.core.data.engine.KotlinTimelineEngine())
        imageAsset = Asset(
            id = "img_1", mediaUri = "content://image/1", mediaType = MediaType.IMAGE,
            displayName = "sticker.png", durationMs = 5000L, width = 512, height = 512,
            sizeBytes = 2048L, importedAt = 0L,
        )
        project = Project(
            id = "project_1", name = "Test", createdAt = 0L, updatedAt = 0L,
            timeline = Timeline(id = "timeline_1", tracks = emptyList()),
        )
    }

    @Test
    fun `adds overlay track with clip`() {
        val result = useCase(project, imageAsset)
        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        val overlayTrack = updated.timeline.tracks.first { it.type == TrackType.OVERLAY }
        assertEquals(1, overlayTrack.clips.size)
        assertEquals("img_1", overlayTrack.clips.first().assetId)
    }

    @Test
    fun `uses asset duration when not specified`() {
        val video = imageAsset.copy(mediaType = MediaType.VIDEO, durationMs = 8000L)
        val result = useCase(project, video)
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks
            .first { it.type == TrackType.OVERLAY }.clips.first()
        assertEquals(8000L, clip.trimEndMs)
    }

    @Test
    fun `rejects non positive duration`() {
        val result = useCase(project, imageAsset, durationMs = 0L)
        assertTrue(result is AppResult.Error)
    }
}
