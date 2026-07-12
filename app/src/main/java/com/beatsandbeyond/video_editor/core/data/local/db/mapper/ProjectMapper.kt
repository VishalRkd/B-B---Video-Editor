package com.beatsandbeyond.video_editor.core.data.local.db.mapper

import com.beatsandbeyond.video_editor.core.data.local.db.entity.ProjectEntity
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Converts between [ProjectEntity] (Room) and [Project] (domain).
 *
 * Timeline serialization uses Gson. If JSON parsing fails (e.g., corrupted data),
 * an empty Timeline is returned to prevent crashes and allow graceful degradation.
 */
object ProjectMapper {

    private val gson = Gson()

    fun toDomain(entity: ProjectEntity): Project = Project(
        id = entity.id,
        name = entity.name,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        resolution = Resolution(
            width = entity.resolutionWidth,
            height = entity.resolutionHeight,
        ),
        frameRate = entity.frameRate,
        thumbnailUri = entity.thumbnailUri,
        timeline = deserializeTimeline(entity.id, entity.timelineJson),
    )

    fun toEntity(domain: Project): ProjectEntity = ProjectEntity(
        id = domain.id,
        name = domain.name,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
        resolutionWidth = domain.resolution.width,
        resolutionHeight = domain.resolution.height,
        frameRate = domain.frameRate,
        thumbnailUri = domain.thumbnailUri,
        timelineJson = serializeTimeline(domain.timeline),
    )

    private fun serializeTimeline(timeline: Timeline): String =
        gson.toJson(timeline)

    private fun deserializeTimeline(projectId: String, json: String): Timeline {
        return try {
            gson.fromJson(json, Timeline::class.java) ?: Timeline.empty(
                timelineId = "$projectId-timeline",
                primaryTrackId = "$projectId-track-0",
            )
        } catch (e: JsonSyntaxException) {
            // Graceful degradation: return an empty timeline rather than crashing.
            Timeline.empty(
                timelineId = "$projectId-timeline",
                primaryTrackId = "$projectId-track-0",
            )
        }
    }
}
