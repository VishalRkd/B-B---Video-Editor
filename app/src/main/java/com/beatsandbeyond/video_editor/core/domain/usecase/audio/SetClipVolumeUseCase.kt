package com.beatsandbeyond.video_editor.core.domain.usecase.audio

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.mapClip
import javax.inject.Inject

/**
 * Sets the volume multiplier on a clip.
 *
 * @param volume 0.0 (muted) to 2.0 (200% boost). Values outside this range are clamped.
 */
class SetClipVolumeUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String, volume: Float): AppResult<Project> {
        val clamped = volume.coerceIn(0.0f, 2.0f)
        val updatedTimeline = project.timeline.mapClip(clipId) { clip ->
            clip.copy(volume = clamped)
        } ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
