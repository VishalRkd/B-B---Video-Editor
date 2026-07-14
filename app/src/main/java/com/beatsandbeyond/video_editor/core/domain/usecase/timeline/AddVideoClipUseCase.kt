package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to add a video/image asset onto the project's primary VIDEO track using the [TimelineEngine].
 */
class AddVideoClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        asset: Asset,
        timelinePositionMs: Long
    ): AppResult<Project> {
        val clipDuration = if (asset.mediaType == MediaType.IMAGE) {
            DEFAULT_IMAGE_DURATION_MS
        } else {
            asset.durationMs
        }

        if (clipDuration <= 0L) {
            return AppResult.Error(IllegalArgumentException("Clip duration must be positive"))
        }

        val newClip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = asset.id,
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = clipDuration,
        )

        val updatedTimeline = timelineEngine.addClipWithOverlapResolution(
            project.timeline,
            TrackType.VIDEO,
            newClip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_IMAGE_DURATION_MS = 3000L
    }
}
