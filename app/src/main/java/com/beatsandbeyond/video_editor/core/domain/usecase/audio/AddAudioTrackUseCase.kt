package com.beatsandbeyond.video_editor.core.domain.usecase.audio

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
 * Adds a new audio clip sourced from [asset] onto the project's AUDIO track.
 *
 * If the project has no AUDIO track yet, one is created. The clip is appended at the
 * end of the audio track (after the last existing audio clip) so multiple music
 * layers stack sequentially rather than overlapping.
 */
class AddAudioTrackUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        asset: Asset,
        timelinePositionMs: Long = 0L
    ): AppResult<Project> {
        if (asset.durationMs <= 0L) {
            return AppResult.Error(IllegalArgumentException("Asset has no duration"))
        }

        val timeline = project.timeline
        val audioTrack = timeline.tracks.firstOrNull { it.type == TrackType.AUDIO }

        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = asset.id,
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = asset.durationMs,
        )

        val updatedTimeline = if (audioTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.AUDIO,
                    clips = listOf(clip),
                )
            )
        } else {
            val newClipsList = mutableListOf<Clip>()
            for (c in audioTrack.clips) {
                val clipStart = c.timelinePositionMs
                val clipEnd = c.timelinePositionMs + c.durationOnTimelineMs

                if (clipStart >= timelinePositionMs) {
                    newClipsList.add(
                        c.copy(timelinePositionMs = clipStart + asset.durationMs)
                    )
                } else if (clipStart < timelinePositionMs && clipEnd > timelinePositionMs) {
                    val leftDuration = timelinePositionMs - clipStart
                    val speed = c.speedFactor

                    newClipsList.add(
                        c.copy(
                            trimEndMs = c.trimStartMs + (leftDuration * speed).toLong()
                        )
                    )
                    newClipsList.add(
                        c.copy(
                            id = UUID.randomUUID().toString(),
                            timelinePositionMs = timelinePositionMs + asset.durationMs,
                            trimStartMs = c.trimStartMs + (leftDuration * speed).toLong(),
                            trimEndMs = c.trimEndMs
                        )
                    )
                } else {
                    newClipsList.add(c)
                }
            }
            newClipsList.add(clip)
            newClipsList.sortBy { it.timelinePositionMs }
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == audioTrack.id) {
                        track.copy(clips = newClipsList)
                    } else track
                }
            )
        }

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
