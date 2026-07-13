package com.beatsandbeyond.video_editor.feature.projectdetail

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.beatsandbeyond.video_editor.core.domain.model.*

/**
 * Instrumented UI test for verifying timeline lane interactions and position calculations.
 */
@RunWith(AndroidJUnit4::class)
class TimelineInteractionTest {
    
    @Test
    fun testLanePositionResolution() {
        val timeline = Timeline("t1", listOf(
            Track("v", TrackType.VIDEO, listOf(Clip("c1", "a1", 0L, 0L, 1000L))),
            Track("a", TrackType.AUDIO, listOf(Clip("c2", "a2", 1000L, 0L, 2000L)))
        ))
        assertEquals(TrackType.VIDEO, timeline.trackTypeOf("c1"))
        assertEquals(TrackType.AUDIO, timeline.trackTypeOf("c2"))
    }
}
