package com.bbvideoeditor.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class EditorCommandsTest {
    private val media = MediaReference(id = "source", uri = "content://test/video", displayName = "Source", kind = MediaKind.VIDEO,
        durationUs = 120_000_000, width = 1920, height = 1080, hasAudio = true)

    private fun clip(id: String = "a", start: Long = 0, source: Long = 0, duration: Long = 10_000_000, speed: Float = 1f) =
        Clip(id = id, mediaId = media.id, startUs = start, sourceRange = TimeRange(source, duration), speed = speed)

    private fun project(vararg clips: Clip): Project = Project(id = "project", name = "Test", createdAt = 1,
        timeline = Timeline(listOf(Track(id = "video", name = "Video", type = TrackType.VIDEO, clips = clips.toList()),
            Track(id = "overlay", name = "Overlay", type = TrackType.OVERLAY),
            Track(id = "audio", name = "Audio", type = TrackType.AUDIO))), media = mapOf(media.id to media)).validate()

    @Test fun splitPreservesTrimSpeedAndOriginalMedia() {
        val original = clip(source = 20_000_000, duration = 30_000_000, speed = 2f)
        val before = project(original)
        val after = SplitClip("a", 5_000_000, "b").apply(before)
        val left = after.findClip("a")!!
        val right = after.findClip("b")!!
        assertEquals(TimeRange(20_000_000, 10_000_000), left.sourceRange)
        assertEquals(TimeRange(30_000_000, 20_000_000), right.sourceRange)
        assertEquals(left.endUs, right.startUs)
        assertEquals(original.endUs, right.endUs)
        assertEquals(2f, right.speed)
        assertSame(before.media[media.id], after.media[media.id])
        assertEquals(original, before.findClip("a"))
    }

    @Test fun fractionalSpeedSplitKeepsExactTimelineBoundaryAndSourcePartition() {
        val random = Random(2197)
        repeat(1000) {
            val speed = listOf(0.1f, 0.25f, 0.75f, 1.1f, 1.5f, 2.75f, 8f).random(random)
            val original = clip(duration = random.nextLong(1_000_000, 50_000_000), speed = speed)
            val at = random.nextLong(1000, original.durationUs - 1000)
            val after = SplitClip("a", at, "b").apply(project(original))
            val left = after.findClip("a")!!
            val right = after.findClip("b")!!
            assertEquals(at, left.endUs)
            assertEquals(at, right.startUs)
            assertEquals(original.endUs, right.endUs)
            assertEquals(left.sourceRange.endUs, right.sourceRange.startUs)
            assertEquals(original.sourceRange.durationUs, left.sourceRange.durationUs + right.sourceRange.durationUs)
            assertTrue(abs(left.durationUs - left.sourceRange.durationUs / speed.toDouble()) <= 11.0)
        }
    }

    @Test fun repeatedFractionalSplitsDoNotAccumulateTimelineDrift() {
        var current = project(clip(duration = 50_000_000, speed = 0.75f))
        val end = current.durationUs
        var rightId = "a"
        repeat(50) { index ->
            val nextId = "part-$index"
            current = SplitClip(rightId, (index + 1L) * 1_000_001L, nextId).apply(current)
            rightId = nextId
        }
        assertEquals(end, current.durationUs)
        val clips = current.findTrack("video")!!.clips
        clips.zipWithNext().forEach { (left, right) ->
            assertEquals(left.endUs, right.startUs)
            assertEquals(left.sourceRange.endUs, right.sourceRange.startUs)
        }
    }

    @Test fun splitAtBoundaryFailsWithoutChangingHistory() {
        val history = EditorHistory(project(clip()))
        assertThrows(IllegalArgumentException::class.java) { history.execute(SplitClip("a", 0)) }
        assertThrows(IllegalArgumentException::class.java) { history.execute(SplitClip("a", 10_000_000)) }
        assertFalse(history.canUndo)
        assertEquals(1, history.project.timeline.clips.size)
    }

    @Test fun undoRedoRoundTripAndNewEditInvalidatesRedo() {
        val history = EditorHistory(project(clip()))
        history.execute(SplitClip("a", 4_000_000, "b"))
        history.execute(DeleteClip("a"))
        assertEquals(listOf("b"), history.project.timeline.clips.map { it.id })
        assertTrue(history.undo())
        assertEquals(listOf("a", "b"), history.project.timeline.clips.map { it.id })
        assertTrue(history.undo())
        assertEquals(10_000_000L, history.project.findClip("a")!!.durationUs)
        assertTrue(history.redo())
        assertEquals(4_000_000L, history.project.findClip("a")!!.durationUs)
        assertTrue(history.canRedo)
        history.execute(UpdateClip("a", volume = 0.3f))
        assertFalse(history.canRedo)
        assertFalse(history.redo())
    }

    @Test fun failedEditPreservesRedoAndProject() {
        val history = EditorHistory(project(clip(), clip("b", 10_000_000)))
        history.execute(DeleteClip("b"))
        history.undo()
        val before = history.project
        assertThrows(IllegalArgumentException::class.java) { history.execute(MoveClip("a", "video", 5_000_000)) }
        assertSame(before, history.project)
        assertTrue(history.canRedo)
    }

    @Test fun trimChecksSourceBoundsAndDoesNotDestroyOriginal() {
        val original = project(clip(source = 5_000_000))
        val after = TrimClip("a", TimeRange(7_000_000, 6_000_000), 2_000_000).apply(original)
        assertEquals(2_000_000L, after.findClip("a")!!.startUs)
        assertEquals(TimeRange(7_000_000, 6_000_000), after.findClip("a")!!.sourceRange)
        assertEquals(TimeRange(5_000_000, 10_000_000), original.findClip("a")!!.sourceRange)
        assertThrows(IllegalArgumentException::class.java) { TrimClip("a", TimeRange(119_000_000, 5_000_000), 0).apply(after) }
    }

    @Test fun moveAcrossTracksPreservesSourceAndRejectsOverlap() {
        val original = project(clip(), clip("b", 12_000_000))
        val after = MoveClip("a", "overlay", 2_000_000).apply(original)
        assertEquals("overlay", after.findTrackForClip("a")!!.id)
        assertEquals(original.findClip("a")!!.sourceRange, after.findClip("a")!!.sourceRange)
        assertEquals(listOf("b"), after.findTrack("video")!!.clips.map { it.id })
        assertThrows(IllegalArgumentException::class.java) { MoveClip("a", "video", 5_000_000).apply(original) }
    }

    @Test fun duplicateRipplesFollowingClipsAndKeepsGap() {
        val after = DuplicateClip("a", "copy").apply(project(clip(), clip("b", 12_000_000)))
        assertEquals(10_000_000L, after.findClip("copy")!!.startUs)
        assertEquals(22_000_000L, after.findClip("b")!!.startUs)
        assertEquals(after.findClip("a")!!.sourceRange, after.findClip("copy")!!.sourceRange)
    }

    @Test fun reorderClosesGapsAndPreservesInitialOffset() {
        val after = ReorderClip("c", 0).apply(project(clip(start = 2_000_000), clip("b", 14_000_000), clip("c", 30_000_000)))
        val clips = after.findTrack("video")!!.clips
        assertEquals(listOf("c", "a", "b"), clips.map { it.id })
        assertEquals(listOf(2_000_000L, 12_000_000L, 22_000_000L), clips.map { it.startUs })
    }

    @Test fun speedChangeRipplesTrackAndMatchesMedia3Flooring() {
        val after = UpdateClip("a", speed = 1.5f).apply(project(clip(duration = 10_000_001), clip("b", 12_000_001)))
        assertEquals(6_666_667L, after.findClip("a")!!.durationUs)
        assertEquals(8_666_667L, after.findClip("b")!!.startUs)
        assertEquals(10_000_001L, after.findClip("a")!!.sourceRange.durationUs)
    }

    @Test fun lockedTrackRejectsEditingButCanBeUnlocked() {
        val locked = SetTrackLocked("video", true).apply(project(clip()))
        assertThrows(IllegalArgumentException::class.java) { DeleteClip("a").apply(locked) }
        assertThrows(IllegalArgumentException::class.java) { MoveClip("a", "overlay", 0).apply(locked) }
        assertTrue(DeleteClip("a").apply(SetTrackLocked("video", false).apply(locked)).timeline.clips.isEmpty())
    }

    @Test fun incompatibleMediaIsRejectedAndImagesCanHaveUserDefinedDuration() {
        val audio = media.copy(id = "music", kind = MediaKind.AUDIO, uri = "content://test/audio")
        val audioClip = clip().copy(mediaId = audio.id)
        assertThrows(IllegalArgumentException::class.java) { AddClips("video", listOf(audioClip), listOf(audio)).apply(project()) }
        val image = media.copy(id = "image", kind = MediaKind.IMAGE, durationUs = 0, uri = "content://test/image")
        val imageClip = clip().copy(mediaId = image.id)
        val after = AddClips("overlay", listOf(imageClip), listOf(image)).apply(project())
        assertEquals(10_000_000L, after.durationUs)
    }

    @Test fun sourceTimestampMappingIncludesTrimAndSpeed() {
        val original = clip(start = 2_000_000, source = 10_000_000, duration = 20_000_000, speed = 2f)
        assertEquals(16_000_000L, original.sourceTimeAt(5_000_000))
        assertEquals(5_000_000L, original.timelineTimeAt(16_000_000))
        assertEquals(original.sourceRange.startUs, original.sourceTimeAt(-1))
        assertEquals(original.sourceRange.endUs, original.sourceTimeAt(Long.MAX_VALUE))
    }

    @Test fun historyCapacityBoundsRetentionAndPreviewIsPure() {
        val history = EditorHistory(project(clip()), capacity = 3)
        val preview = MoveClip("a", "video", 2_000_000).apply(history.project)
        assertEquals(2_000_000L, preview.findClip("a")!!.startUs)
        assertFalse(history.canUndo)
        assertEquals(0L, history.project.findClip("a")!!.startUs)
        repeat(10) { history.execute(RenameProject("Name $it")) }
        repeat(3) { assertTrue(history.undo()) }
        assertFalse(history.undo())
        assertEquals("Name 6", history.project.name)
    }

    @Test fun serializationPreservesFullNonDestructiveModel() {
        val effect = EffectSpec(type = "future.lut", parameters = mapOf("intensity" to 0.5f))
        val original = project(clip().copy(
            transform = Transform(scaleX = 0.5f, scaleY = 0.5f, translationX = 0.25f), effects = listOf(effect),
            properties = listOf(AnimatedProperty("opacity", 1f, listOf(Keyframe(0, 0f), Keyframe(10_000_000, 1f)))),
        ))
        val json = Json { encodeDefaults = true }
        val restored = json.decodeFromString<Project>(json.encodeToString(original)).validate()
        assertEquals(original, restored)
        assertEquals(0.5f, restored.findClip("a")!!.properties.single().valueAt(5_000_000))
    }

    @Test fun malformedProjectAndUnsupportedSchemaAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { project(clip(), clip("b", 1)) }
        assertThrows(IllegalArgumentException::class.java) { project(clip(), clip()).validate() }
        assertThrows(IllegalArgumentException::class.java) { project().copy(schemaVersion = 99).validate() }
        assertThrows(IllegalArgumentException::class.java) { project(clip()).copy(media = emptyMap()).validate() }
        assertThrows(IllegalArgumentException::class.java) { clip(speed = Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { TimeRange(Long.MAX_VALUE - 1, 2) }
    }
}
