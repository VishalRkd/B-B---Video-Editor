package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.findClip
import com.beatsandbeyond.video_editor.core.domain.model.replaceClip
import com.beatsandbeyond.video_editor.core.domain.model.TimelineValidator
import javax.inject.Inject

/**
 * Result of a split clip operation, containing the updated project and split clip IDs.
 */
data class SplitClipResult(
    val project: Project,
    val leftClipId: String,
    val rightClipId: String
)

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
    ): AppResult<SplitClipResult> {
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

        val updatedProject = project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        )

        // Validate timeline invariants
        return when (val validationResult = TimelineValidator.validateAndReturn(updatedProject)) {
            is AppResult.Success -> AppResult.Success(
                SplitClipResult(
                    project = validationResult.data,
                    leftClipId = leftClip.id,
                    rightClipId = rightClip.id
                )
            )
            is AppResult.Error -> AppResult.Error(validationResult.exception)
            is AppResult.Loading -> AppResult.Error(IllegalStateException("Unexpected loading state"))
        }
    }
}
