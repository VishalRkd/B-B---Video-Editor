package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Removes a clip from the timeline using the [TimelineEngine].
 */
class DeleteClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(project: Project, clipId: String): AppResult<Project> {
        val updatedTimeline = timelineEngine.removeClip(project.timeline, clipId)
        val oldClipCount = project.timeline.tracks.flatMap { it.clips }.size
        val newClipCount = updatedTimeline.tracks.flatMap { it.clips }.size
        if (oldClipCount == newClipCount) {
            return AppResult.Error(NoSuchElementException("Clip $clipId not found"))
        }
        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
