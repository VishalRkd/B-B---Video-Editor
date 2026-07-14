package com.beatsandbeyond.video_editor.core.domain.usecase.audio

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a new audio clip sourced from [asset] onto the project's AUDIO track using the [TimelineEngine].
 */
class AddAudioTrackUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        asset: Asset,
        timelinePositionMs: Long = 0L
    ): AppResult<Project> {
        if (asset.durationMs <= 0L) {
            return AppResult.Error(IllegalArgumentException("Asset has no duration"))
        }

        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = asset.id,
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = asset.durationMs,
        )

        val updatedTimeline = timelineEngine.addClipWithOverlapResolution(
            project.timeline,
            TrackType.AUDIO,
            clip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
