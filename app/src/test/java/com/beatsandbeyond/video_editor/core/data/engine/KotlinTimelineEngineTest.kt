package com.beatsandbeyond.video_editor.core.data.engine

import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.Transition
import com.beatsandbeyond.video_editor.core.domain.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class KotlinTimelineEngineTest {

    private lateinit var engine: KotlinTimelineEngine
    private lateinit var timeline: Timeline
    private val trackId = "track_1"

    @Before
    fun setUp() {
        engine = KotlinTimelineEngine()

        val clip1 = Clip(
            id = "clip_1",
            assetId = "asset_1",
            timelinePositionMs = 0L,
            trimStartMs = 0L,
            trimEndMs = 5000L // duration = 5000
        )
        val clip2 = Clip(
            id = "clip_2",
            assetId = "asset_2",
            timelinePositionMs = 6000L,
            trimStartMs = 0L,
            trimEndMs = 4000L // duration = 4000, ends at 10000
        )
        val track = Track(
            id = trackId,
            type = TrackType.VIDEO,
            clips = listOf(clip1, clip2)
        )
        timeline = Timeline(
            id = "timeline_1",
            tracks = listOf(track)
        )
    }

    @Test
    fun `addClip with no overlap succeeds`() {
        val newClip = Clip(
            id = "clip_3",
            assetId = "asset_3",
            timelinePositionMs = 11000L,
            trimStartMs = 0L,
            trimEndMs = 2000L
        )
        val result = engine.addClip(timeline, trackId, newClip)
        assertNotNull(result)
        assertEquals(3, result!!.tracks.first().clips.size)
    }

    @Test
    fun `addClip overlapping existing clip is rejected`() {
        val overlappingClip = Clip(
            id = "clip_3",
            assetId = "asset_3",
            timelinePositionMs = 4000L, // overlaps clip_1 (0-5000)
            trimStartMs = 0L,
            trimEndMs = 2000L
        )
        val result = engine.addClip(timeline, trackId, overlappingClip)
        assertNull(result)
    }

    @Test
    fun `moveClip to free slot succeeds`() {
        val result = engine.moveClip(timeline, "clip_2", 12000L)
        assertNotNull(result)
        val moved = result!!.tracks.first().clips.find { it.id == "clip_2" }
        assertEquals(12000L, moved?.timelinePositionMs)
    }

    @Test
    fun `moveClip causing overlap is rejected`() {
        val result = engine.moveClip(timeline, "clip_2", 4000L) // overlaps clip_1 (0-5000)
        assertNull(result)
    }

    @Test
    fun `moveClips with no overlap succeeds`() {
        val result = engine.moveClips(timeline, setOf("clip_1", "clip_2"), 10000L)
        assertNotNull(result)
        val clips = result!!.tracks.first().clips
        assertEquals(10000L, clips.find { it.id == "clip_1" }?.timelinePositionMs)
        assertEquals(16000L, clips.find { it.id == "clip_2" }?.timelinePositionMs)
    }

    @Test
    fun `moveClips causing overlap with stationary clip is rejected`() {
        val result = engine.moveClips(timeline, setOf("clip_2"), -2000L) // lands clip_2 at 4000, overlaps clip_1 (0-5000)
        assertNull(result)
    }

    @Test
    fun `addTransition with valid adjacent clips succeeds`() {
        val transition = Transition(
            id = "trans_1",
            type = TransitionType.CROSSFADE,
            durationMs = 1000L,
            fromClipId = "clip_1",
            toClipId = "clip_2"
        )
        val result = engine.addTransition(timeline, transition)
        assertEquals(1, result.transitions.size)
        assertEquals("trans_1", result.transitions.first().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addTransition with non-adjacent clips throws exception`() {
        // Create a non-adjacent setup by adding a 3rd clip in between them
        val clip1 = Clip("c1", "a1", 0L, 0L, 2000L)
        val clip2 = Clip("c2", "a2", 3000L, 0L, 2000L)
        val clip3 = Clip("c3", "a3", 6000L, 0L, 2000L)
        val track = Track("track_1", TrackType.VIDEO, listOf(clip1, clip2, clip3))
        val testTimeline = Timeline("t1", listOf(track))

        val transition = Transition("t_err", TransitionType.CROSSFADE, 1000L, "c1", "c3")
        engine.addTransition(testTimeline, transition)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addTransition with duration exceeding clip duration throws exception`() {
        val transition = Transition("t_err", TransitionType.CROSSFADE, 6000L, "clip_1", "clip_2") // clip_2 duration is 4000
        engine.addTransition(timeline, transition)
    }

    @Test
    fun `removeTransition removes transition by ID`() {
        val transition = Transition("trans_1", TransitionType.CROSSFADE, 1000L, "clip_1", "clip_2")
        val withTrans = engine.addTransition(timeline, transition)
        assertEquals(1, withTrans.transitions.size)

        val removed = engine.removeTransition(withTrans, "trans_1")
        assertEquals(0, removed.transitions.size)
    }
}
