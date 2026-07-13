package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
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
        val updatedTimeline = timelineEngine.moveClip(project.timeline, clipId, newTimelinePositionMs)
        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
