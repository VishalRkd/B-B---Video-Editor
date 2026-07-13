package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.findClip
import com.beatsandbeyond.video_editor.core.domain.model.replaceClip
import javax.inject.Inject

/**
 * Splits a clip at a timeline-absolute position by delegating to the [TimelineEngine].
 */
class SplitClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipId: String,
        splitAtTimelineMs: Long,
    ): AppResult<Project> {
        val clip = project.timeline.findClip(clipId)
            ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        // splitAtTimelineMs must be within the clip bounds
        if (splitAtTimelineMs <= clip.timelinePositionMs ||
            splitAtTimelineMs >= clip.timelineEndMs
        ) {
            return AppResult.Error(IllegalArgumentException("Split position must be within clip bounds"))
        }

        val relativePositionMs = splitAtTimelineMs - clip.timelinePositionMs
        val (leftClip, rightClip) = try {
            timelineEngine.splitClip(clip, relativePositionMs)
        } catch (e: Exception) {
            return AppResult.Error(e)
        }

        val updatedTimeline = project.timeline.replaceClip(clipId, listOf(leftClip, rightClip))
            ?: return AppResult.Error(IllegalStateException("Failed to split clip"))

        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
