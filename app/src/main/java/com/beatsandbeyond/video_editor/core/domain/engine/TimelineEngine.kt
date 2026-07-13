package com.beatsandbeyond.video_editor.core.domain.engine

import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.Transition

/**
 * Pure business logic engine for timeline editing operations.
 *
 * All methods are pure functions — they take a current state and return a new state.
 * This design makes the engine trivially testable and enables clean undo/redo
 * by storing previous state snapshots.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement with full clip management logic.
 * Phase N: Add keyframe, animation, and multi-track synchronization support.
 */
interface TimelineEngine {

    /**
     * Appends or inserts a [Clip] onto a [Track] within the given [Timeline].
     * Returns a new [Timeline] with the clip added, or null if an overlap is detected.
     */
    fun addClip(timeline: Timeline, trackId: String, clip: Clip): Timeline?

    /**
     * Removes the clip with the given [clipId] from the timeline.
     * Returns a new [Timeline] with the clip removed.
     */
    fun removeClip(timeline: Timeline, clipId: String): Timeline

    /**
     * Returns a new [Clip] with updated trim points.
     * Validates that [startMs] < [endMs] and that the range is within the asset duration.
     */
    fun trimClip(clip: Clip, startMs: Long, endMs: Long): Clip

    /**
     * Splits a [Clip] at [positionMs] (relative to the clip's start on the timeline).
     * Returns a pair of clips: (before, after).
     */
    fun splitClip(clip: Clip, positionMs: Long): Pair<Clip, Clip>

    /**
     * Moves a clip to a new position on the timeline.
     * Returns a new [Timeline] with the clip repositioned, or null if an overlap is detected.
     */
    fun moveClip(timeline: Timeline, clipId: String, newPositionMs: Long): Timeline?

    /**
     * Moves a set of clips by the same [deltaMs] offset, preserving their relative spacing.
     * Returns a new [Timeline] with all moved clips repositioned, or null if an overlap is detected.
     */
    fun moveClips(timeline: Timeline, clipIds: Set<String>, deltaMs: Long): Timeline?

    /**
     * Returns the computed total duration of the given timeline in milliseconds.
     */
    fun computeDuration(timeline: Timeline): Long

    /**
     * Adds a [Transition] between two adjacent clips.
     * Returns a new [Timeline] containing the transition, or null if validation fails.
     */
    fun addTransition(timeline: Timeline, transition: Transition): Timeline?

    /**
     * Removes a transition by ID.
     * Returns a new [Timeline] without the specified transition, or null if not found.
     */
    fun removeTransition(timeline: Timeline, transitionId: String): Timeline?
}
