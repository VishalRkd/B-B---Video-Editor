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

    private fun clipsOverlap(a: Clip, b: Clip): Boolean {
        val aStart = a.timelinePositionMs
        val aEnd = a.timelineEndMs
        val bStart = b.timelinePositionMs
        val bEnd = b.timelineEndMs
        return aStart < bEnd && aEnd > bStart
    }

    override fun addClip(timeline: Timeline, trackId: String, clip: Clip): Timeline? {
        val targetTrack = timeline.tracks.firstOrNull { it.id == trackId } ?: return null
        val overlaps = targetTrack.clips.any { other -> clipsOverlap(clip, other) }
        if (overlaps) return null

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

    override fun moveClip(timeline: Timeline, clipId: String, newPositionMs: Long): Timeline? {
        val targetTrack = timeline.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } } ?: return null
        val clip = targetTrack.clips.first { it.id == clipId }
        val movedClip = clip.copy(timelinePositionMs = newPositionMs)

        val overlaps = targetTrack.clips.any { other ->
            other.id != clipId && clipsOverlap(movedClip, other)
        }
        if (overlaps) return null

        val updatedTracks = timeline.tracks.map { track ->
            if (track.id == targetTrack.id) {
                val updatedClips = track.clips.map { c ->
                    if (c.id == clipId) movedClip else c
                }.sortedBy { it.timelinePositionMs }
                track.copy(clips = updatedClips)
            } else track
        }
        return timeline.copy(tracks = updatedTracks)
    }

    override fun moveClips(timeline: Timeline, clipIds: Set<String>, deltaMs: Long): Timeline? {
        if (clipIds.isEmpty()) return timeline
        if (deltaMs == 0L) return timeline

        var overlaps = false
        val updatedTracks = timeline.tracks.map { track ->
            val selected = track.clips.filter { it.id in clipIds }
            if (selected.isEmpty()) return@map track

            val stationary = track.clips.filter { it.id !in clipIds }
            val movedClips = selected.map { clip ->
                clip.copy(timelinePositionMs = (clip.timelinePositionMs + deltaMs).coerceAtLeast(0L))
            }

            val hasOverlap = movedClips.any { moved ->
                stationary.any { other -> clipsOverlap(moved, other) }
            }
            if (hasOverlap) {
                overlaps = true
            }

            val updatedClips = track.clips.map { clip ->
                if (clip.id in clipIds) {
                    clip.copy(timelinePositionMs = (clip.timelinePositionMs + deltaMs).coerceAtLeast(0L))
                } else clip
            }.sortedBy { it.timelinePositionMs }
            track.copy(clips = updatedClips)
        }

        if (overlaps) return null
        return timeline.copy(tracks = updatedTracks)
    }

    override fun computeDuration(timeline: Timeline): Long = timeline.totalDurationMs

    override fun addTransition(timeline: Timeline, transition: Transition): Timeline {
        val track = timeline.tracks.firstOrNull { t ->
            t.clips.any { it.id == transition.fromClipId } && t.clips.any { it.id == transition.toClipId }
        } ?: throw IllegalArgumentException("Clips must be on the same track")

        val fromIndex = track.clips.indexOfFirst { it.id == transition.fromClipId }
        val toIndex = track.clips.indexOfFirst { it.id == transition.toClipId }

        require(toIndex == fromIndex + 1) { "Clips must be adjacent on the track" }
        
        val fromClip = track.clips[fromIndex]
        val toClip = track.clips[toIndex]
        
        require(transition.durationMs > 0) { "Transition duration must be positive" }
        require(transition.durationMs <= fromClip.durationOnTimelineMs) { 
            "Transition duration ${transition.durationMs}ms exceeds outgoing clip duration ${fromClip.durationOnTimelineMs}ms" 
        }
        require(transition.durationMs <= toClip.durationOnTimelineMs) { 
            "Transition duration ${transition.durationMs}ms exceeds incoming clip duration ${toClip.durationOnTimelineMs}ms" 
        }

        val updatedTransitions = timeline.transitions.filter { it.id != transition.id } + transition
        return timeline.copy(transitions = updatedTransitions)
    }

    override fun removeTransition(timeline: Timeline, transitionId: String): Timeline {
        val updatedTransitions = timeline.transitions.filter { it.id != transitionId }
        return timeline.copy(transitions = updatedTransitions)
    }
}
