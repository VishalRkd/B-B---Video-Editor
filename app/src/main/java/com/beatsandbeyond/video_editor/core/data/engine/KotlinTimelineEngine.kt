package com.beatsandbeyond.video_editor.core.data.engine

import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.Transition
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin implementation of [TimelineEngine].
 *
 * No Android SDK dependencies. Performs immutable state transformations.
 */
@Singleton
class KotlinTimelineEngine @Inject constructor() : TimelineEngine {

    override fun addClip(timeline: Timeline, trackId: String, clip: Clip): Timeline {
        val updatedTracks = timeline.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = (track.clips + clip).sortedBy { it.timelinePositionMs })
            } else track
        }
        return timeline.copy(tracks = updatedTracks)
    }

    override fun removeClip(timeline: Timeline, clipId: String): Timeline {
        val updatedTracks = timeline.tracks.map { track ->
            track.copy(clips = track.clips.filter { it.id != clipId })
        }
        return timeline.copy(tracks = updatedTracks)
    }

    override fun trimClip(clip: Clip, startMs: Long, endMs: Long): Clip {
        require(startMs >= 0) { "Start must be >= 0" }
        require(endMs > startMs) { "End must be > start" }
        // The clip stays anchored at its current timeline position; only the source
        // trim window changes. (Left-edge trims do NOT slide the clip forward — the
        // right edge stays put, matching CapCut-style editing.)
        return clip.copy(
            trimStartMs = startMs,
            trimEndMs = endMs,
        )
    }

    override fun splitClip(clip: Clip, positionMs: Long): Pair<Clip, Clip> {
        require(positionMs > 0 && positionMs < clip.durationOnTimelineMs) {
            "Split position must be within clip bounds"
        }
        val sourceOffset = (positionMs * clip.speedFactor).toLong()
        val splitSourceMs = clip.trimStartMs + sourceOffset

        val left = clip.copy(
            id = UUID.randomUUID().toString(),
            trimEndMs = splitSourceMs
        )
        val right = clip.copy(
            id = UUID.randomUUID().toString(),
            timelinePositionMs = clip.timelinePositionMs + positionMs,
            trimStartMs = splitSourceMs
        )
        return Pair(left, right)
    }

    override fun moveClip(timeline: Timeline, clipId: String, newPositionMs: Long): Timeline {
        val updatedTracks = timeline.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val updatedClips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        clip.copy(timelinePositionMs = newPositionMs)
                    } else clip
                }.sortedBy { it.timelinePositionMs }
                track.copy(clips = updatedClips)
            } else track
        }
        return timeline.copy(tracks = updatedTracks)
    }

    override fun moveClips(timeline: Timeline, clipIds: Set<String>, deltaMs: Long): Timeline {
        if (clipIds.isEmpty() || deltaMs == 0L) return timeline
        val updatedTracks = timeline.tracks.map { track ->
            val hasSelection = track.clips.any { it.id in clipIds }
            if (!hasSelection) return@map track
            val updatedClips = track.clips.map { clip ->
                if (clip.id in clipIds) {
                    clip.copy(timelinePositionMs = (clip.timelinePositionMs + deltaMs).coerceAtLeast(0L))
                } else clip
            }.sortedBy { it.timelinePositionMs }
            track.copy(clips = updatedClips)
        }
        return timeline.copy(tracks = updatedTracks)
    }

    override fun computeDuration(timeline: Timeline): Long = timeline.totalDurationMs

    override fun addTransition(timeline: Timeline, transition: Transition): Timeline = timeline
    override fun removeTransition(timeline: Timeline, transitionId: String): Timeline = timeline
}
