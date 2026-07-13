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
 * Adds a visual effect clip to the project's EFFECT track.
 */
class AddEffectClipUseCase @Inject constructor() {
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

        val timeline = project.timeline
        val effectTrack = timeline.tracks.firstOrNull { it.type == TrackType.EFFECT }

        val updatedTimeline = if (effectTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.EFFECT,
                    clips = listOf(clip),
                )
            )
        } else {
            val newClips = effectTrack.clips.map { c ->
                if (c.timelinePositionMs >= timelinePositionMs) {
                    c.copy(timelinePositionMs = c.timelinePositionMs + durationMs)
                } else c
            } + clip
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == effectTrack.id) {
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
        const val DEFAULT_EFFECT_DURATION_MS = 3000L
    }
}
