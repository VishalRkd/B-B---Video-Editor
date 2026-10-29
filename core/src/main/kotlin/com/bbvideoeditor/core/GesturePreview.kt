package com.bbvideoeditor.core

/** A validated immutable gesture base permits local checks for operations that preserve IDs,
 * media, canvas and transitions. Commit still executes the original command and full validation.
 * Unknown/ripple operations use the normal command boundary. */
class GesturePreview(private val base: Project) {
    private val clips = base.timeline.tracks.flatMap { track -> track.clips.map { it.id to (track to it) } }.toMap()
    private val tracks = base.timeline.tracks.associateBy { it.id }
    private val indexes = base.timeline.tracks.associate { it.id to TrackIndex(it) }
    init { base.validate() }
    fun apply(command: EditorCommand): Project {
        val clipId = when(command) {
            is MoveClip -> command.clipId; is TrimClip -> command.clipId
            is UpdateClip -> if(command.speed==null && command.volume==null && command.effects==null && command.properties==null) command.clipId else null
            else -> null
        } ?: return command.apply(base)
        val (source,clip) = requireNotNull(clips[clipId]) { "Clip no longer exists" }
        require(!source.locked) { "Track is locked" }
        val target = if(command is MoveClip) requireNotNull(tracks[command.targetTrackId]) else source
        require(!target.locked && target.type.accepts(clip,base.media)) { "Target track cannot accept this clip" }
        val edited = when(command) {
            is MoveClip -> clip.copy(startUs=command.startUs)
            is TrimClip -> clip.copy(startUs=command.newStartUs,sourceRange=command.newSourceRange,
                durationOverrideUs=null,sourceRoundingOffsetUs=0.0)
            is UpdateClip -> clip.copy(transform=command.transform ?: clip.transform)
            else -> error("Unsupported local edit")
        }
        val media=base.media[edited.mediaId]
        require(media==null || media.kind==MediaKind.IMAGE || edited.sourceRange.endUs<=media.durationUs) { "Trim extends beyond source media" }
        require(indexes.getValue(target.id).visibleClips(edited.startUs,edited.endUs).none { it.id!=clipId }) { "Clips cannot overlap within ${target.name}" }
        return base.copy(timeline=base.timeline.copy(tracks=base.timeline.tracks.map { track ->
            when(track.id) {
                target.id -> track.copy(clips=(track.clips.filterNot { it.id==clipId }+edited).sortedBy { it.startUs })
                source.id -> track.copy(clips=track.clips.filterNot { it.id==clipId })
                else -> track
            }
        }))
    }
}
