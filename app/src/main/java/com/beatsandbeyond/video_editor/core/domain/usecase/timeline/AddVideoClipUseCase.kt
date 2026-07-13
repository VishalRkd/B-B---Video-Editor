package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to add a video/image asset onto the project's primary VIDEO track.
 *
 * Implements advanced insertion logic:
 * - Inserts the clip exactly at [timelinePositionMs].
 * - If the insert point falls inside an existing clip, that clip is split into two,
 *   and the right part is pushed to the end of the new clip.
 * - All subsequent clips starting after [timelinePositionMs] are shifted right
 *   by the new clip's duration.
 */
class AddVideoClipUseCase @Inject constructor() {
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

        val timeline = project.timeline
        val videoTrack = timeline.tracks.firstOrNull { it.type == TrackType.VIDEO }
            ?: return AppResult.Error(IllegalStateException("No VIDEO track found in timeline"))

        val newClipsList = mutableListOf<Clip>()
        
        for (clip in videoTrack.clips) {
            val clipStart = clip.timelinePositionMs
            val clipEnd = clip.timelinePositionMs + clip.durationOnTimelineMs

            if (clipStart >= timelinePositionMs) {
                // Clip starts after insertion point: shift it right
                newClipsList.add(
                    clip.copy(timelinePositionMs = clipStart + clipDuration)
                )
            } else if (clipStart < timelinePositionMs && clipEnd > timelinePositionMs) {
                // Clip overlaps insertion point: split it
                val leftDuration = timelinePositionMs - clipStart
                val rightDuration = clipEnd - timelinePositionMs
                val speed = clip.speedFactor

                // Create left split segment
                newClipsList.add(
                    clip.copy(
                        trimEndMs = clip.trimStartMs + (leftDuration * speed).toLong()
                    )
                )
                // Create right split segment, pushed after the new clip
                newClipsList.add(
                    clip.copy(
                        id = UUID.randomUUID().toString(),
                        timelinePositionMs = timelinePositionMs + clipDuration,
                        trimStartMs = clip.trimStartMs + (leftDuration * speed).toLong(),
                        trimEndMs = clip.trimEndMs
                    )
                )
            } else {
                // Clip ends before insertion point: keep unchanged
                newClipsList.add(clip)
            }
        }

        // Add the newly imported clip
        newClipsList.add(newClip)
        newClipsList.sortBy { it.timelinePositionMs }

        val updatedTimeline = timeline.copy(
            tracks = timeline.tracks.map { track ->
                if (track.id == videoTrack.id) {
                    track.copy(clips = newClipsList)
                } else track
            }
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_IMAGE_DURATION_MS = 3000L
    }
}
