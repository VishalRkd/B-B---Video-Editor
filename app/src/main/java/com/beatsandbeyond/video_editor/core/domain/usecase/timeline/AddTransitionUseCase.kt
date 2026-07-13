package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Transition
import javax.inject.Inject

/**
 * Use case to add a transition between adjacent track clips.
 */
class AddTransitionUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(project: Project, transition: Transition): AppResult<Project> {
        val updatedTimeline = timelineEngine.addTransition(project.timeline, transition)
            ?: return AppResult.Error(IllegalArgumentException("Transition validation failed: check clip adjacency and duration"))
        return AppResult.Success(
            project.copy(
                timeline = updatedTimeline,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
