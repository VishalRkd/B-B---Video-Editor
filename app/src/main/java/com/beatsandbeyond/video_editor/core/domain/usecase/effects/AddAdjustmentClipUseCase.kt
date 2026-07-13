package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds an adjustment clip to the project's ADJUSTMENT track.
 */
class AddAdjustmentClipUseCase @Inject constructor() {
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

        val timeline = project.timeline
        val adjustmentTrack = timeline.tracks.firstOrNull { it.type == TrackType.ADJUSTMENT }

        val updatedTimeline = if (adjustmentTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.ADJUSTMENT,
                    clips = listOf(clip),
                )
            )
        } else {
            val newClips = adjustmentTrack.clips.map { c ->
                if (c.timelinePositionMs >= timelinePositionMs) {
                    c.copy(timelinePositionMs = c.timelinePositionMs + durationMs)
                } else c
            } + clip
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == adjustmentTrack.id) {
                        track.copy(clips = newClips.sortedBy { it.timelinePositionMs })
                    } else track
                }
            )
        }

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_ADJUSTMENT_DURATION_MS = 3000L
    }
}
