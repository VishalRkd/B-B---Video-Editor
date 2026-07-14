package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds an image/video overlay clip sourced from [asset] onto the project's OVERLAY track using the [TimelineEngine].
 */
class AddOverlayClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        asset: Asset,
        timelinePositionMs: Long = 0L,
        durationMs: Long? = null,
    ): AppResult<Project> {
        val clipDuration = durationMs ?: asset.durationMs
        if (clipDuration <= 0L) {
            return AppResult.Error(IllegalArgumentException("Overlay duration must be positive"))
        }

        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = asset.id,
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = clipDuration,
        )

        val updatedTimeline = timelineEngine.addClipWithOverlapResolution(
            project.timeline,
            TrackType.OVERLAY,
            clip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
