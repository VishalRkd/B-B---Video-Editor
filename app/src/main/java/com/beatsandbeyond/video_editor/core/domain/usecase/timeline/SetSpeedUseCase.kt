package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Sets the speed multiplier on a clip using the [TimelineEngine].
 */
class SetSpeedUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(project: Project, clipId: String, speedFactor: Float): AppResult<Project> {
        if (speedFactor < 0.25f || speedFactor > 4.0f) {
            return AppResult.Error(IllegalArgumentException("Speed must be 0.25x to 4.0x"))
        }

        val updatedTimeline = timelineEngine.setClipSpeed(project.timeline, clipId, speedFactor)
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
