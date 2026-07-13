package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Trims a clip and ripples (shifts) all later clips on the same track by the
 * resulting duration delta, so no gap is left behind.
 */
class RippleTrimUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTrimStartMs: Long,
        newTrimEndMs: Long,
    ): AppResult<Project> {
        if (newTrimStartMs < 0) {
            return AppResult.Error(IllegalArgumentException("Trim start must be >= 0"))
        }
        if (newTrimEndMs <= newTrimStartMs) {
            return AppResult.Error(IllegalArgumentException("Trim end must be > trim start"))
        }

        val track = project.timeline.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } }
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))
        val clip = track.clips.first { it.id == clipId }

        val trimmed = timelineEngine.trimClip(clip, newTrimStartMs, newTrimEndMs)
        val oldDuration = clip.durationOnTimelineMs
        val newDuration = trimmed.durationOnTimelineMs
        val delta = newDuration - oldDuration

        if (delta == 0L) {
            return AppResult.Success(project)
        }

        // Shift every clip that starts strictly AFTER the trimmed clip, so later
        // clips stay attached to the trimmed clip's (possibly moved) right edge.
        // The trimmed clip itself is never re-shifted here.
        val updatedTracks = project.timeline.tracks.map { t ->
            if (t.id != track.id) return@map t
            val updatedClips = t.clips.map { c ->
                if (c.id == clipId) {
                    trimmed
                } else if (c.timelinePositionMs > clip.timelinePositionMs) {
                    c.copy(timelinePositionMs = (c.timelinePositionMs + delta).coerceAtLeast(0L))
                } else c
            }.sortedBy { it.timelinePositionMs }
            t.copy(clips = updatedClips)
        }
        return AppResult.Success(project.copy(
            timeline = project.timeline.copy(tracks = updatedTracks),
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
