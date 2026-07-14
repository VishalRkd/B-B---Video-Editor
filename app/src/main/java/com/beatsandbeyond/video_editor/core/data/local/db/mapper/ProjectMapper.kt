package com.beatsandbeyond.video_editor.core.data.local.db.mapper

import com.beatsandbeyond.video_editor.core.data.local.db.entity.ProjectEntity
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.utils.Logger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type
import java.util.UUID

/**
 * Converts between [ProjectEntity] (Room) and [Project] (domain).
 *
 * Timeline serialization uses Gson. If JSON parsing fails (e.g., corrupted data),
 * an empty Timeline is returned to prevent crashes and allow graceful degradation.
 */
object ProjectMapper {

    private val gson = GsonBuilder()
        .registerTypeAdapter(VideoFilter::class.java, VideoFilterAdapter())
        .create()

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
            val raw = gson.fromJson(json, Timeline::class.java)
            sanitizeTimeline(raw ?: Timeline.empty(
                timelineId = "$projectId-timeline",
                primaryTrackId = "$projectId-track-0",
            ))
        } catch (e: Exception) {
            Logger.e("ProjectMapper", "Failed to deserialize timeline for project $projectId", e)
            Timeline.empty(
                timelineId = "$projectId-timeline",
                primaryTrackId = "$projectId-track-0",
            )
        }
    }

    /**
     * Replaces any null [Clip.filter] (or other nullable-by-Gson fields) with their
     * domain defaults. This guards against timelines persisted before newer fields
     * were introduced.
     */
    private fun sanitizeTimeline(timeline: Timeline?): Timeline {
        if (timeline == null) {
            return Timeline.empty(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        }
        val tracks = timeline.tracks ?: emptyList()
        val transitions = timeline.transitions ?: emptyList()
        
        val sanitizedTracks = tracks.map { track: Track ->
            val clips = track.clips ?: emptyList()
            val sanitizedClips = clips.map { clip: Clip ->
                var c = if (clip.filter == null) clip.copy(filter = VideoFilter.None) else clip
                if (c.animatableProperties == null) {
                    c = c.copy(animatableProperties = emptyList())
                }
                c
            }
            track.copy(clips = sanitizedClips)
        }
        return timeline.copy(tracks = sanitizedTracks, transitions = transitions)
    }
}

class VideoFilterAdapter : JsonSerializer<VideoFilter>, JsonDeserializer<VideoFilter> {
    override fun serialize(src: VideoFilter, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", src::class.java.simpleName)
        when (src) {
            is VideoFilter.None -> {}
            is VideoFilter.Brightness -> jsonObject.addProperty("value", src.value)
            is VideoFilter.Contrast -> jsonObject.addProperty("value", src.value)
            is VideoFilter.Saturation -> jsonObject.addProperty("value", src.value)
            is VideoFilter.Vintage -> jsonObject.addProperty("intensity", src.intensity)
            is VideoFilter.Vignette -> jsonObject.addProperty("radius", src.radius)
        }
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): VideoFilter {
        return try {
            val jsonObject = json.asJsonObject
            val type = jsonObject.get("type")?.asString ?: "None"
            when (type) {
                "Brightness" -> VideoFilter.Brightness(jsonObject.get("value")?.asFloat ?: 0f)
                "Contrast" -> VideoFilter.Contrast(jsonObject.get("value")?.asFloat ?: 1f)
                "Saturation" -> VideoFilter.Saturation(jsonObject.get("value")?.asFloat ?: 1f)
                "Vintage" -> VideoFilter.Vintage(jsonObject.get("intensity")?.asFloat ?: 0f)
                "Vignette" -> VideoFilter.Vignette(jsonObject.get("radius")?.asFloat ?: 0f)
                else -> VideoFilter.None
            }
        } catch (e: Exception) {
            Logger.e("VideoFilterAdapter", "Failed to deserialize filter JSON", e)
            VideoFilter.None
        }
    }
}
