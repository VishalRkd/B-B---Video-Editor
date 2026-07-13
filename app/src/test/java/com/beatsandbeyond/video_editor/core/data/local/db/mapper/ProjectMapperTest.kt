package com.beatsandbeyond.video_editor.core.data.local.db.mapper

import com.beatsandbeyond.video_editor.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class ProjectMapperTest {

    @Test
    fun testSerializeAndDeserializeTimelineWithVideoFilter() {
        // Arrange
        val projectId = UUID.randomUUID().toString()
        val timelineId = UUID.randomUUID().toString()
        val primaryTrackId = UUID.randomUUID().toString()
        
        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = UUID.randomUUID().toString(),
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 5000L,
            filter = VideoFilter.Brightness(0.5f)
        )
        
        val clip2 = Clip(
            id = UUID.randomUUID().toString(),
            assetId = UUID.randomUUID().toString(),
            timelinePositionMs = 5000L,
            trimStartMs = 0L,
            trimEndMs = 2000L,
            filter = VideoFilter.None
        )

        val track = Track(
            id = primaryTrackId,
            type = TrackType.VIDEO,
            clips = listOf(clip, clip2)
        )

        val originalProject = Project(
            id = projectId,
            name = "Test Project",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            timeline = Timeline(timelineId, listOf(track)),
            resolution = Resolution.FHD,
            frameRate = 30f
        )

        // Act
        val entity = ProjectMapper.toEntity(originalProject)
        val deserializedProject = ProjectMapper.toDomain(entity)

        // Assert
        val deserializedTimeline = deserializedProject.timeline
        assertNotNull(deserializedTimeline)
        assertEquals(1, deserializedTimeline.tracks.size)
        
        val deserializedTrack = deserializedTimeline.tracks.first()
        assertEquals(2, deserializedTrack.clips.size)
        
        val deserializedClip1 = deserializedTrack.clips[0]
        assertEquals(VideoFilter.Brightness(0.5f), deserializedClip1.filter)
        
        val deserializedClip2 = deserializedTrack.clips[1]
        assertEquals(VideoFilter.None, deserializedClip2.filter)
    }
}
