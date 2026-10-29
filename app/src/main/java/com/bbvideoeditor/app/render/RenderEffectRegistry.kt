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

data class EffectParameter(val name: String, val default: Float, val range: ClosedFloatingPointRange<Float>,
    val animatable: Boolean = false) {
    init { require(name.isNotBlank() && default.isFinite() && default in range) }
}

data class RenderEffectDescriptor(override val type: String, val parameters: List<EffectParameter>,
    val capability: EffectCapability = EffectCapability(), val inputCount: Int = 1,
    val stage: GeometryStage = GeometryStage.CLIP_EFFECTS, val textureUnits: Int = 1) : EffectDescriptor {
    init { require(type.isNotBlank() && inputCount > 0 && textureUnits > 0)
        require(parameters.map { it.name }.distinct().size == parameters.size) }
}

@UnstableApi
data class RegisteredRenderEffect(val descriptor: RenderEffectDescriptor, val factory: RenderEffectFactory)

@UnstableApi
class RenderEffectRegistry(registrations: List<RegisteredRenderEffect> = emptyList()) {
    /** Compatibility adapter for existing parameterless plugins. New plugins declare a schema. */
    constructor(factories: Map<String, RenderEffectFactory>) : this(factories.map { (type, factory) ->
        RegisteredRenderEffect(RenderEffectDescriptor(type, emptyList()), factory)
    })
    private val custom = registrations.associateBy { it.descriptor.type }
    init {
        require(custom.size == registrations.size) { "Duplicate effect registration" }
        require(custom.keys.none { ColorAdjustments.find(it) != null }) { "Built-in effect IDs cannot be overridden" }
    }
    fun descriptor(type: String): RenderEffectDescriptor? = custom[type]?.descriptor ?: ColorAdjustments.find(type)?.let {
        RenderEffectDescriptor(it.type, listOf(EffectParameter("amount", it.default, it.range)), EffectCapability(singleInput = true))
    }
    fun capability(type: String): EffectCapability? = descriptor(type)?.capability

    fun validate(spec: EffectSpec) {
        val descriptor = descriptor(spec.type) ?: throw UnsupportedRenderFeature("The effect '${spec.type}' is not installed.")
        val capability = descriptor.capability
        if (!capability.preview || !capability.export || descriptor.inputCount != 1 || descriptor.stage != GeometryStage.CLIP_EFFECTS)
            throw UnsupportedRenderFeature("'${spec.type}' requires an executor for its declared input/stage capabilities.")
        val parameters = descriptor.parameters.associateBy { it.name }
        if (spec.parameters.any { (name, value) -> parameters[name]?.let { value.isFinite() && value in it.range } != true })
            throw RenderCapabilityException(RenderProblem.InvalidEffectParameter, "Invalid or unknown parameter for '${spec.type}'.")
        if (spec.properties.isNotEmpty() && (!capability.animatedParameters || spec.properties.any { property ->
                parameters[property.name]?.let { schema -> schema.animatable && property.value in schema.range &&
                    property.keyframes.all { it.value in schema.range } } != true
            })) throw RenderCapabilityException(RenderProblem.UnsupportedAnimatedProperty,
                "Animated parameters for '${spec.type}' are not supported by its declared schema.")
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
        custom[spec.type]?.let { registered ->
            val defaults = registered.descriptor.parameters.associate { it.name to it.default }
            val runtime=registered.factory.create(spec.copy(parameters = defaults + spec.parameters), clip, media)
            // Visual and audio items use independent sequences. Accepting audio processors here
            // would silently discard them from the dedicated original-source audio stream.
            if(runtime.audioProcessors.isNotEmpty())throw UnsupportedRenderFeature(
                "'${spec.type}' requires an audio-domain effect executor; visual registrations cannot provide audio processors.")
            return runtime
        }
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
