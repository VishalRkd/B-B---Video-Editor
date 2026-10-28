package com.bbvideoeditor.app.render

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.transformer.Effects
import com.bbvideoeditor.core.*

/** Each invocation owns fresh runtime effects/processors; no sharing between preview and export. */
@UnstableApi
fun interface RenderEffectFactory {
    fun create(spec: EffectSpec, clip: Clip, media: MediaReference): Effects
}

data class EffectCapability(
    val preview: Boolean = true,
    val export: Boolean = true,
    val singleInput: Boolean = false,
    val animatedParameters: Boolean = false,
)

@UnstableApi
class RenderEffectRegistry(private val factories: Map<String, RenderEffectFactory> = emptyMap()) {
    fun capability(type: String): EffectCapability? = when {
        type in factories -> EffectCapability()
        ColorAdjustments.find(type) != null -> EffectCapability(singleInput = true)
        else -> null
    }

    fun validate(spec: EffectSpec) {
        val capability = capability(spec.type) ?: throw UnsupportedRenderFeature("The effect '${spec.type}' is not installed.")
        if (!capability.preview || !capability.export) throw UnsupportedRenderFeature("'${spec.type}' requires both preview and export support.")
        if (spec.properties.isNotEmpty() && !capability.animatedParameters) {
            throw RenderCapabilityException(RenderProblem.UnsupportedAnimatedProperty,
                "Animated parameters for '${spec.type}' are not supported.")
        }
        if (spec.type !in factories) ColorAdjustments.find(spec.type)?.amount(spec)
    }

    /** Runs before opening media, allocating effects, or starting an export service. */
    fun preflight(project: Project) {
        if (project.timeline.transitions.isNotEmpty()) throw RenderCapabilityException(
            RenderProblem.UnsupportedTransition, "Transitions require a two-input transition renderer; none is installed.")
        project.timeline.tracks.forEach { track -> track.clips.forEach { clip ->
            if ((track.type == TrackType.AUDIO || track.type == TrackType.TEXT) && clip.properties.isNotEmpty()) throw RenderCapabilityException(
                RenderProblem.UnsupportedAnimatedProperty, "Animated ${track.type.name.lowercase()} properties are not supported.")
            RenderProperties.validate(clip)
            clip.effects.forEach { spec ->
                // Disabled unknown effects are preserved in the document, never executed.
                if (spec.enabled) {
                    validate(spec)
                    if (clip.text != null || track.type == TrackType.AUDIO) throw UnsupportedRenderFeature(
                        "Effect '${spec.type}' is supported on video/image clips only.")
                }
            }
        } }
    }

    fun create(spec: EffectSpec, clip: Clip, media: MediaReference): Effects {
        validate(spec)
        factories[spec.type]?.let { return it.create(spec, clip, media) }
        val descriptor = requireNotNull(ColorAdjustments.find(spec.type))
        val amount = descriptor.amount(spec)
        val effect = when (descriptor.type) {
            "brightness" -> Brightness(amount)
            "contrast" -> Contrast(amount)
            "saturation" -> HslAdjustment.Builder().adjustSaturation(amount).build()
            else -> throw UnsupportedRenderFeature("The effect '${spec.type}' is not installed.")
        }
        return Effects(emptyList(), listOf(effect))
    }
}

class UnsupportedRenderFeature(message: String) : RenderCapabilityException(RenderProblem.UnsupportedEffect, message)
