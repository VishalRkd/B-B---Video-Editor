package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents a validation error on the [Timeline].
 */
sealed class TimelineValidationError {
    data class DuplicateClipId(val clipId: String) : TimelineValidationError()
    data class InvalidTrimPoints(val clipId: String, val startMs: Long, val endMs: Long) : TimelineValidationError()
    data class TrimExceedsAssetDuration(val clipId: String, val trimEndMs: Long, val assetDurationMs: Long) : TimelineValidationError()
    data class InvalidTimelinePosition(val clipId: String, val positionMs: Long) : TimelineValidationError()
    data class InvalidSpeedFactor(val clipId: String, val speedFactor: Float) : TimelineValidationError()
    data class InvalidVolume(val clipId: String, val volume: Float) : TimelineValidationError()
    data class ClipsOverlap(val trackId: String, val clipId1: String, val clipId2: String) : TimelineValidationError()
    data class TrackClipsUnsorted(val trackId: String) : TimelineValidationError()
}

/**
 * Pure domain validator to check structural invariants on a [Timeline].
 */
object TimelineValidator {

    /**
     * Validates structural invariants of the given [timeline].
     *
     * @param timeline the timeline to validate.
     * @param getAssetDurationMs lambda that returns the duration of the asset backing a clip, if known.
     */
    fun validate(
        timeline: Timeline,
        getAssetDurationMs: (String) -> Long? = { null }
    ): List<TimelineValidationError> {
        val errors = mutableListOf<TimelineValidationError>()
        val clipIds = mutableSetOf<String>()

        for (track in timeline.tracks) {
            var prevClip: Clip? = null
            var lastPosition = -1L

            for (clip in track.clips) {
                // 1. Clip ID uniqueness
                if (!clipIds.add(clip.id)) {
                    errors.add(TimelineValidationError.DuplicateClipId(clip.id))
                }

                // 2. trimStartMs >= 0 and trimEndMs > trimStartMs
                if (clip.trimStartMs < 0 || clip.trimEndMs <= clip.trimStartMs) {
                    errors.add(TimelineValidationError.InvalidTrimPoints(clip.id, clip.trimStartMs, clip.trimEndMs))
                }

                // 3. trimEndMs <= asset duration (if asset is found)
                val assetDuration = getAssetDurationMs(clip.assetId)
                if (assetDuration != null && clip.trimEndMs > assetDuration) {
                    errors.add(TimelineValidationError.TrimExceedsAssetDuration(clip.id, clip.trimEndMs, assetDuration))
                }

                // 4. timelinePositionMs >= 0
                if (clip.timelinePositionMs < 0) {
                    errors.add(TimelineValidationError.InvalidTimelinePosition(clip.id, clip.timelinePositionMs))
                }

                // 5. speedFactor within [0.25f..4.0f]
                if (clip.speedFactor < 0.25f || clip.speedFactor > 4.0f) {
                    errors.add(TimelineValidationError.InvalidSpeedFactor(clip.id, clip.speedFactor))
                }

                // 6. volume within [0.0f..2.0f]
                if (clip.volume < 0.0f || clip.volume > 2.0f) {
                    errors.add(TimelineValidationError.InvalidVolume(clip.id, clip.volume))
                }

                // 7. Track ordering and overlap checks
                if (clip.timelinePositionMs < lastPosition) {
                    errors.add(TimelineValidationError.TrackClipsUnsorted(track.id))
                }
                lastPosition = clip.timelinePositionMs

                if (prevClip != null) {
                    val prevEnd = prevClip.timelineEndMs
                    val currentStart = clip.timelinePositionMs
                    if (currentStart < prevEnd) {
                        errors.add(TimelineValidationError.ClipsOverlap(track.id, prevClip.id, clip.id))
                    }
                }
                prevClip = clip
            }
        }

        return errors
    }

    /**
     * Validates a mutated [Project] timeline and returns it wrapped in [com.beatsandbeyond.video_editor.core.common.AppResult.Success].
     * If validation fails, throws an exception in debug builds, or returns [com.beatsandbeyond.video_editor.core.common.AppResult.Error] in release.
     */
    fun validateAndReturn(project: Project): com.beatsandbeyond.video_editor.core.common.AppResult<Project> {
        val errors = validate(project.timeline)
        if (errors.isNotEmpty()) {
            val exception = IllegalStateException("Timeline invariant violated: $errors")
            if (DomainDebugConfig.isDebug) {
                throw exception
            } else {
                return com.beatsandbeyond.video_editor.core.common.AppResult.Error(exception)
            }
        }
        return com.beatsandbeyond.video_editor.core.common.AppResult.Success(project)
    }
}
