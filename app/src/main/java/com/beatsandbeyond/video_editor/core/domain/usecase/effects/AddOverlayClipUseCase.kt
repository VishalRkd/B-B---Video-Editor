package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds an image/video overlay clip sourced from [asset] onto the project's OVERLAY track.
 *
 * Overlays are placed at [timelinePositionMs] (default 0). If no OVERLAY track exists,
 * one is created. Images use their full duration; videos use the selected trim window.
 */
class AddOverlayClipUseCase @Inject constructor() {
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

        val timeline = project.timeline
        val overlayTrack = timeline.tracks.firstOrNull { it.type == TrackType.OVERLAY }

        val updatedTimeline = if (overlayTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.OVERLAY,
                    clips = listOf(clip),
                )
            )
        } else {
            val newClips = overlayTrack.clips.map { c ->
                if (c.timelinePositionMs >= timelinePositionMs.coerceAtLeast(0L)) {
                    c.copy(timelinePositionMs = c.timelinePositionMs + clipDuration)
                } else c
            } + clip
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == overlayTrack.id) {
                        track.copy(clips = newClips.sortedBy { it.timelinePositionMs })
                    } else track
                }
            )
        }

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
