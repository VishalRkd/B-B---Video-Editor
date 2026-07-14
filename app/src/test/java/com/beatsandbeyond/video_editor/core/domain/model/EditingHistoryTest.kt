package com.beatsandbeyond.video_editor.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [EditingHistory].
 */
class EditingHistoryTest {

    private lateinit var project1: Project
    private lateinit var project2: Project
    private lateinit var project3: Project

    @Before
    fun setUp() {
        val emptyTimeline = Timeline(id = "t_1")
        project1 = Project(id = "p", name = "P1", createdAt = 0L, updatedAt = 0L, timeline = emptyTimeline)
        project2 = project1.copy(name = "P2")
        project3 = project2.copy(name = "P3")
    }

    @Test
    fun `initial history state is empty`() {
        val history = EditingHistory()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `pushing state updates stacks`() {
        val history = EditingHistory().push(project1)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `undo returns previous state`() {
        val history = EditingHistory().push(project1)
        val undoResult = history.undo(project2)

        assertTrue(undoResult != null)
        val (newHistory, revertedState) = undoResult!!
        assertEquals(project1, revertedState)
        assertFalse(newHistory.canUndo)
        assertTrue(newHistory.canRedo)
    }

    @Test
    fun `redo returns next state`() {
        val history = EditingHistory().push(project1)
        val undoResult = history.undo(project2)
        val (historyAfterUndo, revertedState) = undoResult!!

        val redoResult = historyAfterUndo.redo(revertedState)
        assertTrue(redoResult != null)
        val (newHistory, redoneState) = redoResult!!
        assertEquals(project2, redoneState)
        assertTrue(newHistory.canUndo)
        assertFalse(newHistory.canRedo)
    }

    @Test
    fun `pushing new state clears redo stack`() {
        val history = EditingHistory().push(project1)
        val (historyAfterUndo, revertedState) = history.undo(project2)!!
        
        // Pushing a new action after undo
        val historyAfterPush = historyAfterUndo.push(revertedState)
        assertTrue(historyAfterPush.canUndo)
        assertFalse(historyAfterPush.canRedo) // Redo stack cleared
    }

    @Test
    fun `max size is enforced`() {
        var history = EditingHistory(maxSize = 2)
        
        // Push 3 states (retaining current state outside)
        history = history.push(project1)
        history = history.push(project2)
        
        // Past stack has size 2 (retains project1 and project2)
        assertEquals(2, history.past.size)
        assertEquals(project1, history.past[0])
        assertEquals(project2, history.past[1])

        // Add one more
        history = history.push(project3)

        // Oldest state (project1) should be dropped
        assertEquals(2, history.past.size)
        assertEquals(project2, history.past[0])
        assertEquals(project3, history.past[1])
    }
}
