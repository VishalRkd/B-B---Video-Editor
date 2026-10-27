package com.bbvideoeditor.app.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.projects.ProjectRepository
import com.bbvideoeditor.app.render.TextBitmapCache
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captionToolSavesRealTextTimingAndStyleAndReopensAfterUndoRedo() {
        val editor = EditorViewModel(Project())
        val cache = TextBitmapCache()
        compose.setContent { EditorTheme { EditorScreen(editor, onBack = {}, onImport = {}, textBitmaps = cache,
            preview = { TextCanvasLayer(editor, cache, it) }) } }
        compose.onNodeWithText("Caption", substring = false).performScrollTo().performClick()
        compose.onNodeWithTag("text-input").performTextInput("A real caption")
        compose.onNodeWithTag("text-start").performScrollTo().performTextReplacement("0.5")
        compose.onNodeWithTag("text-duration").performTextReplacement("1.5")
        compose.onNodeWithText("Bold", substring = false).performScrollTo().performClick()
        compose.waitUntil(5_000) { isEnabled().matches(compose.onNodeWithTag("save-text").fetchSemanticsNode()) }
        compose.onNodeWithTag("save-text").performClick()
        lateinit var saved: Project
        compose.runOnIdle {
            saved = editor.state.value.project
            val clip = saved.timeline.clips.single()
            assertEquals("A real caption", clip.text!!.text)
            assertTrue(clip.text!!.bold)
            assertEquals(500_000L, clip.startUs)
            assertEquals(1_500_000L, clip.durationUs)
            assertEquals(-.72f, clip.transform.translationY, 0f)
            assertEquals(500_000L, editor.state.value.positionUs)
            assertTrue(saved.media.isEmpty())
        }
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.runOnIdle { assertTrue(editor.state.value.project.timeline.clips.isEmpty()) }
        compose.onNodeWithContentDescription("Redo").performClick()
        compose.runOnIdle { assertEquals(saved.timeline, editor.state.value.project.timeline) }
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val repository = ProjectRepository(context)
            try {
                repository.save(editor.committedProject)
                assertEquals(editor.committedProject, ProjectRepository(context).load(saved.id))
            } finally { repository.delete(saved.id) }
        }
    }

    @Test fun canvasMovesUnderFingerAndPinchRotateCommitOneUndoStepEach() {
        val title = Clip(startUs = 0, sourceRange = TimeRange(0, 3_000_000), text = TextContent("DRAG", fontSize = 180f))
        val project = AddTextClip(title).apply(Project())
        val editor = EditorViewModel(project)
        val cache = TextBitmapCache()
        cache.get(title.text!!, project.canvas, 1080) // Prewarm on instrumentation thread.
        compose.setContent { EditorTheme { Box(Modifier.size(320.dp, 180.dp).background(Color.Black)) {
            TextCanvasLayer(editor, cache, Modifier.fillMaxSize())
        } } }
        val canvas = compose.onNodeWithTag("text-canvas")
        compose.waitForIdle()
        // Separate injection calls intentionally keep the finger down while inspecting state.
        canvas.performTouchInput { down(center); moveBy(Offset(80f, 20f), 150) }
        compose.runOnIdle {
            assertTrue("Text did not follow the finger", editor.state.value.project.findClip(title.id)!!.transform.translationX > 0)
            assertEquals(title.transform, editor.committedProject.findClip(title.id)!!.transform)
        }
        canvas.performTouchInput { up() }
        compose.runOnIdle {
            assertTrue(editor.committedProject.findClip(title.id)!!.transform.translationX > 0)
            editor.undo()
            assertEquals(title, editor.state.value.project.findClip(title.id))
            assertFalse(editor.state.value.canUndo)
        }
        canvas.performTouchInput {
            down(0, center - Offset(30f, 0f)); down(1, center + Offset(30f, 0f))
            moveTo(0, center - Offset(55f, 25f), delayMillis = 100)
            moveTo(1, center + Offset(55f, 25f), delayMillis = 100)
            up(0); up(1)
        }
        compose.runOnIdle {
            val transformed = editor.committedProject.findClip(title.id)!!.transform
            assertTrue("Pinch did not resize text: $transformed", transformed.scaleX > 1.5f)
            assertTrue("Two-finger rotation was not applied: $transformed", kotlin.math.abs(transformed.rotationDegrees) > 15f)
            editor.undo()
            assertEquals(title, editor.state.value.project.findClip(title.id))
            assertFalse(editor.state.value.canUndo)
        }
    }

    @Test fun lockedTextCanBeSelectedButCannotBeMoved() {
        val title = Clip(startUs = 0, sourceRange = TimeRange(0, 3_000_000), text = TextContent("LOCKED", fontSize = 180f))
        val unlocked = AddTextClip(title).apply(Project())
        val editor = EditorViewModel(SetTrackLocked(unlocked.findTrackForClip(title.id)!!.id, true).apply(unlocked))
        val cache = TextBitmapCache()
        cache.get(title.text!!, unlocked.canvas, 1080)
        compose.setContent { EditorTheme { Box(Modifier.size(320.dp, 180.dp)) { TextCanvasLayer(editor, cache, Modifier.fillMaxSize()) } } }
        compose.onNodeWithTag("text-canvas").performTouchInput { swipe(center, center + Offset(120f, 20f), 300) }
        compose.runOnIdle {
            assertEquals(title.id, editor.state.value.selectedClipId)
            assertEquals(title, editor.state.value.project.findClip(title.id))
            assertFalse(editor.state.value.canUndo)
        }
    }

    @Test fun leftTrimExtendsNativeTextIntoEmptyTimelineSpace() {
        val clip = Clip(startUs = 2_000_000, sourceRange = TimeRange(0, 3_000_000), text = TextContent("Extend caption"))
        val editor = EditorViewModel(AddTextClip(clip).apply(Project()))
        compose.setContent { EditorTheme { EditorScreen(editor, onBack = {}, onImport = {}) } }
        compose.runOnIdle { editor.select(clip.id) }
        compose.onNodeWithContentDescription("Trim start", useUnmergedTree = true).performTouchInput {
            down(center); moveBy(Offset(-90f, 0f), 150); moveBy(Offset(-20f, 0f), 100); up()
        }
        compose.runOnIdle {
            val extended = editor.committedProject.findClip(clip.id)!!
            assertTrue("Text was constrained by a nonexistent source boundary", extended.startUs < clip.startUs)
            assertEquals(clip.endUs, extended.endUs)
            assertEquals(clip.text, extended.text)
            editor.undo()
            assertEquals(clip, editor.state.value.project.findClip(clip.id))
            assertFalse(editor.state.value.canUndo)
        }
    }
}
