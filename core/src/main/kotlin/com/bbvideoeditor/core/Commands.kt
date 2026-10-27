package com.bbvideoeditor.core

import kotlin.math.roundToLong

/** Commands are pure. Failed validation leaves both the document and undo stacks untouched. */
interface EditorCommand {
    val label: String
    fun apply(project: Project): Project
}

private fun Project.editTrack(trackId: String, operation: (Track) -> Track): Project {
    val track = requireNotNull(findTrack(trackId)) { "Track no longer exists" }
    require(!track.locked) { "Track is locked" }
    return copy(timeline = timeline.copy(tracks = timeline.tracks.map { if (it.id == trackId) operation(it) else it })).validate()
}

private fun Project.requireClip(clipId: String): Pair<Track, Clip> {
    val track = requireNotNull(findTrackForClip(clipId)) { "Clip no longer exists" }
    require(!track.locked) { "Track is locked" }
    return track to track.clips.first { it.id == clipId }
}

data class AddClips(val trackId: String, val clips: List<Clip>, val media: List<MediaReference> = emptyList()) : EditorCommand {
    override val label: String = "Import clips"
    override fun apply(project: Project): Project {
        val additions = media.associateBy { it.id }
        require(additions.size == media.size) { "Duplicate media ID" }
        additions.forEach { (id, reference) ->
            require(project.media[id] == null || project.media[id] == reference) { "Media ID already refers to another source" }
        }
        return project.copy(media = project.media + additions).editTrack(trackId) { track ->
            track.copy(clips = (track.clips + clips).sortedBy { it.startUs })
        }
    }
}

/** Text is native project content and never creates a synthetic media reference. */
data class AddTextClip(val clip: Clip, val trackId: String? = null, val newTrackId: String = newId()) : EditorCommand {
    override val label = "Add text"
    override fun apply(project: Project): Project {
        require(clip.text != null)
        val target = trackId?.let { requireNotNull(project.findTrack(it)) }
            ?: project.timeline.tracks.firstOrNull { track ->
                track.type == TrackType.TEXT && !track.locked && track.clips.none { clip.startUs < it.endUs && clip.endUs > it.startUs }
            }
        if (target != null) return AddClips(target.id, listOf(clip)).apply(project)
        return project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks +
            Track(id = newTrackId, name = "Text ${project.timeline.tracks.count { it.type == TrackType.TEXT } + 1}",
                type = TrackType.TEXT, clips = listOf(clip)))).validate()
    }
}

data class UpdateTextClip(val clipId: String, val content: TextContent, val startUs: Long, val durationUs: Long) : EditorCommand {
    override val label = "Edit text"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        require(clip.text != null)
        val edited = clip.copy(text = content, startUs = startUs, sourceRange = TimeRange(clip.sourceRange.startUs, durationUs),
            durationOverrideUs = null, sourceRoundingOffsetUs = 0.0)
        return project.editTrack(track.id) { it.copy(clips = it.clips.map { item -> if (item.id == clipId) edited else item }.sortedBy { item -> item.startUs }) }
    }
}

data class MoveClip(val clipId: String, val targetTrackId: String, val startUs: Long) : EditorCommand {
    override val label: String = "Move clip"
    override fun apply(project: Project): Project {
        val (source, clip) = project.requireClip(clipId)
        val target = requireNotNull(project.findTrack(targetTrackId)) { "Target track no longer exists" }
        require(!target.locked) { "Target track is locked" }
        val moved = clip.copy(startUs = startUs)
        return project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks.map { track ->
            when (track.id) {
                target.id -> track.copy(clips = (track.clips.filterNot { it.id == clipId } + moved).sortedBy { it.startUs })
                source.id -> track.copy(clips = track.clips.filterNot { it.id == clipId })
                else -> track
            }
        })).validate()
    }
}

data class TrimClip(val clipId: String, val newSourceRange: TimeRange, val newStartUs: Long) : EditorCommand {
    override val label: String = "Trim clip"
    override fun apply(project: Project): Project {
        val (track, _) = project.requireClip(clipId)
        return project.editTrack(track.id) { original ->
            original.copy(clips = original.clips.map {
                if (it.id == clipId) it.copy(startUs = newStartUs, sourceRange = newSourceRange, durationOverrideUs = null, sourceRoundingOffsetUs = 0.0) else it
            }.sortedBy { it.startUs })
        }
    }
}

data class SplitClip(val clipId: String, val atUs: Long, val newClipId: String = newId()) : EditorCommand {
    override val label: String = "Split clip"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        require(atUs > clip.startUs && atUs < clip.endUs) { "Place the playhead inside the clip to split it" }
        val leftDurationUs = atUs - clip.startUs
        val exactSplitSourceUs = leftDurationUs.toDouble() * clip.speed + clip.sourceRoundingOffsetUs
        val splitSourceUs = exactSplitSourceUs.roundToLong()
        require(splitSourceUs > 0 && splitSourceUs < clip.sourceRange.durationUs) { "Split is too close to the source boundary" }
        val left = clip.copy(
            sourceRange = TimeRange(clip.sourceRange.startUs, splitSourceUs),
            durationOverrideUs = leftDurationUs,
        )
        val right = clip.copy(
            id = newClipId,
            startUs = atUs,
            sourceRange = TimeRange(clip.sourceRange.startUs + splitSourceUs, clip.sourceRange.durationUs - splitSourceUs),
            durationOverrideUs = clip.endUs - atUs,
            sourceRoundingOffsetUs = exactSplitSourceUs - splitSourceUs,
        )
        return project.editTrack(track.id) {
            it.copy(clips = it.clips.flatMap { existing -> if (existing.id == clipId) listOf(left, right) else listOf(existing) })
        }
    }
}

data class DeleteClip(val clipId: String, val ripple: Boolean = false) : EditorCommand {
    override val label: String = "Delete clip"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        // Remove references before validating the clip deletion.
        val withoutTransitions = project.copy(timeline = project.timeline.copy(transitions = project.timeline.transitions.filterNot {
            it.fromClipId == clipId || it.toClipId == clipId
        }))
        return withoutTransitions.editTrack(track.id) {
            it.copy(clips = it.clips.filterNot { existing -> existing.id == clipId }.map { existing ->
                if (ripple && existing.startUs >= clip.endUs) existing.copy(startUs = existing.startUs - clip.durationUs) else existing
            })
        }
    }
}

/** Insert a copy immediately after the original and shift later clips without discarding gaps. */
data class DuplicateClip(val clipId: String, val newClipId: String = newId()) : EditorCommand {
    override val label: String = "Duplicate clip"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        val duplicate = clip.copy(id = newClipId, startUs = clip.endUs)
        return project.editTrack(track.id) {
            it.copy(clips = (it.clips.map { existing ->
                if (existing.startUs >= clip.endUs) existing.copy(startUs = Math.addExact(existing.startUs, clip.durationUs)) else existing
            } + duplicate).sortedBy { existing -> existing.startUs })
        }
    }
}

/** Ripple reorder explicitly closes interior gaps and preserves the first clip's starting offset. */
data class ReorderClip(val clipId: String, val index: Int) : EditorCommand {
    override val label: String = "Reorder clip"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        require(index in track.clips.indices) { "Invalid destination index" }
        val sorted = track.clips.sortedBy { it.startUs }
        val reordered = sorted.filterNot { it.id == clipId }.toMutableList().apply { add(index, clip) }
        var position = sorted.first().startUs
        val clips = reordered.map { item -> item.copy(startUs = position).also { position = Math.addExact(position, item.durationUs) } }
        return project.editTrack(track.id) { it.copy(clips = clips) }
    }
}

data class UpdateClip(
    val clipId: String,
    val speed: Float? = null,
    val volume: Float? = null,
    val transform: Transform? = null,
    val effects: List<EffectSpec>? = null,
    val properties: List<AnimatedProperty>? = null,
    val ripple: Boolean = true,
) : EditorCommand {
    override val label: String = "Adjust clip"
    override fun apply(project: Project): Project {
        val (track, clip) = project.requireClip(clipId)
        val edited = clip.copy(
            speed = speed ?: clip.speed,
            volume = volume ?: clip.volume,
            transform = transform ?: clip.transform,
            effects = effects ?: clip.effects,
            properties = properties ?: clip.properties,
            durationOverrideUs = if (speed != null && speed != clip.speed) null else clip.durationOverrideUs,
            sourceRoundingOffsetUs = if (speed != null && speed != clip.speed) 0.0 else clip.sourceRoundingOffsetUs,
        )
        val durationChange = edited.durationUs - clip.durationUs
        return project.editTrack(track.id) {
            it.copy(clips = it.clips.map { existing ->
                when {
                    existing.id == clipId -> edited
                    ripple && existing.startUs >= clip.endUs -> existing.copy(startUs = Math.addExact(existing.startUs, durationChange))
                    else -> existing
                }
            })
        }
    }
}

data class AddTrack(val track: Track) : EditorCommand {
    constructor(type: TrackType, name: String) : this(Track(name = name, type = type))
    override val label: String = "Add track"
    override fun apply(project: Project): Project = project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks + track)).validate()
}

data class RenameProject(val name: String) : EditorCommand {
    override val label: String = "Rename project"
    override fun apply(project: Project): Project = project.copy(name = name.trim()).validate()
}

data class SetCanvas(val canvas: CanvasSettings) : EditorCommand {
    override val label: String = "Change canvas"
    override fun apply(project: Project): Project = project.copy(canvas = canvas).validate()
}

data class SetTrackMuted(val trackId: String, val muted: Boolean) : EditorCommand {
    override val label: String = "Mute track"
    override fun apply(project: Project): Project = project.editTrack(trackId) { it.copy(muted = muted) }
}

data class SetTrackLocked(val trackId: String, val locked: Boolean) : EditorCommand {
    override val label: String = "Lock track"
    override fun apply(project: Project): Project {
        requireNotNull(project.findTrack(trackId)) { "Track no longer exists" }
        return project.copy(timeline = project.timeline.copy(tracks = project.timeline.tracks.map {
            if (it.id == trackId) it.copy(locked = locked) else it
        })).validate()
    }
}

/** A logical edit can consist of many operations while still being one undo step. */
data class CompositeCommand(override val label: String, val commands: List<EditorCommand>) : EditorCommand {
    override fun apply(project: Project): Project = commands.fold(project) { state, command -> command.apply(state) }.validate()
}

/** Snapshot history shares immutable clips/media and is bounded to prevent unlimited retention. */
class EditorHistory(initial: Project, private val capacity: Int = 100) {
    private data class Entry(val project: Project, val label: String)
    private val past = ArrayDeque<Entry>()
    private val future = ArrayDeque<Entry>()
    var project: Project = initial.validate()
        private set
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()
    val undoLabel: String? get() = past.lastOrNull()?.label
    val redoLabel: String? get() = future.lastOrNull()?.label

    init { require(capacity > 0) }

    /** Returns false for a no-op. Invalid commands throw and preserve the current history. */
    fun execute(command: EditorCommand): Boolean {
        val result = command.apply(project).validate()
        if (result == project) return false
        past.addLast(Entry(project, command.label))
        while (past.size > capacity) past.removeFirst()
        future.clear()
        project = result.copy(updatedAt = System.currentTimeMillis())
        return true
    }

    fun undo(): Boolean {
        val entry = past.removeLastOrNull() ?: return false
        future.addLast(Entry(project, entry.label))
        project = entry.project.copy(updatedAt = System.currentTimeMillis())
        return true
    }

    fun redo(): Boolean {
        val entry = future.removeLastOrNull() ?: return false
        past.addLast(Entry(project, entry.label))
        project = entry.project.copy(updatedAt = System.currentTimeMillis())
        return true
    }

    fun replace(project: Project) {
        val valid = project.validate()
        this.project = valid
        past.clear()
        future.clear()
    }
}
