package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.TextStyle
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import java.util.UUID
import javax.inject.Inject

/**
 * Adds a text overlay clip to the project's TEXT track using the [TimelineEngine].
 */
class AddTextClipUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
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

        val updatedTimeline = timelineEngine.addClipWithOverlapResolution(
            project.timeline,
            TrackType.TEXT,
            clip
        )

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }

    companion object {
        const val DEFAULT_TEXT_DURATION_MS = 3000L
    }
}
