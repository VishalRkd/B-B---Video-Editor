package com.bbvideoeditor.core

/** Stable machine-readable failures shared by render preflight and both executors. */
enum class RenderProblem {
    UnsupportedEffect, InvalidEffectParameter, UnsupportedAnimatedProperty, UnsupportedTransition,
}

open class RenderCapabilityException(val problem: RenderProblem, message: String) : IllegalArgumentException(message)

/** Stored in EffectSpec.parameters["amount"]. Neutral is zero for all three adjustments. */
data class ColorAdjustment(val type: String, val label: String, val range: ClosedFloatingPointRange<Float>) {
    val default: Float = 0f
    fun amount(spec: EffectSpec): Float {
        val amount = spec.parameters["amount"] ?: default
        if (spec.parameters.keys.any { it != "amount" } || !amount.isFinite() || amount !in range) {
            throw RenderCapabilityException(RenderProblem.InvalidEffectParameter,
                "$label requires a finite amount in $range and no unknown parameters.")
        }
        return amount
    }
}

object ColorAdjustments {
    val brightness = ColorAdjustment("brightness", "Brightness", -1f..1f)
    val contrast = ColorAdjustment("contrast", "Contrast", -1f..1f)
    val saturation = ColorAdjustment("saturation", "Saturation", -100f..100f)
    val all = listOf(brightness, contrast, saturation)
    fun find(type: String) = all.firstOrNull { it.type == type }
}

/** Clip properties replace static values, in absolute source microseconds (including trim/speed).
 * No per-frame sorting, validation, or project traversal. Validate once at the render boundary.
 */
object RenderProperties {
    val names = setOf("opacity", "translationX", "translationY", "scaleX", "scaleY", "rotationDegrees")

    fun validate(clip: Clip) {
        val seen = mutableSetOf<String>()
        clip.properties.forEach { property ->
            if (property.name !in names || !seen.add(property.name)) {
                throw RenderCapabilityException(RenderProblem.UnsupportedAnimatedProperty,
                    "Unsupported or repeated clip property '${property.name}'.")
            }
            fun valid(value: Float) = when (property.name) {
                "opacity" -> value in 0f..1f
                "scaleX", "scaleY" -> value > 0 && value.isFinite()
                else -> value.isFinite()
            }
            if (!valid(property.value) || property.keyframes.any { !valid(it.value) }) {
                throw RenderCapabilityException(RenderProblem.UnsupportedAnimatedProperty,
                    "Invalid values for '${property.name}'.")
            }
        }
    }

    fun transformAt(clip: Clip, timelineTimeUs: Long): Transform {
        if (clip.properties.isEmpty()) return clip.transform
        val sourceUs = clip.sourceTimeAt(timelineTimeUs)
        var transform = clip.transform
        for (property in clip.properties) {
            val value = property.valueAt(sourceUs)
            transform = when (property.name) {
                "opacity" -> transform.copy(opacity = value)
                "translationX" -> transform.copy(translationX = value)
                "translationY" -> transform.copy(translationY = value)
                "scaleX" -> transform.copy(scaleX = value)
                "scaleY" -> transform.copy(scaleY = value)
                "rotationDegrees" -> transform.copy(rotationDegrees = value)
                else -> throw RenderCapabilityException(RenderProblem.UnsupportedAnimatedProperty, "Unsupported clip property '${property.name}'.")
            }
        }
        return transform
    }
}

/** A derived index over immutable editing data, never a second editable project. */
class RenderTrack(val track: Track) {
    private val index = TrackIndex(track)
    fun clipAt(timeUs: Long): Clip? = index.clipAt(timeUs)
    fun stateAt(timeUs: Long): ClipRenderState? = clipAt(timeUs)?.let {
        ClipRenderState(it, timeUs, it.sourceTimeAt(timeUs), RenderProperties.transformAt(it, timeUs))
    }
}

data class ClipRenderState(val clip: Clip, val timelineTimeUs: Long, val sourceTimeUs: Long, val transform: Transform)
data class RenderFrame(val visuals: List<ClipRenderState>, val audio: List<ClipRenderState>, val text: List<ClipRenderState>)

/**
 * Canonical ordering: decoder orientation -> fit -> ordered clip effects -> compositor scale,
 * rotation, translation, opacity -> media stack -> text stack. Canvas outside content is black.
 * Visuals are top first (Media3); text is bottom first (overlay painter). Muting affects audio
 * only, locking affects editing only. Gaps use half-open [start,end) membership.
 */
class RenderPlan private constructor(val project: Project) {
    val durationUs = project.durationUs
    val visuals = project.timeline.tracks.filter {
        (it.type == TrackType.VIDEO || it.type == TrackType.OVERLAY) && it.clips.isNotEmpty()
    }.asReversed().map(::RenderTrack)
    val text = project.timeline.tracks.filter { it.type == TrackType.TEXT && it.clips.isNotEmpty() }.map(::RenderTrack)
    val audio = project.timeline.tracks.filterNot { it.muted }.mapNotNull { track ->
        val audible = track.clips.filter { project.media[it.mediaId]?.hasAudio == true }
        if (audible.isEmpty()) null else RenderTrack(track.copy(type = TrackType.AUDIO, clips = audible))
    }
    val visualClips = visuals.flatMap { it.track.clips }.sortedBy { it.startUs }
    val disjointVisuals = visualClips.zipWithNext().all { (a, b) -> a.endUs <= b.startUs }
    fun frameAt(timeUs: Long) = RenderFrame(visuals.mapNotNull { it.stateAt(timeUs) },
        audio.mapNotNull { it.stateAt(timeUs) }, text.mapNotNull { it.stateAt(timeUs) })

    companion object {
        fun derive(project: Project): RenderPlan = RenderPlan(project.validate())
    }
}

/** Scheduling contract for future TWO-input renderers, not an executable filter.
 * Explicit overlap only: no hidden source handles, ripple, or timeline duration changes.
 * Current executors reject all transitions until a compositor implementation is registered.
 */
data class TransitionWindow(val spec: TransitionSpec, val outgoing: Clip, val incoming: Clip, val range: TimeRange) {
    fun progressAt(timeUs: Long): Float = ((timeUs - range.startUs).toDouble() / range.durationUs).coerceIn(0.0, 1.0).toFloat()
    companion object {
        fun schedule(project: Project, spec: TransitionSpec): TransitionWindow {
            val outgoing = requireNotNull(project.findClip(spec.fromClipId))
            val incoming = requireNotNull(project.findClip(spec.toClipId))
            require(outgoing.id != incoming.id && outgoing.text == null && incoming.text == null)
            require(project.findTrackForClip(outgoing.id)?.type in setOf(TrackType.VIDEO, TrackType.OVERLAY))
            require(project.findTrackForClip(incoming.id)?.type in setOf(TrackType.VIDEO, TrackType.OVERLAY))
            val range = TimeRange(incoming.startUs, spec.durationUs)
            require(outgoing.startUs <= range.startUs && range.endUs <= outgoing.endUs && range.endUs <= incoming.endUs) {
                "A transition requires both visual clips throughout its explicit overlap."
            }
            return TransitionWindow(spec, outgoing, incoming, range)
        }
    }
}
