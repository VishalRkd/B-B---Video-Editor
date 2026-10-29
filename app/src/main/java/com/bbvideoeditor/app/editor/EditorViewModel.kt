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

data class EditorDocumentState(val project: Project, val revision: Long = 0, val durationUs: Long = project.durationUs,
    val selectedClipId: String? = null, val canUndo: Boolean = false, val canRedo: Boolean = false,
    val message: String? = null, val editTextClipId: String? = null) {
    fun withTransport(transport: EditorTransportState) = EditorUiState(project,selectedClipId,transport.positionUs,
        transport.playing,canUndo,canRedo,message,transport.scrubbing,editTextClipId)
}
data class EditorTransportState(val positionUs: Long = 0, val playing: Boolean = false, val scrubbing: Boolean = false)

/** UI state and command coordination. Players consume the same immutable Project. */
class EditorViewModel(initialProject: Project) : ViewModel() {
    private var history = EditorHistory(initialProject)
    private val mutableState = MutableStateFlow(EditorUiState(initialProject))
    /** Compatibility view for commands/tests. Production Compose subscribes to narrow flows. */
    val state = mutableState.asStateFlow()
    private val mutableDocument = MutableStateFlow(EditorDocumentState(initialProject))
    val document = mutableDocument.asStateFlow()
    private val mutableTransport = MutableStateFlow(EditorTransportState())
    val transport = mutableTransport.asStateFlow()
    val referencedMedia: Set<String> get() = history.referencedMedia
    private fun setState(next: EditorUiState) {
        val previous=mutableState.value
        mutableState.value=next
        mutableTransport.value=EditorTransportState(next.positionUs,next.playing,next.scrubbing)
        if(previous.project !== next.project || previous.selectedClipId!=next.selectedClipId || previous.canUndo!=next.canUndo ||
            previous.canRedo!=next.canRedo || previous.message!=next.message || previous.editTextClipId!=next.editTextClipId) {
            val revision=mutableDocument.value.revision + if(previous.project !== next.project)1 else 0
            mutableDocument.value=EditorDocumentState(next.project,revision,next.project.durationUs,next.selectedClipId,
                next.canUndo,next.canRedo,next.message,next.editTextClipId)
        }
    }
    val committedProject: Project get() = history.project
    var onProjectChanged: (Project) -> Unit = {}
    var onSeek: (Long) -> Unit = {}
    var onPlayingChanged: (Boolean) -> Unit = {}
    private var gestureBase: Project? = null
    private var gesturePreview: GesturePreview? = null
    private var gestureCommand: EditorCommand? = null

    fun select(id: String?) {
        setState(state.value.copy(selectedClipId = id))
        val clip = id?.let { state.value.project.findClip(it) }
        if (clip?.text != null && (state.value.positionUs < clip.startUs || state.value.positionUs >= clip.endUs)) {
            seek(clip.startUs); endScrub()
        }
    }
    fun message(text: String?) { setState(state.value.copy(message = text)) }
    fun editText(id: String?) { setState(state.value.copy(editTextClipId = id)) }
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
        gesturePreview = GesturePreview(history.project)
        gestureCommand = null
    }
    fun previewGesture(command: EditorCommand) {
        val base = gestureBase ?: return
        try {
            val preview = gesturePreview?.apply(command) ?: command.apply(base)
            setState(state.value.copy(project = preview))
            gestureCommand = command
        } catch (_: IllegalArgumentException) { /* Keep the last valid edit at a collision. */ }
    }
    fun finishGesture() {
        val command = gestureCommand
        gestureBase = null
        gesturePreview = null
        gestureCommand = null
        if (command != null) execute(command) else publish(notify = false)
    }
    fun cancelGesture() {
        gestureBase = null
        gesturePreview = null
        gestureCommand = null
        publish(notify = false)
    }
    fun seek(positionUs: Long, scrubbing: Boolean = true) {
        val position = positionUs.coerceIn(0L, document.value.durationUs)
        if (scrubbing) setPlaying(false)
        setState(state.value.copy(positionUs = position, scrubbing = scrubbing))
        onSeek(position)
    }
    fun endScrub() { setState(state.value.copy(scrubbing = false)) }
    fun setPlaying(playing: Boolean) {
        if(state.value.playing==playing)return
        setState(state.value.copy(playing = playing))
        onPlayingChanged(playing)
    }
    fun updatePlayback(positionUs: Long, playing: Boolean) {
        if (!state.value.scrubbing) {
            setState(state.value.copy(
                positionUs = positionUs.coerceIn(0L, document.value.durationUs),
                playing = playing,
            ))
        }
    }
    private fun publish(notify: Boolean = true) {
        val project = history.project
        setState(state.value.copy(
            project = project,
            selectedClipId = state.value.selectedClipId?.takeIf { project.findClip(it) != null },
            editTextClipId = state.value.editTextClipId?.takeIf { project.findClip(it) != null },
            positionUs = state.value.positionUs.coerceAtMost(project.timeline.durationUs),
            canUndo = history.canUndo, canRedo = history.canRedo,
        ))
        if (notify) onProjectChanged(project)
    }
}
