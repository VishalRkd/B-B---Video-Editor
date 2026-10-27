package com.bbvideoeditor.app.render

import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.editor.EditorViewModel
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.app.projects.WorkspaceViewModel
import com.bbvideoeditor.core.CanvasSettings
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.DeleteClip
import com.bbvideoeditor.core.EditorHistory
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.TimeRange
import com.bbvideoeditor.core.Timeline
import com.bbvideoeditor.core.Track
import com.bbvideoeditor.core.TrackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PreviewLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun pendingPlayCanBeCancelledBeforeCompositionPreparationFinishes() {
        val project = imageProject("pending-play")
        withEngine { engine ->
            // The main-thread transaction cannot yield to the compiler's debounce/preparation.
            onMain {
                engine.updateProject(project)
                assertTrue("Project compilation should be pending", engine.state.value.buffering)
                assertNull(engine.state.value.player)
                engine.play()
                assertTrue("Pending play must be visible before the decoder is ready", engine.state.value.playWhenReady)
                assertFalse(engine.state.value.playing)
                engine.pause()
                assertFalse("Pause must cancel pending play immediately", engine.state.value.playWhenReady)
            }
            awaitReady(engine)
            assertRemainsPaused(engine, "cancelled pending play")
        }
    }

    @Test fun removingAllClipsClearsPlayIntentAndUndoStaysPaused() {
        val project = imageProject("empty-project-intent")
        val history = EditorHistory(project)
        withEngine { engine ->
            onMain { engine.updateProject(project) }
            awaitReady(engine)
            onMain { engine.play() }
            await("initial playback") {
                assertNull(engine.state.value.error)
                engine.state.value.playing
            }
            onMain {
                assertTrue(history.execute(DeleteClip(project.timeline.clips.single().id)))
                engine.updateProject(history.project)
                assertEquals(0L, history.project.durationUs)
                assertFalse("An empty timeline retained play intent", engine.state.value.playWhenReady)
                assertFalse(engine.state.value.playing)
                assertFalse(engine.state.value.buffering)
                assertNull("An empty timeline retained decoder resources", engine.state.value.player)
                assertEquals(0L, engine.state.value.positionUs)
                assertTrue(history.undo())
                engine.updateProject(history.project)
            }
            awaitReady(engine)
            assertRemainsPaused(engine, "undo after removing the final clip")
        }
    }

    @Test fun pauseImmediatelyAfterAnInClipSeekDoesNotResumeWhenScrubbingEnds() {
        val project = imageProject("seek-pause-intent")
        withEngine { engine ->
            onMain { engine.updateProject(project) }
            awaitReady(engine)
            onMain {
                engine.play()
                assertTrue("Prepared player should receive play intent", engine.state.value.playWhenReady)
                engine.seek(1_234_000)
                engine.pause()
                assertFalse("Pause after seeking must clear play intent", engine.state.value.playWhenReady)
            }
            // Observe beyond the 180 ms scrub timeout plus another 250 ms of stable pause.
            assertRemainsPaused(engine, "pause immediately after seeking during playback", stableForMs = 600)
        }
    }

    @Test fun actualEditorRetainsPausedPlayheadWhenActivityIsRecreated() {
        val app = context.applicationContext as EditorApplication
        val created = runBlocking { app.projects.create("Preview lifecycle recreation") }
        val contents = imageProject("recreation-position")
        val project = created.copy(canvas = contents.canvas, media = contents.media, timeline = contents.timeline)
        runBlocking { app.projects.save(project) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                var workspace: WorkspaceViewModel? = null
                var activity: MainActivity? = null
                try {
                    scenario.onActivity { current ->
                        activity = current
                        workspace = ViewModelProvider(current)[WorkspaceViewModel::class.java]
                        workspace!!.open(project.id)
                    }
                    await("project opened in the real editor") {
                        workspace!!.state.value.editor?.state?.value?.project?.id == project.id
                    }
                    val originalWorkspace = requireNotNull(workspace)
                    val originalEditor = requireNotNull(originalWorkspace.state.value.editor)
                    await("original editor preview ready") {
                        onMain { findPlayerView(activity!!.window.decorView)?.player?.playbackState == Player.STATE_READY }
                    }
                    val targetUs = 1_234_000L
                    onMain { originalEditor.seek(targetUs); originalEditor.endScrub() }
                    awaitEditorPosition(originalEditor, { requireNotNull(activity) }, targetUs)
                    val originalPlayer = onMain { requireNotNull(findPlayerView(activity!!.window.decorView)?.player) }

                    scenario.recreate()
                    scenario.onActivity { current ->
                        activity = current
                        workspace = ViewModelProvider(current)[WorkspaceViewModel::class.java]
                        assertSame("WorkspaceViewModel must survive configuration recreation", originalWorkspace, workspace)
                        assertSame("The editor should be retained by its workspace", originalEditor, workspace!!.state.value.editor)
                    }
                    awaitEditorPosition(originalEditor, { requireNotNull(activity) }, targetUs)
                    onMain {
                        val recreatedPlayer = requireNotNull(findPlayerView(activity!!.window.decorView)?.player)
                        assertNotSame("Recreated activity should own a new preview player", originalPlayer, recreatedPlayer)
                        assertFalse("Activity recreation unexpectedly resumed playback", recreatedPlayer.playWhenReady)
                    }
                    // Catch a late first emission from the fresh engine overwriting the retained time.
                    val stableUntilMs = SystemClock.elapsedRealtime() + 400
                    while (SystemClock.elapsedRealtime() < stableUntilMs) {
                        onMain {
                            assertTrue("Fresh preview reset the editor playhead to ${originalEditor.state.value.positionUs} us",
                                abs(originalEditor.state.value.positionUs - targetUs) <= 2_000)
                            assertFalse("Recreated editor unexpectedly began playing", originalEditor.state.value.playing)
                        }
                        Thread.sleep(20)
                    }
                } finally {
                    // Finish the editor save before deleting this test's repository entry.
                    onMain { workspace?.closeEditor() }?.let { closing -> runBlocking { closing.join() } }
                }
            }
        } finally {
            runBlocking { app.projects.delete(created.id) }
        }
    }

    private fun imageProject(name: String): Project {
        val media = MediaFixtures(context).image(name, Color.RED)
        return Project(canvas = CanvasSettings(320, 180, 30), media = mapOf(media.id to media),
            timeline = Timeline(listOf(Track(name = "Video", type = TrackType.VIDEO, clips = listOf(
                Clip(mediaId = media.id, startUs = 0, sourceRange = TimeRange(0, 3_000_000)),
            )))))
    }

    private fun withEngine(block: (PreviewEngine) -> Unit) {
        val engine = onMain { PreviewEngine(context, CompositionCompiler(context)) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var view: PlayerView? = null
            var binding: Job? = null
            try {
                scenario.onActivity { activity ->
                    view = PlayerView(activity).apply { useController = false }
                    activity.setContentView(view)
                    binding = CoroutineScope(Dispatchers.Main.immediate).launch {
                        engine.state.collect { view!!.player = it.player }
                    }
                }
                block(engine)
            } finally {
                onMain { binding?.cancel(); view?.player = null; engine.close() }
            }
        }
    }

    private fun awaitReady(engine: PreviewEngine) {
        await("preview preparation") {
            assertNull("Preview preparation failed", engine.state.value.error)
            onMain { engine.state.value.player?.playbackState == Player.STATE_READY && !engine.state.value.buffering }
        }
    }

    private fun assertRemainsPaused(engine: PreviewEngine, label: String, stableForMs: Long = 400) {
        val initialPositionUs = engine.state.value.positionUs
        val stableUntilMs = SystemClock.elapsedRealtime() + stableForMs
        while (SystemClock.elapsedRealtime() < stableUntilMs) {
            onMain {
                val state = engine.state.value
                assertNull("$label failed", state.error)
                assertFalse("$label retained transport play intent", state.playWhenReady)
                assertFalse("$label started playback", state.playing)
                assertFalse("$label requested playback from the underlying player", requireNotNull(state.player).playWhenReady)
                assertTrue("$label advanced the paused clock from $initialPositionUs to ${state.positionUs} us",
                    abs(state.positionUs - initialPositionUs) <= 2_000)
            }
            Thread.sleep(20)
        }
    }

    private fun awaitEditorPosition(editor: EditorViewModel, activity: () -> MainActivity, positionUs: Long) {
        await("editor and preview restored to $positionUs us") {
            onMain {
                val player = findPlayerView(activity().window.decorView)?.player
                player?.playbackState == Player.STATE_READY &&
                    abs(player.currentPosition * 1000 - positionUs) <= 2_000 &&
                    abs(editor.state.value.positionUs - positionUs) <= 2_000 && !editor.state.value.playing
            }
        }
    }

    private fun findPlayerView(view: View): PlayerView? {
        if (view is PlayerView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) findPlayerView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun await(label: String, predicate: () -> Boolean) {
        val deadlineMs = SystemClock.elapsedRealtime() + 20_000
        while (!predicate()) {
            if (SystemClock.elapsedRealtime() >= deadlineMs) fail("Timed out waiting for $label")
            Thread.sleep(20)
        }
    }

    private fun <T> onMain(block: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
}
