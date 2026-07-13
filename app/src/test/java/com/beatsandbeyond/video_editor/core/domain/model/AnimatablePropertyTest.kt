package com.beatsandbeyond.video_editor.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying keyframe interpolation and animatable property evaluation.
 */
class AnimatablePropertyTest {

    @Test
    fun testDefaultValueWhenNoKeyframes() {
        val property = AnimatableProperty(PropertyType.SCALE)
        assertEquals(1.0f, property.getValueAt(100L), 0.001f)
    }

    @Test
    fun testEvaluationBeforeFirstKeyframe() {
        val keyframes = listOf(
            Keyframe(100L, 2.0f, InterpolationType.LINEAR),
            Keyframe(200L, 4.0f, InterpolationType.LINEAR)
        )
        val property = AnimatableProperty(PropertyType.SCALE, keyframes)
        assertEquals(2.0f, property.getValueAt(50L), 0.001f)
    }

    @Test
    fun testEvaluationAfterLastKeyframe() {
        val keyframes = listOf(
            Keyframe(100L, 2.0f, InterpolationType.LINEAR),
            Keyframe(200L, 4.0f, InterpolationType.LINEAR)
        )
        val property = AnimatableProperty(PropertyType.SCALE, keyframes)
        assertEquals(4.0f, property.getValueAt(250L), 0.001f)
    }

    @Test
    fun testLinearInterpolation() {
        val keyframes = listOf(
            Keyframe(100L, 2.0f, InterpolationType.LINEAR),
            Keyframe(200L, 4.0f, InterpolationType.LINEAR)
        )
        val property = AnimatableProperty(PropertyType.SCALE, keyframes)
        assertEquals(3.0f, property.getValueAt(150L), 0.001f)
    }

    @Test
    fun testEaseInInterpolation() {
        val keyframes = listOf(
            Keyframe(0L, 0.0f, InterpolationType.EASE_IN),
            Keyframe(100L, 1.0f, InterpolationType.LINEAR)
        )
        val property = AnimatableProperty(PropertyType.OPACITY, keyframes)
        // At progress = 0.5, easeIn(t) = t^2 = 0.25
        assertEquals(0.25f, property.getValueAt(50L), 0.001f)
    }
}
