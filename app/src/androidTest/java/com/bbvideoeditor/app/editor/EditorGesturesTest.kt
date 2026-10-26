package com.bbvideoeditor.app.editor

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorGesturesTest {
    @get:Rule val compose = createComposeRule()

    private fun editor(): EditorViewModel {
        val media = MediaReference(uri = "content://test/video", displayName = "Editing test", kind = MediaKind.VIDEO, durationUs = 20_000_000)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 6_000_000))
        val project = Project(media = mapOf(media.id to media), timeline = Timeline(listOf(
            Track(name = "Video", type = TrackType.VIDEO, clips = listOf(clip)),
            Track(name = "Overlay", type = TrackType.OVERLAY), Track(name = "Audio", type = TrackType.AUDIO),
        )))
        val editor = EditorViewModel(project)
        compose.setContent { EditorTheme { EditorScreen(editor, onBack = {}, onImport = {}) } }
        compose.waitForIdle()
        return editor
    }

    @Test fun visibleToolbarEditsTheRealProjectAndUndoRestoresIt() {
        val editor = editor()
        val clip = editor.state.value.project.timeline.clips.single()
        compose.onNodeWithContentDescription("Clip Editing test", substring = true).performClick()
        compose.runOnIdle { editor.seek(1_000_000); editor.endScrub() }
        compose.onNodeWithText("Split").performClick()
        compose.runOnIdle {
            assertEquals(2, editor.state.value.project.timeline.clips.size)
            assertEquals(1_000_000L, editor.state.value.project.findClip(clip.id)!!.durationUs)
        }
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.runOnIdle { assertEquals(listOf(clip), editor.state.value.project.timeline.clips) }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(editor.state.value.project.timeline.clips.isEmpty()) }
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.runOnIdle { assertEquals(listOf(clip), editor.state.value.project.timeline.clips) }
    }

    @Test fun trimHandleChangesSourceRangeAndCommitsOneUndoStep() {
        val editor = editor()
        val original = editor.state.value.project.timeline.clips.single()
        compose.onNodeWithContentDescription("Clip Editing test", substring = true).performClick()
        compose.onNodeWithContentDescription("Trim start", useUnmergedTree = true).performTouchInput {
            down(center)
            moveBy(Offset(90f, 0f), 120)
            moveBy(Offset(20f, 0f), 120)
            up()
        }
        compose.runOnIdle {
            val trimmed = editor.state.value.project.findClip(original.id)!!
            assertTrue("Trim did not change the source range", trimmed.sourceRange.startUs > 0)
            assertEquals(original.sourceRange.endUs, trimmed.sourceRange.endUs)
            assertEquals(trimmed.sourceRange.startUs, trimmed.startUs)
            editor.undo()
            assertEquals(original, editor.state.value.project.findClip(original.id))
            assertFalse(editor.state.value.canUndo)
        }
    }

    @Test fun horizontalScrubMovesTimeAndLongDragMovesBetweenTracks() {
        val editor = editor()
        val clip = editor.state.value.project.timeline.clips.single()
        compose.runOnIdle { editor.seek(2_000_000); editor.endScrub() }
        val timeline = compose.onNodeWithTag("timeline")
        timeline.performTouchInput { swipe(Offset(width * .7f, 15f), Offset(width * .4f, 15f), 450) }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(editor.state.value.positionUs > 2_000_000); editor.seek(1_000_000); editor.endScrub() }
        // Hold the clip body and move exactly one visual lane down. The gesture owner must survive reparenting.
        val node = compose.onNodeWithContentDescription("Clip Editing test", substring = true)
        node.performTouchInput {
            down(Offset(width * .45f, height * .5f))
            advanceEventTime(650)
            moveBy(Offset(0f, height * 1.25f), 160)
            moveBy(Offset(8f, 0f), 100)
            up()
        }
        compose.runOnIdle {
            assertEquals(TrackType.OVERLAY, editor.state.value.project.findTrackForClip(clip.id)!!.type)
            editor.undo()
            assertEquals(TrackType.VIDEO, editor.state.value.project.findTrackForClip(clip.id)!!.type)
        }
    }
}
