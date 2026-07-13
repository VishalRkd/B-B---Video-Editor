package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Immutable undo/redo history stack for project editing.
 *
 * Uses a copy-on-write approach: every edit creates a new [EditingHistory] instance.
 * Maximum [maxSize] past states are kept to limit memory usage.
 */
data class EditingHistory(
    val past: List<Project> = emptyList(),
    val future: List<Project> = emptyList(),
    val maxSize: Int = 50,
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /**
     * Records [newState] as the current state, adding [current] to the past stack.
     * Clears the redo (future) stack.
     */
    fun push(current: Project, newState: Project): EditingHistory = copy(
        past = (past + current).takeLast(maxSize),
        future = emptyList(),
    )

    /**
     * Reverts to the previous state. Returns null if nothing to undo.
     * Returns [Pair] of (new history, reverted project state).
     */
    fun undo(current: Project): Pair<EditingHistory, Project>? {
        if (!canUndo) return null
        return Pair(
            copy(past = past.dropLast(1), future = listOf(current) + future),
            past.last(),
        )
    }

    /**
     * Re-applies the most recently undone state. Returns null if nothing to redo.
     */
    fun redo(current: Project): Pair<EditingHistory, Project>? {
        if (!canRedo) return null
        return Pair(
            copy(past = past + current, future = future.drop(1)),
            future.first(),
        )
    }
}
