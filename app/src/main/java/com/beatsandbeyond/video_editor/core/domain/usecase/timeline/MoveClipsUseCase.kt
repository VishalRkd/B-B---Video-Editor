package com.beatsandbeyond.video_editor.core.domain.usecase.timeline

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Project
import javax.inject.Inject

/**
 * Moves a group of clips by the same delta, preserving relative spacing.
 * Used for multi-select group drag.
 */
class MoveClipsUseCase @Inject constructor(
    private val timelineEngine: TimelineEngine,
) {
    operator fun invoke(
        project: Project,
        clipIds: Set<String>,
        deltaMs: Long,
    ): AppResult<Project> {
        if (clipIds.isEmpty()) {
            return AppResult.Error(IllegalArgumentException("No clips selected"))
        }
        if (deltaMs == 0L) {
            return AppResult.Success(project)
        }

        // Reject the move if any moved clip would overlap a STATIONARY clip on the
        // same track. (Clips within the moving set all shift by the same delta, so
        // their relative spacing is preserved and they cannot newly overlap each other.)
        val overlaps = project.timeline.tracks.any { track ->
            val selected = track.clips.filter { it.id in clipIds }
            val stationary = track.clips.filter { it.id !in clipIds }
            selected.any { movedClip ->
                val newStart = (movedClip.timelinePositionMs + deltaMs).coerceAtLeast(0L)
                val newEnd = newStart + movedClip.durationOnTimelineMs
                stationary.any { other ->
                    newStart < other.timelineEndMs && newEnd > other.timelinePositionMs
                }
            }
        }
        if (overlaps) {
            return AppResult.Error(IllegalStateException("Clips cannot overlap on the same track"))
        }

        val updatedTimeline = timelineEngine.moveClips(project.timeline, clipIds, deltaMs)
            ?: return AppResult.Error(IllegalStateException("Clips cannot overlap on the same track"))
        return AppResult.Success(project.copy(
            timeline = updatedTimeline,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
