package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TimelineValidator
import javax.inject.Inject

/**
 * Moves a group of clips by the same delta, preserving relative spacing.
 * Used for multi-select group drag.
 */
class MoveClipsUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipIds: Set<String>,
        deltaMs: Long,
    ): AppResult<Project> {
        if (clipIds.isEmpty()) {
            return AppResult.Error(IllegalArgumentException("No clips selected"))
        }
        if (deltaMs == 0L) {
            return AppResult.Success(project)
        }

        val updatedTimeline = timelineEngine.moveClips(project.timeline, clipIds, deltaMs)
            ?: return AppResult.Error(IllegalStateException("Clips cannot overlap on the same track"))
        val updatedProject = project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        )
        return TimelineValidator.validateAndReturn(updatedProject)
    }
}
