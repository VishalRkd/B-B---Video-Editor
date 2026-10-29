package com.bbvideoeditor.app.projects

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.editor.EditorViewModel
import com.bbvideoeditor.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkspaceState(
    val projects: List<Project> = emptyList(),
    val editor: EditorViewModel? = null,
    val busy: Boolean = false,
    val importing: Boolean = false,
    val message: String? = null,
    val missingMedia: List<MediaReference> = emptyList(),
)

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as EditorApplication
    private val mutableState = MutableStateFlow(WorkspaceState(busy = true))
    val state = mutableState.asStateFlow()
    private var assetLease:AutoCloseable?=null
    private var saveJob: Job? = null
    private var importJob: Job? = null

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        try {
            val projects = app.projects.list()
            app.assets.collectOrphans()
            mutableState.value = state.value.copy(projects = projects, busy = false,
                message = app.projects.issues.value.takeIf { it.isNotEmpty() }?.joinToString("\n"))
        }
        catch (e: Exception) { if (e is CancellationException) throw e; report(e) }
    }

    fun create(name: String) = viewModelScope.launch {
        mutableState.value = state.value.copy(busy = true)
        try { openProject(app.projects.create(name.ifBlank { "Untitled project" })) }
        catch (e: Exception) { if (e is CancellationException) throw e; report(e) }
    }

    fun open(id: String) = viewModelScope.launch {
        mutableState.value = state.value.copy(busy = true)
        try { openProject(app.projects.load(id)) }
        catch (e: Exception) { if (e is CancellationException) throw e; report(e) }
    }

    private suspend fun openProject(project: Project) {
        val editor = EditorViewModel(project)
        val lease=app.assets.acquire(editor)
        assetLease?.close();assetLease=lease
        editor.onProjectChanged = { changed ->
            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                delay(300)
                try { app.projects.save(changed) }
                catch (e: Exception) { if (e is CancellationException) throw e; editor.message("Could not save: ${e.message}") }
            }
        }
        mutableState.value = state.value.copy(editor = editor, busy = false, missingMedia = app.importer.unavailable(project))
        app.projects.rememberLastProject(project.id)
    }

    fun flush() {
        val project = state.value.editor?.committedProject ?: return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            try { app.projects.save(project) }
            catch (e: Exception) { if (e is CancellationException) throw e; state.value.editor?.message("Could not save: ${e.message}") }
        }
    }

    fun closeEditor() = viewModelScope.launch {
        if (state.value.importing) { state.value.editor?.message("Wait for the import to finish before closing."); return@launch }
        val editor = state.value.editor ?: return@launch
        editor.setPlaying(false)
        editor.cancelGesture()
        saveJob?.cancelAndJoin()
        try {
            app.projects.save(editor.state.value.project)
            editor.onProjectChanged = {}
            assetLease?.close();assetLease=null
            mutableState.value = state.value.copy(editor = null, projects = app.projects.list(), missingMedia = emptyList())
        } catch (e: Exception) { if (e is CancellationException) throw e; editor.message("Could not save: ${e.message}") }
    }

    fun delete(id: String) = viewModelScope.launch {
        try { app.projects.delete(id); app.assets.collectOrphans(); refresh() }
        catch (e: Exception) { if (e is CancellationException) throw e; report(e) }
    }

    fun importMedia(uris: List<Uri>, targetType: TrackType) {
        if (uris.isEmpty() || state.value.importing) return
        val editor = state.value.editor ?: return
        editor.setPlaying(false)
        mutableState.value = state.value.copy(importing = true)
        importJob = viewModelScope.launch {
            try {
                val result = app.importer.importMedia(uris)
                val current = editor.state.value
                var project = current.project
                val track = project.timeline.tracks.firstOrNull { it.id == current.selectedClipId?.let(project::findTrackForClip)?.id && it.type == targetType && !it.locked }
                    ?: project.timeline.tracks.firstOrNull { it.type == targetType && !it.locked }
                requireNotNull(track) { "Unlock a compatible track before importing." }
                var nextStart = if (targetType == TrackType.VIDEO) track.durationUs else current.positionUs
                val clips = mutableListOf<Clip>()
                val accepted = result.media.filter { track.type.accepts(it.kind) }
                for (media in accepted) {
                    val duration = if (media.kind == MediaKind.IMAGE) 5_000_000L else media.durationUs
                    val workingTrack = track.copy(clips = track.clips + clips)
                    val start = workingTrack.availableStart(nextStart, duration)
                    val clip = Clip(mediaId = media.id, startUs = start, sourceRange = TimeRange(0, duration),
                        transform = if (targetType == TrackType.OVERLAY) Transform(scaleX = .42f, scaleY = .42f, translationX = .5f, translationY = .5f) else Transform())
                    clips += clip
                    nextStart = clip.endUs
                }
                if (clips.isNotEmpty()) {
                    editor.execute(AddClips(track.id, clips, accepted))
                    editor.select(clips.first().id)
                    editor.seek(clips.first().startUs); editor.endScrub()
                    // Persist immediately after import so URI references survive interruption.
                    saveJob?.cancelAndJoin()
                    app.projects.save(editor.state.value.project)
                }
                val errors = result.errors + if (accepted.size != result.media.size) listOf("Some selected files do not belong on this track.") else emptyList()
                if (errors.isNotEmpty()) editor.message(errors.joinToString("\n"))
                mutableState.value = state.value.copy(missingMedia = app.importer.unavailable(editor.state.value.project))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                editor.message(e.message ?: "Media import failed.")
            } finally { mutableState.value = state.value.copy(importing = false) }
        }
    }

    override fun onCleared(){assetLease?.close();assetLease=null;super.onCleared()}

    fun dismissMessage() { mutableState.value = state.value.copy(message = null) }
    private fun report(error: Exception) { mutableState.value = state.value.copy(busy = false, message = error.message ?: "The project could not be opened.") }
}
