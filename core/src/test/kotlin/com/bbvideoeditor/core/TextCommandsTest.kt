package com.bbvideoeditor.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TextCommandsTest {
    private fun title(id: String = "title", start: Long = 0, duration: Long = 3_000_000) =
        Clip(id = id, startUs = start, sourceRange = TimeRange(0, duration), text = TextContent("Hello\nनमस्ते"))

    @Test fun creatingTextIsOneUndoableActionWithoutSyntheticMedia() {
        val before = Project()
        val history = EditorHistory(before)
        history.execute(AddTextClip(title()))
        assertTrue(history.project.media.isEmpty())
        assertEquals(TrackType.TEXT, history.project.findTrackForClip("title")!!.type)
        assertEquals(3_000_000L, history.project.durationUs)
        assertTrue(history.undo())
        assertEquals(before, history.project.copy(updatedAt = before.updatedAt))
        assertFalse(history.canUndo)
        assertTrue(history.redo())
        assertEquals(title(), history.project.findClip("title"))
    }

    @Test fun overlappingTitlesCreateLayersWhileAdjacentCaptionsReuseUnlockedTracks() {
        val first = AddTextClip(title()).apply(Project())
        val adjacent = AddTextClip(title("caption2", 3_000_000)).apply(first)
        assertEquals(1, adjacent.timeline.tracks.count { it.type == TrackType.TEXT })
        val layer = AddTextClip(title("overlay", 1_000_000)).apply(adjacent)
        assertEquals(2, layer.timeline.tracks.count { it.type == TrackType.TEXT })
        val locked = SetTrackLocked(first.findTrackForClip("title")!!.id, true).apply(first)
        assertEquals(2, AddTextClip(title("after", 4_000_000)).apply(locked).timeline.tracks.count { it.type == TrackType.TEXT })
    }

    @Test fun trimSplitDuplicateReorderAndDeletePreserveTypography() {
        val first = AddTextClip(title()).apply(Project())
        val trimmed = TrimClip("title", TimeRange(500_000, 2_000_000), 500_000).apply(first)
        val split = SplitClip("title", 1_500_000, "right").apply(trimmed)
        assertEquals(1_500_000L, split.findClip("title")!!.endUs)
        assertEquals(2_500_000L, split.findClip("right")!!.endUs)
        assertEquals(title().text, split.findClip("right")!!.text)
        val duplicate = DuplicateClip("right", "copy").apply(split)
        assertEquals(3_500_000L, duplicate.findClip("copy")!!.endUs)
        val reordered = ReorderClip("copy", 0).apply(duplicate)
        assertEquals("copy", reordered.findTrackForClip("copy")!!.clips.first().id)
        val deleted = DeleteClip("right").apply(reordered)
        assertNull(deleted.findClip("right"))
        assertEquals(title().text, deleted.findClip("copy")!!.text)
    }

    @Test fun editingTimingAndStylePreservesTransformAndRejectsCollisionsAtomically() {
        val first = AddTextClip(title().copy(transform = Transform(translationY = -.7f, rotationDegrees = 15f))).apply(Project())
        val project = AddTextClip(title("next", 3_000_000)).apply(first)
        val history = EditorHistory(project)
        val style = TextContent("Edited", font = TextFont.SERIF, bold = true, outlineWidth = 2f)
        assertThrows(IllegalArgumentException::class.java) { history.execute(UpdateTextClip("title", style, 0, 4_000_000)) }
        assertEquals(project, history.project)
        assertFalse(history.canUndo)
        history.execute(UpdateTextClip("title", style, 250_000, 2_000_000))
        assertEquals(style, history.project.findClip("title")!!.text)
        assertEquals(first.findClip("title")!!.transform, history.project.findClip("title")!!.transform)
        assertEquals(2_250_000L, history.project.findClip("title")!!.endUs)
        history.undo()
        assertEquals(project, history.project.copy(updatedAt = project.updatedAt))
    }

    @Test fun moveSupportsTextTracksAndHonorsLocksAndTypeSafety() {
        val first = AddTextClip(title()).apply(Project())
        val secondTrack = Track(id = "text2", name = "Second", type = TrackType.TEXT)
        val project = AddTrack(secondTrack).apply(first)
        val moved = MoveClip("title", secondTrack.id, 1_000_000).apply(project)
        assertEquals(secondTrack.id, moved.findTrackForClip("title")!!.id)
        assertThrows(IllegalArgumentException::class.java) { MoveClip("title", project.timeline.tracks.first().id, 0).apply(project) }
        val locked = SetTrackLocked(secondTrack.id, true).apply(moved)
        assertThrows(IllegalArgumentException::class.java) { UpdateTextClip("title", TextContent("No"), 0, 1_000_000).apply(locked) }
    }

    @Test fun persistenceRoundTripsTextAndMigratesLegacyMediaWithoutChangingEdits() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val project = AddTextClip(title()).apply(Project())
        assertEquals(project, json.decodeFromString<Project>(json.encodeToString(project)).upgradeSchema())
        val source = MediaReference(id = "media", uri = "content://video", displayName = "Video", kind = MediaKind.VIDEO, durationUs = 4_000_000)
        val clip = Clip(mediaId = source.id, startUs = 100_000, sourceRange = TimeRange(200_000, 2_000_000), speed = 2f)
        val legacy = Project(schemaVersion = 1, media = mapOf(source.id to source), timeline = Timeline(listOf(Track(name = "Video", type = TrackType.VIDEO, clips = listOf(clip)))))
        val upgraded = json.decodeFromString<Project>(json.encodeToString(legacy)).upgradeSchema()
        assertEquals(CURRENT_PROJECT_SCHEMA, upgraded.schemaVersion)
        assertEquals(clip, upgraded.timeline.clips.single())
        assertEquals(legacy.media, upgraded.media)
    }

    @Test fun invalidContentCannotEnterTheProject() {
        assertThrows(IllegalArgumentException::class.java) { title().copy(mediaId = "also-media") }
        assertThrows(IllegalArgumentException::class.java) { title().copy(text = null) }
        assertThrows(IllegalArgumentException::class.java) { title().copy(speed = 2f) }
        assertThrows(IllegalArgumentException::class.java) { TextContent(" ") }
        assertThrows(IllegalArgumentException::class.java) { TextContent("Text", fontSize = Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { Project(schemaVersion = 99).upgradeSchema() }
    }
}
