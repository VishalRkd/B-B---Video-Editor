package com.videoeditorpro.android.domain.command

import com.videoeditorpro.android.domain.model.EditorProject

/**
 * Encapsulates an editing operation that can be executed and reversed.
 * Enables full Undo / Redo capability across the entire editor.
 */
interface EditCommand {
    val description: String

    /**
     * Applies the modification to [project] and returns the updated project state.
     */
    fun execute(project: EditorProject): EditorProject

    /**
     * Reverses the modification made by [execute] and returns the previous project state.
     */
    fun undo(project: EditorProject): EditorProject
}
