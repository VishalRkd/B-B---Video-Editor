package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TextStyle
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddTextClipUseCase].
 */
class AddTextClipUseCaseTest {

    private lateinit var useCase: AddTextClipUseCase
    private lateinit var project: Project

    @Before
    fun setUp() {
        useCase = AddTextClipUseCase()
        project = Project(
            id = "project_1", name = "Test", createdAt = 0L, updatedAt = 0L,
            timeline = Timeline(id = "timeline_1", tracks = emptyList()),
        )
    }

    @Test
    fun `adds text track with clip`() {
        val result = useCase(project, TextStyle(text = "Hello"))
        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        val textTrack = updated.timeline.tracks.first { it.type == TrackType.TEXT }
        assertEquals(1, textTrack.clips.size)
        assertEquals("Hello", textTrack.clips.first().textStyle?.text)
    }

    @Test
    fun `rejects blank text`() {
        val result = useCase(project, TextStyle(text = "   "))
        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `uses provided duration`() {
        val result = useCase(project, TextStyle(text = "Hi"), durationMs = 5000L)
        assertTrue(result is AppResult.Success)
        val clip = (result as AppResult.Success).data.timeline.tracks
            .first { it.type == TrackType.TEXT }.clips.first()
        assertEquals(5000L, clip.trimEndMs)
    }
}
