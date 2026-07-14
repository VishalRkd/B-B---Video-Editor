package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TimelineValidator
import javax.inject.Inject

/**
 * Reorders/moves a clip to a new position on the timeline track using [TimelineEngine].
 */
class MoveClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTimelinePositionMs: Long,
    ): AppResult<Project> {
        if (newTimelinePositionMs < 0) {
            return AppResult.Error(IllegalArgumentException("Timeline position must be >= 0"))
        }

        val track = project.timeline.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } }
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))
        val clip = track.clips.first { it.id == clipId }
        val updatedTimeline = timelineEngine.moveClip(project.timeline, clipId, newTimelinePositionMs)
            ?: return AppResult.Error(IllegalStateException("Clips cannot overlap on the same track"))
        val updatedProject = project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        )
        return TimelineValidator.validateAndReturn(updatedProject)
    }
}
