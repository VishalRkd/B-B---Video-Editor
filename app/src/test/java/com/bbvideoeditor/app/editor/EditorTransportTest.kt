package com.bbvideoeditor.app.editor

import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Test

class EditorTransportTest {
    private val media=MediaReference(uri="file:/fixture",displayName="fixture",kind=MediaKind.VIDEO,durationUs=30_000_000)
    private val clip=Clip(mediaId=media.id,startUs=0,sourceRange=TimeRange(0,10_000_000))
    private val project=Project(media=mapOf(media.id to media),timeline=Timeline(listOf(Track(name="V",type=TrackType.VIDEO,clips=listOf(clip)))))

    @Test fun scrubLifecycleNotifiesOnlyEdgesAndReleaseKeepsTheLatestIntent() {
        val editor = EditorViewModel(project)
        val edges = mutableListOf<Boolean>(); var final = -1L
        editor.onScrubbingChanged = { edges += it; if (!it) final = editor.state.value.positionUs }
        repeat(120) { editor.seek(it * 10_000L) }
        editor.endScrub(); editor.endScrub()
        assertEquals(listOf(true, false), edges)
        assertEquals(1_190_000L, final)
        assertSame(project, editor.document.value.project)
    }

    @Test fun transportTicksKeepTheDocumentIdentityAndRevision() {
        val editor=EditorViewModel(project);val document=editor.document.value
        repeat(500){editor.seek(it*10_000L);editor.endScrub();editor.updatePlayback(it*10_000L,true)}
        assertSame(document,editor.document.value)
        assertEquals(4_990_000L,editor.transport.value.positionUs)
        assertFalse(editor.document.value.canUndo)
    }

    @Test fun aHundredGesturePreviewsCreateOneUndoEntryAndCancelCreatesNone() {
        val editor=EditorViewModel(project);var saves=0;editor.onProjectChanged={saves++}
        editor.beginGesture()
        repeat(100){editor.previewGesture(UpdateClip(clip.id,transform=Transform(translationX=it/100f)))}
        assertEquals(project,editor.committedProject);assertEquals(0,saves)
        editor.finishGesture();assertEquals(1,saves)
        assertEquals(.99f,editor.committedProject.findClip(clip.id)!!.transform.translationX,0f)
        editor.undo();assertEquals(project.timeline,editor.committedProject.timeline);assertFalse(editor.document.value.canUndo)
        editor.beginGesture();editor.previewGesture(UpdateClip(clip.id,transform=Transform(scaleX=.5f)));editor.cancelGesture()
        assertEquals(editor.committedProject,editor.document.value.project);assertFalse(editor.document.value.canUndo)
    }
}
