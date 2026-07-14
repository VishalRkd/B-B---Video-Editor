package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a visual effect clip to the project's EFFECT track using the [TimelineEngine].
 */
class AddEffectClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        timelinePositionMs: Long,
        durationMs: Long = DEFAULT_EFFECT_DURATION_MS
    ): AppResult<Project> {
        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = "effect_${UUID.randomUUID()}",
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = durationMs,
        )

        val updatedTimeline = timelineEngine.addClipWithShift(
            project.timeline,
            TrackType.EFFECT,
            clip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_EFFECT_DURATION_MS = 3000L
    }
}
