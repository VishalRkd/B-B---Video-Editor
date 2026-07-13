package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Use case to remove a transition from the timeline by ID.
 */
class RemoveTransitionUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(project: Project, transitionId: String): AppResult<Project> {
        val updatedTimeline = timelineEngine.removeTransition(project.timeline, transitionId)
            ?: return AppResult.Error(NoSuchElementException("Transition $transitionId not found"))
        return AppResult.Success(
            project.copy(
                timeline = updatedTimeline,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
