package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds an adjustment clip to the project's ADJUSTMENT track using the [TimelineEngine].
 */
class AddAdjustmentClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        timelinePositionMs: Long,
        durationMs: Long = DEFAULT_ADJUSTMENT_DURATION_MS
    ): AppResult<Project> {
        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = "adjustment_${UUID.randomUUID()}",
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = durationMs,
        )

        val updatedTimeline = timelineEngine.addClipWithShift(
            project.timeline,
            TrackType.ADJUSTMENT,
            clip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_ADJUSTMENT_DURATION_MS = 3000L
    }
}
