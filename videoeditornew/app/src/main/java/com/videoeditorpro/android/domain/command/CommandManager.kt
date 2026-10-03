package com.videoeditorpro.android.domain.command

import com.videoeditorpro.android.domain.model.EditorProject

/**
 * Manages the undo / redo history stack for an active editing session.
 */
class CommandManager(private val maxHistorySize: Int = 30) {

    private val undoStack = mutableListOf<EditCommand>()
    private val redoStack = mutableListOf<EditCommand>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val lastUndoDescription: String?
        get() = undoStack.lastOrNull()?.description

    val lastRedoDescription: String?
        get() = redoStack.lastOrNull()?.description

    /**
     * Executes a new command, applies it to the project, and clears the redo stack.
     */
    fun execute(command: EditCommand, currentProject: EditorProject): EditorProject {
        val updated = command.execute(currentProject)
        undoStack.add(command)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        return updated
    }

    /**
     * Pops the last command from the undo stack, rolls it back, and pushes to redo stack.
     */
    fun undo(currentProject: EditorProject): Pair<EditorProject, EditCommand>? {
        if (!canUndo) return null
        val command = undoStack.removeAt(undoStack.lastIndex)
        val reverted = command.undo(currentProject)
        redoStack.add(command)
        return Pair(reverted, command)
    }

    /**
     * Pops the last command from the redo stack, re-applies it, and pushes back to undo stack.
     */
    fun redo(currentProject: EditorProject): Pair<EditorProject, EditCommand>? {
        if (!canRedo) return null
        val command = redoStack.removeAt(redoStack.lastIndex)
        val reApplied = command.execute(currentProject)
        undoStack.add(command)
        return Pair(reApplied, command)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
