package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import javax.inject.Inject

/**
 * Applies a [VideoFilter] to a clip using the [TimelineEngine].
 */
class ApplyFilterUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(project: Project, clipId: String, filter: VideoFilter): AppResult<Project> {
        val updatedTimeline = timelineEngine.setClipFilter(project.timeline, clipId, filter)
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
