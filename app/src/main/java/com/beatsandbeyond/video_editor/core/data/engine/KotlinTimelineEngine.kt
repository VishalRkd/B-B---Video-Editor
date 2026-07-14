package com.beatsandbeyond.video_editor.core.data.engine

import com.beatsandbeyond.video_editor.core.domain.engine.TimelineEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.Transition
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.domain.model.mapClip
import com.beatsandbeyond.video_editor.core.domain.model.insertAndResolveOverlaps
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

            if (selected.any { it.timelinePositionMs + deltaMs < 0L }) {
                overlaps = true
                return@map track
            }

            val stationary = track.clips.filter { it.id !in clipIds }
            val movedClips = selected.map { clip ->
                clip.copy(timelinePositionMs = clip.timelinePositionMs + deltaMs)
            }

            val hasOverlap = movedClips.any { moved ->
                stationary.any { other -> clipsOverlap(moved, other) }
            }
            if (hasOverlap) {
                overlaps = true
            }

            val updatedClips = track.clips.map { clip ->
                if (clip.id in clipIds) {
                    clip.copy(timelinePositionMs = clip.timelinePositionMs + deltaMs)
                } else clip
            }.sortedBy { it.timelinePositionMs }
            track.copy(clips = updatedClips)
        }

        if (overlaps) return null
        return timeline.copy(tracks = updatedTracks)
    }

    override fun computeDuration(timeline: Timeline): Long = timeline.totalDurationMs

    override fun addTransition(timeline: Timeline, transition: Transition): Timeline? {
        val track = timeline.tracks.firstOrNull { t ->
            t.clips.any { it.id == transition.fromClipId } && t.clips.any { it.id == transition.toClipId }
        } ?: return null

        val fromIndex = track.clips.indexOfFirst { it.id == transition.fromClipId }
        val toIndex = track.clips.indexOfFirst { it.id == transition.toClipId }

        if (toIndex != fromIndex + 1) return null
        
        val fromClip = track.clips[fromIndex]
        val toClip = track.clips[toIndex]
        
        if (transition.durationMs <= 0) return null
        if (transition.durationMs > fromClip.durationOnTimelineMs) return null
        if (transition.durationMs > toClip.durationOnTimelineMs) return null

        val updatedTransitions = timeline.transitions.filter { it.id != transition.id } + transition
        return timeline.copy(transitions = updatedTransitions)
    }

    override fun removeTransition(timeline: Timeline, transitionId: String): Timeline? {
        val exists = timeline.transitions.any { it.id == transitionId }
        if (!exists) return null
        val updatedTransitions = timeline.transitions.filter { it.id != transitionId }
        return timeline.copy(transitions = updatedTransitions)
    }

    override fun addClipWithOverlapResolution(timeline: Timeline, trackType: TrackType, clip: Clip): Timeline {
        val targetTrack = timeline.tracks.firstOrNull { it.type == trackType }
        return if (targetTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = trackType,
                    clips = listOf(clip),
                )
            )
        } else {
            val updatedTrack = targetTrack.insertAndResolveOverlaps(clip)
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == targetTrack.id) updatedTrack else track
                }
            )
        }
    }

    override fun addClipWithShift(timeline: Timeline, trackType: TrackType, clip: Clip): Timeline {
        val targetTrack = timeline.tracks.firstOrNull { it.type == trackType }
        val durationMs = clip.durationOnTimelineMs
        val timelinePositionMs = clip.timelinePositionMs
        return if (targetTrack == null) {
            timeline.copy(
                tracks = timeline.tracks + Track(
                    id = UUID.randomUUID().toString(),
                    type = trackType,
                    clips = listOf(clip),
                )
            )
        } else {
            val newClips = targetTrack.clips.map { c ->
                if (c.timelinePositionMs >= timelinePositionMs) {
                    c.copy(timelinePositionMs = c.timelinePositionMs + durationMs)
                } else c
            } + clip
            timeline.copy(
                tracks = timeline.tracks.map { track ->
                    if (track.id == targetTrack.id) {
                        track.copy(clips = newClips.sortedBy { it.timelinePositionMs })
                    } else track
                }
            )
        }
    }

    override fun setClipSpeed(timeline: Timeline, clipId: String, speedFactor: Float): Timeline? {
        return timeline.mapClip(clipId) { it.copy(speedFactor = speedFactor) }
    }

    override fun setClipVolume(timeline: Timeline, clipId: String, volume: Float): Timeline? {
        val clamped = volume.coerceIn(0.0f, 2.0f)
        return timeline.mapClip(clipId) { it.copy(volume = clamped) }
    }

    override fun setClipFilter(timeline: Timeline, clipId: String, filter: VideoFilter): Timeline? {
        return timeline.mapClip(clipId) { it.copy(filter = filter) }
    }
}
