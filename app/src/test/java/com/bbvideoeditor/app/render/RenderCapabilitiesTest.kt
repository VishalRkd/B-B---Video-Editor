package com.bbvideoeditor.app.render

import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Test

class RenderCapabilitiesTest {
    private val media = MediaReference(uri = "file:missing", displayName = "fixture", kind = MediaKind.VIDEO, durationUs = 2_000_000, hasAudio = true)
    private val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 1_000_000))
    private fun project(c: Clip = clip, type: TrackType = TrackType.VIDEO) = Project(media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "track", type = type, clips = listOf(c)))))
    private val registry = RenderEffectRegistry()

    @Test fun shippedEffectsHaveParityCapabilitiesAndAllowSingleInputRendering() {
        ColorAdjustments.all.forEach { definition ->
            val capability = requireNotNull(registry.capability(definition.type))
            assertTrue(capability.preview && capability.export)
            assertFalse(capability.animatedParameters)
            assertTrue(capability.singleInput)
            registry.preflight(project(clip.copy(effects = listOf(EffectSpec(type = definition.type)))))
        }
    }
    @Test fun unknownEffectsFailAndDisabledSpecsRemainExplicitlyDisabled() {
        val spec = EffectSpec(type = "missing-plugin")
        assertEquals(RenderProblem.UnsupportedEffect, assertThrows(RenderCapabilityException::class.java) {
            registry.preflight(project(clip.copy(effects = listOf(spec))))
        }.problem)
        registry.preflight(project(clip.copy(effects = listOf(spec.copy(enabled = false)))))
    }
    @Test fun animationAndTrackCapabilitiesAreEnforcedEvenWhenMuted() {
        val spec = EffectSpec(type = "brightness", properties = listOf(AnimatedProperty("amount", 0f)))
        assertEquals(RenderProblem.UnsupportedAnimatedProperty, assertThrows(RenderCapabilityException::class.java) {
            registry.preflight(project(clip.copy(effects = listOf(spec))))
        }.problem)
        assertThrows(RenderCapabilityException::class.java) { registry.preflight(project(clip.copy(effects = listOf(EffectSpec(type = "contrast"))), TrackType.AUDIO)) }
        val text = Clip(startUs = 0, sourceRange = TimeRange(0, 1_000_000), text = TextContent(text = "Caption"), properties = listOf(AnimatedProperty("opacity", .5f)))
        assertThrows(RenderCapabilityException::class.java) { registry.preflight(project(text, TrackType.TEXT)) }
    }
    @Test fun transitionsAreReportedBeforeAnyExportOrMediaWork() {
        val p = project().let { it.copy(timeline = it.timeline.copy(transitions = listOf(TransitionSpec(type = "cross_dissolve", durationUs = 1, fromClipId = clip.id, toClipId = clip.id)))) }
        assertEquals(RenderProblem.UnsupportedTransition, assertThrows(RenderCapabilityException::class.java) { registry.preflight(p) }.problem)
    }
}
