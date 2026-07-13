package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TextStyle
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.insertAndResolveOverlaps
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a text overlay clip to the project's TEXT track.
 *
 * Text clips have no backing [Asset] — they are rendered from a [TextStyle]. The clip
 * is placed at [timelinePositionMs] (default 0) with the given [durationMs]. If no TEXT
 * track exists, one is created.
 */
class AddTextClipUseCase @Inject constructor() {
    operator fun invoke(
        project: Project,
        textStyle: TextStyle,
        timelinePositionMs: Long = 0L,
        durationMs: Long = DEFAULT_TEXT_DURATION_MS,
    ): AppResult<Project> {
        if (textStyle.text.isBlank()) {
            return AppResult.Error(IllegalArgumentException("Text cannot be blank"))
        }
        if (durationMs <= 0L) {
            return AppResult.Error(IllegalArgumentException("Duration must be positive"))
        }

        val clip = Clip(
            id = UUID.randomUUID().toString(),
            assetId = "text_${UUID.randomUUID()}",
            timelinePositionMs = timelinePositionMs.coerceAtLeast(0L),
            trimStartMs = 0L,
            trimEndMs = durationMs,
            textStyle = textStyle,
        )

        val timeline = project.timeline
        val textTrack = timeline.tracks.firstOrNull { it.type == TrackType.TEXT }

        val updatedTimeline = if (textTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = TrackType.TEXT,
                    clips = listOf(clip),
                )
            )
        } else {
            val updatedTrack = textTrack.insertAndResolveOverlaps(clip)
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == textTrack.id) updatedTrack else track
                }
            )
        }

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_TEXT_DURATION_MS = 3000L
    }
}
