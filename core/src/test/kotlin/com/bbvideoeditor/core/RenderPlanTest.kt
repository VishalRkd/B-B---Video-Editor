package com.bbvideoeditor.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RenderPlanTest {
    private val media = MediaReference(id = "video", uri = "file:test", displayName = "test", kind = MediaKind.VIDEO,
        durationUs = 10_000_000, hasAudio = true)
    private fun project(vararg tracks: Track) = Project(media = mapOf(media.id to media), timeline = Timeline(tracks.toList()))
    private fun clip(id: String, start: Long = 0, duration: Long = 2_000_000) =
        Clip(id = id, mediaId = media.id, startUs = start, sourceRange = TimeRange(500_000, duration))

    @Test fun activeStatesShareHalfOpenBoundsSourceMappingLayerOrderAndMuteRules() {
        val base = clip("base").copy(speed = 2f)
        val overlay = clip("overlay", 250_000, 500_000).copy(transform = Transform(opacity = .25f))
        val plan = RenderPlan.derive(project(
            Track(name = "base", type = TrackType.VIDEO, clips = listOf(base)),
            Track(name = "top", type = TrackType.OVERLAY, clips = listOf(overlay), muted = true, locked = true)))
        assertEquals(listOf("overlay", "base"), plan.frameAt(250_000).visuals.map { it.clip.id })
        assertEquals(1_000_000L, plan.frameAt(250_000).visuals.last().sourceTimeUs)
        assertEquals(.25f, plan.frameAt(250_000).visuals.first().transform.opacity)
        assertEquals(listOf("base"), plan.frameAt(250_000).audio.map { it.clip.id })
        assertEquals(listOf("base"), plan.frameAt(750_000).visuals.map { it.clip.id })
        assertTrue(plan.frameAt(1_000_000).visuals.isEmpty())
        assertTrue(plan.frameAt(-1).visuals.isEmpty())
    }

    @Test fun runtimeKeyframesUseSourceTimeAndPreserveSplitAndTrimMeaning() {
        val animated = clip("animated").copy(speed = 2f, properties = listOf(AnimatedProperty("opacity", .7f,
            listOf(Keyframe(750_000, .2f, Interpolation.HOLD), Keyframe(1_000_000, .4f), Keyframe(1_500_000, 1f)))))
        RenderProperties.validate(animated)
        fun alpha(t: Long) = RenderProperties.transformAt(animated, t).opacity
        assertEquals(.2f, alpha(0), .0001f)
        assertEquals(.2f, alpha(125_000), .0001f)
        assertEquals(.2f, alpha(200_000), .0001f)
        assertEquals(.4f, alpha(250_000), .0001f)
        assertEquals(.7f, alpha(375_000), .0001f)
        assertEquals(1f, alpha(900_000), .0001f)
        val p = project(Track(name = "v", type = TrackType.VIDEO, clips = listOf(animated)))
        val split = RenderPlan.derive(SplitClip(animated.id, 375_000, "right").apply(p))
        assertEquals(.7f, split.frameAt(375_000).visuals.single().transform.opacity, .0001f)
        val trimmed = TrimClip(animated.id, TimeRange(1_000_000, 1_500_000), 250_000).apply(p)
        assertEquals(.7f, RenderPlan.derive(trimmed).frameAt(375_000).visuals.single().transform.opacity, .0001f)
    }

    @Test fun runtimeBindsAllTransformPropertiesAndRejectsAmbiguousOrInvalidBindings() {
        val c = clip("c").copy(properties = listOf(AnimatedProperty("translationX", .3f), AnimatedProperty("translationY", -.2f),
            AnimatedProperty("scaleX", 2f), AnimatedProperty("scaleY", .5f), AnimatedProperty("rotationDegrees", 45f)))
        RenderProperties.validate(c)
        assertEquals(Transform(2f, .5f, 45f, .3f, -.2f), RenderProperties.transformAt(c, 0))
        listOf(listOf(AnimatedProperty("opacity", 2f)), listOf(AnimatedProperty("scaleX", 0f)),
            listOf(AnimatedProperty("unknown", 1f)), listOf(AnimatedProperty("opacity", 1f), AnimatedProperty("opacity", 0f)))
            .forEach { properties -> assertThrows(RenderCapabilityException::class.java) { RenderProperties.validate(c.copy(properties = properties)) } }
    }

    @Test fun interpolationDoesNotOverflowBetweenFiniteExtremeValues() {
        val property = AnimatedProperty("translationX", 0f, listOf(Keyframe(0, -Float.MAX_VALUE), Keyframe(100, Float.MAX_VALUE)))
        assertEquals(0f, property.valueAt(50), 0f)
        assertTrue(property.valueAt(75).isFinite())
    }

    @Test fun effectsRoundTripAndHistoryPreservesOrderEnabledStateAndSplit() {
        val c = clip("c")
        val history = EditorHistory(project(Track(name = "v", type = TrackType.VIDEO, clips = listOf(c))))
        val chain = listOf(EffectSpec(type = "contrast", parameters = mapOf("amount" to .2f)),
            EffectSpec(type = "brightness", parameters = mapOf("amount" to -.1f)), EffectSpec(type = "saturation", enabled = false))
        history.execute(UpdateClip(c.id, effects = chain))
        val reloaded = Json.decodeFromString<Project>(Json.encodeToString(history.project)).upgradeSchema()
        assertEquals(chain, reloaded.findClip(c.id)!!.effects)
        history.undo(); assertTrue(history.project.findClip(c.id)!!.effects.isEmpty())
        history.redo(); assertEquals(chain, history.project.findClip(c.id)!!.effects)
        history.execute(SplitClip(c.id, 1_000_000, "split"))
        assertEquals(chain, history.project.findClip("split")!!.effects)
    }

    @Test fun effectRangesRejectUnknownNonfiniteAndOutOfRangeParameters() {
        ColorAdjustments.all.forEach { definition ->
            assertEquals(0f, definition.amount(EffectSpec(type = definition.type)))
            listOf(Float.NaN, Float.POSITIVE_INFINITY, definition.range.endInclusive + 1f).forEach { value ->
                assertThrows(RenderCapabilityException::class.java) { definition.amount(EffectSpec(type = definition.type, parameters = mapOf("amount" to value))) }
            }
            assertThrows(RenderCapabilityException::class.java) { definition.amount(EffectSpec(type = definition.type, parameters = mapOf("typo" to 0f))) }
        }
    }

    @Test fun transitionSchedulingRequiresTwoExplicitOverlappingVisualInputs() {
        val a = clip("a")
        val b = clip("b", 1_000_000)
        val p = project(Track(name = "a", type = TrackType.VIDEO, clips = listOf(a)), Track(name = "b", type = TrackType.OVERLAY, clips = listOf(b)))
        val spec = TransitionSpec(type = "cross_dissolve", durationUs = 1_000_000, fromClipId = "a", toClipId = "b")
        val window = TransitionWindow.schedule(p, spec)
        assertEquals(0f, window.progressAt(0)); assertEquals(.5f, window.progressAt(1_500_000)); assertEquals(1f, window.progressAt(3_000_000))
        assertThrows(IllegalArgumentException::class.java) { TransitionWindow.schedule(p, spec.copy(durationUs = 2_000_000)) }
        assertThrows(IllegalArgumentException::class.java) { TransitionWindow.schedule(p, spec.copy(toClipId = "a")) }
    }
}
