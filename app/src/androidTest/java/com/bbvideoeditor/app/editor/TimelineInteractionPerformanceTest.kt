package com.bbvideoeditor.app.editor

import android.graphics.Bitmap
import android.os.*
import android.view.FrameMetrics
import android.view.MotionEvent
import android.view.Window
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.app.render.MediaFixtures
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.sin

/** Real-time input complements (and never replaces) the unchanged paced 16-case matrix.
 * Events are injected at wall-clock cadence without Compose idling between pointer moves. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class TimelineInteractionPerformanceTest {
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var activity: MainActivity
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var app: EditorApplication
    private lateinit var editor: EditorViewModel
    private lateinit var engine: PreviewEngine
    private lateinit var original: Clip
    private val renderCounters = TimelineRenderCounters()
    private val density get() = app.resources.displayMetrics.density

    @Before fun optIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("timelineInteractivePerformance") == "true")
        app = instrumentation.targetContext.applicationContext as EditorApplication
    }

    private fun open() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity = it }
        val fixtures = MediaFixtures(app)
        val video = fixtures.video("interaction-video", .3f, .5f, .2f)
        val overlay = fixtures.video("interaction-overlay", .2f, .3f, .7f)
        val audio = fixtures.audio()
        val tracks = listOf(TrackType.VIDEO, TrackType.OVERLAY, TrackType.AUDIO, TrackType.TEXT).map { type ->
            Track(name = type.name, type = type, clips = (0 until 100).map { index ->
                Clip(startUs = index * 2_000_000L, sourceRange = TimeRange(if (type == TrackType.TEXT) 0 else 200_000, 1_500_000),
                    mediaId = when (type) { TrackType.VIDEO -> video.id; TrackType.OVERLAY -> overlay.id; TrackType.AUDIO -> audio.id; else -> null },
                    text = if (type == TrackType.TEXT) TextContent("Caption $index") else null,
                    transform = if (type == TrackType.OVERLAY) Transform(scaleX = .35f, scaleY = .35f) else Transform())
            })
        }
        val project = Project(media = listOf(video, overlay, audio).associateBy { it.id }, timeline = Timeline(tracks))
        original = tracks.first().clips[5]
        editor = EditorViewModel(project)
        main { editor.seek(10_500_000); editor.endScrub(); engine = PreviewEngine(app, app.compiler, 10_500_000, renderTextInPlayer = false) }
        main { activity.setContent {
            val document by editor.document.collectAsState()
            DisposableEffect(editor, engine) {
                editor.onSeek = { engine.seek(it) }
                editor.onScrubbingChanged = engine::setScrubbing
                editor.onPlayingChanged = { if (it) engine.play() else engine.pause() }
                onDispose { editor.onSeek = {}; editor.onScrubbingChanged = {}; editor.onPlayingChanged = {} }
            }
            LaunchedEffect(document.project) { engine.updateProject(document.project) }
            LaunchedEffect(engine) { engine.state.collect { editor.updatePlayback(it.positionUs, it.playing) } }
            EditorTheme {
                CompositionLocalProvider(LocalTimelineRenderCounters provides renderCounters) {
                Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                EditorScreen(editor, app.thumbnails, onBack = {}, onImport = {}, waveforms = app.waveforms, textBitmaps = app.textBitmaps,
                    preview = { modifier ->
                        val surface by engine.surface.collectAsState()
                        Box(modifier) {
                            AndroidView(factory = { PlayerView(it).apply { useController = false } }, update = { it.player = surface.player },
                                onRelease = { it.player = null }, modifier = Modifier.fillMaxSize())
                            TextCanvasLayer(editor, app.textBitmaps, Modifier.fillMaxSize())
                        }
                    })
                }
                }
            }
        } }
        await(30_000) { engine.state.value.submittedPositionUs != null && !engine.state.value.buffering }
        instrumentation.waitForIdleSync()
        timelineBounds() // Wait for the actual unmerged geometry without a virtual test clock.
    }

    private fun close() {
        main { activity.setContent {}; engine.close() }
        scenario.close()
    }

    @Test fun continuousScrubFlingZoomDragTrimAndPlaybackFollow() {
        open()
        try {
            record("ScrubCold") { scrub(120) }
            record("ScrubWarm") { scrub(120) }
            record("Fling") {
                reset()
                val bounds = timelineBounds()
                val x = bounds.left + bounds.width * .7f; val y = bounds.top + 12
                val down = SystemClock.uptimeMillis()
                val starts = main { renderCounters.flingStarts }
                val animationFrames = main { renderCounters.flingFrames }
                event(down, MotionEvent.ACTION_DOWN, x, y)
                repeat(37) { i -> SystemClock.sleep(16); event(down, MotionEvent.ACTION_MOVE, x - (i + 1), y) }
                // Preserve all 45 moves. Use actual wall time in the final eight, so injector
                // overhead cannot turn an intended fast fling into a below-threshold drag.
                val fastStart = SystemClock.uptimeMillis()
                var lastMoveAt = fastStart
                var lastX = x - 37
                repeat(8) {
                    SystemClock.sleep(16); lastMoveAt = SystemClock.uptimeMillis()
                    lastX = x - 37 - (lastMoveAt - fastStart) * .6f
                    event(down, MotionEvent.ACTION_MOVE, lastX, y, at = lastMoveAt)
                }
                // Lift immediately at the last hardware sample, even if injectInputEvent itself
                // stalls under profiling. An unintended >40 ms hold correctly cancels velocity.
                val injectionReturnMs = SystemClock.uptimeMillis() - lastMoveAt
                event(down, MotionEvent.ACTION_UP, lastX, y, sync = true, at = lastMoveAt)
                report("FlingInput", "moves=45, finalSegmentPxPerSecond=600, releaseSampleGapMs=0, injectionReturnMs=$injectionReturnMs")
                await(5_000) { main { renderCounters.flingStarts > starts && renderCounters.flingFrames >= animationFrames + 2 } }
                await(5_000) { !editor.state.value.scrubbing }
            }
            record("PinchZoom") { pinch() }
            record("ClipDrag") {
                reset(); main { editor.select(original.id) }
                val bounds = timelineBounds(); val x = bounds.left + 70 * density + (bounds.width - 70 * density) / 2
                val y = bounds.top + (30 + 38) * density
                val down = SystemClock.uptimeMillis(); event(down, MotionEvent.ACTION_DOWN, x, y)
                SystemClock.sleep(650)
                repeat(60) { i -> SystemClock.sleep(16); event(down, MotionEvent.ACTION_MOVE, x + (i + 1) * .5f, y) }
                event(down, MotionEvent.ACTION_UP, x + 30, y)
                main {
                    assertTrue(editor.committedProject.findClip(original.id)!!.startUs > original.startUs)
                    editor.undo(); assertEquals(original, editor.committedProject.findClip(original.id))
                    editor.redo(); editor.undo()
                }
            }
            record("ClipTrim") {
                reset(); main { editor.select(original.id) }; instrumentation.waitForIdleSync()
                val handle = nodeBounds { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Trim start") == true }
                val x = handle.center.x; val y = handle.center.y; val down = SystemClock.uptimeMillis()
                event(down, MotionEvent.ACTION_DOWN, x, y)
                repeat(60) { i -> SystemClock.sleep(16); event(down, MotionEvent.ACTION_MOVE, x + (i + 1) * .5f, y) }
                event(down, MotionEvent.ACTION_UP, x + 30, y)
                main {
                    val changed = editor.committedProject.findClip(original.id)!!
                    assertTrue(changed.sourceRange.startUs > original.sourceRange.startUs)
                    assertEquals(original.sourceRange.endUs, changed.sourceRange.endUs)
                    editor.undo(); assertEquals(original, editor.committedProject.findClip(original.id))
                }
            }
            record("PlaybackFollow") {
                reset(); main { editor.setPlaying(true) }; SystemClock.sleep(1_800)
                main { assertTrue(editor.state.value.positionUs > 11_000_000); editor.setPlaying(false) }
            }
            assertEquals(1L, main { engine.diagnostics.snapshot().playerCreates })
        } finally { close() }
    }

    @Test fun thirtySecondGestureAndRepeatedEditPlaybackSoak() {
        open()
        try {
            record("GestureSoak30s") {
                val until = SystemClock.elapsedRealtime() + 30_000
                while (SystemClock.elapsedRealtime() < until) { scrub(90); pinch() }
            }
            record("EditPlaybackSoak") {
                repeat(6) {
                    reset(); main { editor.setPlaying(true) }; SystemClock.sleep(350)
                    main {
                        editor.setPlaying(false)
                        editor.beginGesture()
                        editor.previewGesture(TrimClip(original.id, TimeRange(300_000, 1_400_000), 10_100_000))
                        editor.finishGesture(); editor.undo(); editor.redo(); editor.undo()
                        editor.beginGesture()
                        editor.previewGesture(MoveClip(original.id, editor.committedProject.timeline.tracks.first().id, 10_200_000))
                        editor.finishGesture(); editor.undo(); editor.redo(); editor.undo()
                        assertEquals(original, editor.committedProject.findClip(original.id))
                    }
                }
            }
            await(30_000) { !engine.state.value.buffering }
            val metrics = main { engine.diagnostics.snapshot() }
            assertEquals(1L, metrics.playerCreates); assertEquals(0L, metrics.playerReleases)
            assertTrue(app.thumbnails.queueMetrics().queued <= 96)
            assertTrue(app.thumbnails.queueMetrics().running <= app.resourceBudget.thumbnailWorkers)
            assertTrue(app.waveforms.queueMetrics().queued <= 32)
            assertTrue(app.waveforms.queueMetrics().running <= 1)
            val rasterWorkers = Thread.getAllStackTraces().keys.count { it.isAlive && it.name == "FrameStudio:TextRaster" }
            assertTrue("Raster workers accumulated: $rasterWorkers", rasterWorkers <= 1)
            report("soak-resources", "preview=$metrics, thumbnails=${app.thumbnails.queueMetrics()}, thumbnailBytes=${app.thumbnails.memoryBytes}, waveforms=${app.waveforms.queueMetrics()}, waveformBytes=${app.waveforms.memoryBytes}, rasterWorkers=$rasterWorkers, textBytes=${app.textBitmaps.memoryBytes}, pssKb=${Debug.getPss()}")
        } finally { close() }
    }

    private fun reset() {
        main { editor.setPlaying(false); editor.seek(10_500_000); editor.endScrub() }
        await(30_000) { !engine.state.value.buffering }
        instrumentation.waitForIdleSync()
    }

    private fun scrub(count: Int) {
        reset()
        val bounds = timelineBounds(); val x = bounds.left + bounds.width * .65f; val y = bounds.top + 12
        val down = SystemClock.uptimeMillis(); event(down, MotionEvent.ACTION_DOWN, x, y)
        repeat(count) { i -> SystemClock.sleep(16); event(down, MotionEvent.ACTION_MOVE, x + (sin(i / 8.0) * 95).toFloat(), y) }
        val lastX = x + (sin((count - 1) / 8.0) * 95).toFloat()
        // Real stationary hold ends velocity before release; fling is measured independently.
        repeat(8) { SystemClock.sleep(16); event(down, MotionEvent.ACTION_MOVE, lastX, y) }
        if (InstrumentationRegistry.getArguments().getString("timelineCaptureScreen") == "true") {
            instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                File(app.cacheDir, "timeline-interaction.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        assertTrue("Pointer did not update timeline while still held", main { editor.state.value.scrubbing })
        event(down, MotionEvent.ACTION_UP, lastX, y)
        await(15_000) { !engine.state.value.buffering }
        assertNull(engine.state.value.error)
    }

    private fun pinch() {
        reset()
        val bounds = timelineBounds(); val y = bounds.top + (30 + 38) * density
        val x = bounds.left + bounds.width * .65f; val down = SystemClock.uptimeMillis()
        event(down, MotionEvent.ACTION_DOWN, x - 55, y)
        multi(down, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), x - 55, x + 55, y)
        repeat(45) { i -> SystemClock.sleep(16); multi(down, MotionEvent.ACTION_MOVE, x - 55 - i, x + 55 + i, y) }
        multi(down, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), x - 99, x + 99, y)
        event(down, MotionEvent.ACTION_UP, x - 99, y)
        await(15_000) { !engine.state.value.buffering }
        assertNull(engine.state.value.error)
    }

    private fun event(down: Long, action: Int, x: Float, y: Float, sync: Boolean = false, at: Long = SystemClock.uptimeMillis()) {
        val input = MotionEvent.obtain(down, at, action, x, y, 0)
        input.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
        try { check(instrumentation.uiAutomation.injectInputEvent(input, sync)) } finally { input.recycle() }
    }

    private fun multi(down: Long, action: Int, x0: Float, x1: Float, y: Float) {
        val properties = Array(2) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coordinates = arrayOf(x0, x1).map { x -> MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f } }.toTypedArray()
        val input = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 2, properties, coordinates, 0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
        try { check(instrumentation.uiAutomation.injectInputEvent(input, false)) } finally { input.recycle() }
    }

    private fun timelineBounds() = nodeBounds { it.config.getOrNull(SemanticsProperties.TestTag) == "timeline" }
    private fun nodeBounds(matches: (SemanticsNode) -> Boolean): Rect {
        fun find(node: SemanticsNode): SemanticsNode? {
            if (matches(node)) return node
            for (child in node.children) find(child)?.let { return it }
            return null
        }
        fun inView(view: View): Rect? {
            if (view is ViewRootForTest) {
                find(view.semanticsOwner.unmergedRootSemanticsNode)?.let { node ->
                    val location = IntArray(2); view.getLocationOnScreen(location)
                    return node.boundsInRoot.translate(location[0].toFloat(), location[1].toFloat())
                }
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) inView(view.getChildAt(i))?.let { return it }
            return null
        }
        // Android accessibility may merge a clip's handle description into its parent, or cache
        // stale descendants after selection. Read Compose's public test tree on Main instead;
        // actual wall-clock MotionEvents and frame collection remain unchanged.
        val end = SystemClock.elapsedRealtime() + 5_000
        var bounds = main { inView(activity.window.decorView) }
        while (bounds == null && SystemClock.elapsedRealtime() < end) {
            SystemClock.sleep(20)
            bounds = main { inView(activity.window.decorView) }
        }
        return requireNotNull(bounds) { "Requested unmerged timeline input target is absent" }
    }
    private fun await(timeoutMs: Long, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (!condition()) { check(SystemClock.elapsedRealtime() < end) { "Interactive preview timed out: ${engine.state.value}" }; SystemClock.sleep(20) }
    }
    private fun <T> main(block: () -> T): T { var value: T? = null; instrumentation.runOnMainSync { value = block() }; @Suppress("UNCHECKED_CAST") return value as T }
    private fun runtime(key: String) = Debug.getRuntimeStat(key)?.toLongOrNull() ?: 0L
    private fun report(name: String, value: String) = instrumentation.sendStatus(2, Bundle().apply { putString("timeline_interactive_metrics", "$name {$value}") })

    private fun record(name: String, action: () -> Unit) {
        val frameThread = HandlerThread("InteractionFrameMetrics").apply { start() }
        val frames = mutableListOf<LongArray>(); var lost = 0
        val fields = intArrayOf(FrameMetrics.TOTAL_DURATION, FrameMetrics.INPUT_HANDLING_DURATION, FrameMetrics.LAYOUT_MEASURE_DURATION,
            FrameMetrics.DRAW_DURATION, FrameMetrics.SYNC_DURATION, FrameMetrics.COMMAND_ISSUE_DURATION, FrameMetrics.SWAP_BUFFERS_DURATION)
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped -> synchronized(frames) { lost += dropped; frames += LongArray(fields.size) { metrics.getMetric(fields[it]) } } }
        val allocated = runtime("art.gc.bytes-allocated"); val gc = runtime("art.gc.gc-count"); val pause = runtime("art.gc.blocking-gc-time")
        val before = main { engine.diagnostics.snapshot() }; var cpu = 0L
        main { activity.window.addOnFrameMetricsAvailableListener(listener, Handler(frameThread.looper)); cpu = Debug.threadCpuTimeNanos() }
        Trace.beginSection(name)
        try { action() } finally {
            Trace.endSection()
            var cpuMs = 0.0
            main { cpuMs = (Debug.threadCpuTimeNanos() - cpu) / 1e6; activity.window.removeOnFrameMetricsAvailableListener(listener) }
            frameThread.quitSafely(); frameThread.join(2_000)
            val data = synchronized(frames) { frames.toList() }; val total = data.map { it[0] }.sorted()
            val after = main { engine.diagnostics.snapshot() }
            val render = main { "flingStarts=${renderCounters.flingStarts}, flingFrames=${renderCounters.flingFrames}, releaseVelocity=${renderCounters.lastReleaseVelocity}, timelineCommits=${renderCounters.timelineCommits}, trackCommits=${renderCounters.trackCommits}, clipCommits=${renderCounters.clipCommits}, waveformGeometry=${renderCounters.waveformGeometry}" }
            fun percentile(p: Double) = if (total.isEmpty()) 0.0 else total[((total.size - 1) * p).toInt()] / 1e6
            report(name, "frames=${total.size}, p50Ms=${percentile(.5)}, p95Ms=${percentile(.95)}, maxMs=${total.lastOrNull()?.div(1e6)}, over33ms=${total.count { it > 33_333_334 }}, listenerDrops=$lost, mainCpuMs=$cpuMs, allocatedMiB=${(runtime("art.gc.bytes-allocated") - allocated) / 1048576.0}, gcCount=${runtime("art.gc.gc-count") - gc}, blockingGcMs=${runtime("art.gc.blocking-gc-time") - pause}, nativeSeeks=${after.nativeSeeks - before.nativeSeeks}, metrics=$after, stageMeanMs=${fields.indices.map { i -> data.map { it[i] }.average() / 1e6 }}, $render, pssKb=${Debug.getPss()}")
        }
    }
}
