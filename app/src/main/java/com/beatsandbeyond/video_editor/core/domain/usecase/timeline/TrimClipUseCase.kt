package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.mapClip
import javax.inject.Inject

/**
 * Trims a clip by adjusting its trim start and end points via the [TimelineEngine].
 */
class TrimClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipId: String,
        newTrimStartMs: Long,
        newTrimEndMs: Long,
    ): AppResult<Project> {
        if (newTrimStartMs < 0) return AppResult.Error(
            IllegalArgumentException("Trim start must be >= 0")
        )
        if (newTrimEndMs <= newTrimStartMs) return AppResult.Error(
            IllegalArgumentException("Trim end must be > trim start")
        )

        val updatedTimeline = project.timeline.mapClip(clipId) { clip ->
            timelineEngine.trimClip(clip, newTrimStartMs, newTrimEndMs)
        } ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        val updatedProject = project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        )
        return com.beatsandbeyond.video_editor.core.domain.model.TimelineValidator.validateAndReturn(updatedProject)
    }
}
