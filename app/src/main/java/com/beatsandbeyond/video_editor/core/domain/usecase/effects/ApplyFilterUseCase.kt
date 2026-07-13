package com.beatsandbeyond.video_editor.core.domain.usecase.effects

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.domain.model.mapClip
import javax.inject.Inject

/**
 * Applies a [VideoFilter] to a clip. The filter is stored non-destructively on the clip
 * and applied at render/preview time (GLSL pipeline lands in Phase 7).
 */
class ApplyFilterUseCase @Inject constructor() {
    operator fun invoke(project: Project, clipId: String, filter: VideoFilter): AppResult<Project> {
        val updatedTimeline = project.timeline.mapClip(clipId) { clip ->
            clip.copy(filter = filter)
        } ?: return AppResult.Error(NoSuchElementException("Clip $clipId not found"))

        return AppResult.Success(
            project.copy(timeline = updatedTimeline, updatedAt = System.currentTimeMillis())
        )
    }
}
