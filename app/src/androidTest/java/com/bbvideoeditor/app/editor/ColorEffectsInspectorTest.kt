package com.bbvideoeditor.app.editor

import androidx.compose.ui.semantics.SemanticsActions
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
class ColorEffectsInspectorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inspectorAppliesRealOrderedEffectsAsOneUndoStepAndCancelPreservesProject() {
        val media = MediaReference(uri = "content://test/image", displayName = "Color test", kind = MediaKind.IMAGE, durationUs = 0)
        val clip = Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 2_000_000))
        val editor = EditorViewModel(Project(media = mapOf(media.id to media), timeline = Timeline(listOf(Track(name = "v", type = TrackType.VIDEO, clips = listOf(clip))))))
        compose.setContent { EditorTheme { EditorScreen(editor, onBack = {}, onImport = {}) } }
        compose.runOnIdle { editor.select(clip.id) }
        compose.onNodeWithText("Color").performScrollTo().performClick()
        compose.onNodeWithText("Add Brightness").performClick()
        compose.onNodeWithTag("effect-amount-0").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(.4f) }
        compose.onNodeWithText("Apply effects").performClick()
        compose.runOnIdle {
            val effect = editor.committedProject.findClip(clip.id)!!.effects.single()
            assertEquals("brightness", effect.type)
            assertEquals(.4f, effect.parameters["amount"]!!, .0001f)
            editor.undo(); assertTrue(editor.committedProject.findClip(clip.id)!!.effects.isEmpty()); assertFalse(editor.state.value.canUndo)
            editor.redo(); assertEquals(effect, editor.committedProject.findClip(clip.id)!!.effects.single())
        }
        compose.onNodeWithText("Color").performScrollTo().performClick()
        compose.onNodeWithText("Add Contrast").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(1, editor.committedProject.findClip(clip.id)!!.effects.size) }
    }
}
