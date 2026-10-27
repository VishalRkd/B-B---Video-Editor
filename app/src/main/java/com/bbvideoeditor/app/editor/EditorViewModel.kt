package com.bbvideoeditor.app.editor

import androidx.lifecycle.ViewModel
import com.bbvideoeditor.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorUiState(
    val project: Project,
    val selectedClipId: String? = null,
    val positionUs: Long = 0L,
    val playing: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val message: String? = null,
    val scrubbing: Boolean = false,
    val editTextClipId: String? = null,
)

/** UI state and command coordination. Players consume the same immutable Project. */
class EditorViewModel(initialProject: Project) : ViewModel() {
    private var history = EditorHistory(initialProject)
    private val mutableState = MutableStateFlow(EditorUiState(initialProject))
    val state = mutableState.asStateFlow()
    val committedProject: Project get() = history.project
    var onProjectChanged: (Project) -> Unit = {}
    var onSeek: (Long) -> Unit = {}
    var onPlayingChanged: (Boolean) -> Unit = {}
    private var gestureBase: Project? = null
    private var gestureCommand: EditorCommand? = null

    fun select(id: String?) {
        mutableState.value = state.value.copy(selectedClipId = id)
        val clip = id?.let { state.value.project.findClip(it) }
        if (clip?.text != null && (state.value.positionUs < clip.startUs || state.value.positionUs >= clip.endUs)) {
            seek(clip.startUs); endScrub()
        }
    }
    fun message(text: String?) { mutableState.value = state.value.copy(message = text) }
    fun editText(id: String?) { mutableState.value = state.value.copy(editTextClipId = id) }
    fun execute(command: EditorCommand) {
        try {
            if (history.execute(command)) publish()
        } catch (e: IllegalArgumentException) { message(e.message ?: "This edit is not possible.") }
    }
    fun undo() { cancelGesture(); if (history.undo()) publish() }
    fun redo() { cancelGesture(); if (history.redo()) publish() }
    fun beginGesture() {
        setPlaying(false)
        gestureBase = history.project
        gestureCommand = null
    }
    fun previewGesture(command: EditorCommand) {
        val base = gestureBase ?: return
        try {
            val preview = command.apply(base)
            mutableState.value = state.value.copy(project = preview)
            gestureCommand = command
        } catch (_: IllegalArgumentException) { /* Keep the last valid edit at a collision. */ }
    }
    fun finishGesture() {
        val command = gestureCommand
        gestureBase = null
        gestureCommand = null
        if (command != null) execute(command) else publish(notify = false)
    }
    fun cancelGesture() {
        gestureBase = null
        gestureCommand = null
        publish(notify = false)
    }
    fun seek(positionUs: Long, scrubbing: Boolean = true) {
        val position = positionUs.coerceIn(0L, state.value.project.timeline.durationUs)
        if (scrubbing) setPlaying(false)
        mutableState.value = state.value.copy(positionUs = position, scrubbing = scrubbing)
        onSeek(position)
    }
    fun endScrub() { mutableState.value = state.value.copy(scrubbing = false) }
    fun setPlaying(playing: Boolean) {
        mutableState.value = state.value.copy(playing = playing)
        onPlayingChanged(playing)
    }
    fun updatePlayback(positionUs: Long, playing: Boolean) {
        if (!state.value.scrubbing) {
            mutableState.value = state.value.copy(
                positionUs = positionUs.coerceIn(0L, state.value.project.timeline.durationUs),
                playing = playing,
            )
        }
    }
    private fun publish(notify: Boolean = true) {
        val project = history.project
        mutableState.value = state.value.copy(
            project = project,
            selectedClipId = state.value.selectedClipId?.takeIf { project.findClip(it) != null },
            editTextClipId = state.value.editTextClipId?.takeIf { project.findClip(it) != null },
            positionUs = state.value.positionUs.coerceAtMost(project.timeline.durationUs),
            canUndo = history.canUndo, canRedo = history.canRedo,
        )
        if (notify) onProjectChanged(project)
    }
}
