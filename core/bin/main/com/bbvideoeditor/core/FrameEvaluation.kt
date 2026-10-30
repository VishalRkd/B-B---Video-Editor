package com.bbvideoeditor.core

@JvmInline value class OutputFrameTime(val us: Long)
@JvmInline value class TimelineTime(val us: Long)
@JvmInline value class ClipLocalTime(val us: Long)
@JvmInline value class SourceTime(val us: Long)

data class ClipTimeMapping(val output: OutputFrameTime, val timeline: TimelineTime,
    val local: ClipLocalTime, val source: SourceTime)

/** Output and timeline currently share the same origin. Keyframes remain absolute SOURCE time. */
fun Clip.mapTime(time: OutputFrameTime) = ClipTimeMapping(time, TimelineTime(time.us),
    ClipLocalTime(time.us.coerceIn(startUs,endUs) - startUs), SourceTime(sourceTimeAt(time.us)))

/** Extension slots are named, not implemented features. No crop/mask data is serialized yet. */
enum class GeometryStage { DECODE_ORIENTATION, SOURCE_GEOMETRY, FIT, CLIP_EFFECTS, TRANSFORM,
    OPACITY, MEDIA_COMPOSITE, TEXT_OVERLAYS, OUTPUT_ENCODING }
val canonicalGeometryStages = GeometryStage.entries.toList()

data class EvaluatedLayer(val trackId: String, val stackOrder: Int, val muted: Boolean,
    val clip: Clip, val time: ClipTimeMapping, val staticTransform: Transform,
    val transform: Transform, val effects: List<EffectSpec>) {
    val opacity: Float get() = transform.opacity
    val gain: Float get() = if (muted) 0f else clip.volume
}
data class EvaluatedTransition(val window: TransitionWindow, val outgoing: EvaluatedLayer,
    val incoming: EvaluatedLayer, val progress: Float)
data class EvaluatedFrameState(val time: OutputFrameTime, val canvas: CanvasSettings,
    val visuals: List<EvaluatedLayer>, val audio: List<EvaluatedLayer>, val text: List<EvaluatedLayer>,
    val transitions: List<EvaluatedTransition>) {
    /** Sum of user gains. Channel downmix and correlated sources can require further headroom. */
    val audioGainSum: Float get() = audio.sumOf { it.gain.toDouble() }.toFloat()
}

internal fun RenderTrack.evaluate(time: OutputFrameTime, order: Int): EvaluatedLayer? = clipAt(time.us)?.let { clip ->
    EvaluatedLayer(track.id, order, track.muted, clip, clip.mapTime(time), clip.transform,
        RenderProperties.transformAt(clip, time.us), clip.effects.filter { it.enabled })
}

/** Relinking changes one source identity's location/metadata, never any edit decisions. */
data class RelinkMedia(val mediaId: String, val replacement: MediaReference) : EditorCommand {
    override val label = "Relink media"
    override fun apply(project: Project): Project {
        val old = requireNotNull(project.media[mediaId]) { "Source no longer exists" }
        require(old.kind == replacement.kind) { "Replacement must have the same media kind" }
        val clips = project.timeline.clips.filter { it.mediaId == mediaId }
        require(replacement.kind == MediaKind.IMAGE || clips.all { it.sourceRange.endUs <= replacement.durationUs }) {
            "Replacement is shorter than the existing source ranges"
        }
        require(!old.hasAudio || replacement.hasAudio) { "Replacement must preserve the existing audio stream" }
        return project.copy(media = project.media + (mediaId to replacement.copy(id = mediaId))).validate()
    }
}
