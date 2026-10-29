package com.bbvideoeditor.app.editor

import android.content.pm.ApplicationInfo
import android.os.*
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.app.render.MediaFixtures
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.sin

/** Window FrameMetrics and ART counters around 120 paced finger updates, matching the audit's
 * injection style. Includes real caches and an optional live layered decoder graph. SurfaceView
 * native GPU work is NOT measured by the containing window's GPU_DURATION metric. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class TimelinePerformanceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun representativeTimelineMatrix() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("foundationPerformance")=="true")
        val app=compose.activity.application as EditorApplication
        val fixtures=MediaFixtures(app)
        val video=fixtures.video("benchmark-video",.3f,.5f,.2f)
        val overlay=fixtures.video("benchmark-overlay",.2f,.3f,.7f)
        val audio=fixtures.audio()
        val arguments=InstrumentationRegistry.getArguments()
        val counts=arguments.getString("foundationTimelineCounts")?.split(',')?.map{it.toInt()} ?: listOf(10,25,50,100)
        require(counts.isNotEmpty() && counts.all{it in listOf(10,25,50,100)})
        val assetModes=arguments.getString("foundationTimelineAssets")?.let{listOf(it.toBooleanStrict())} ?: listOf(false,true)
        val nativeModes=arguments.getString("foundationTimelineNative")?.let{listOf(it.toBooleanStrict())} ?: listOf(false,true)
        for(count in counts)for(assets in assetModes)for(native in nativeModes) {
            val tracks=listOf(TrackType.VIDEO,TrackType.OVERLAY,TrackType.AUDIO,TrackType.TEXT).map { type -> Track(name=type.name,type=type,clips=(0 until count).map { index ->
                Clip(startUs=index*2_000_000L,sourceRange=TimeRange(if(type==TrackType.TEXT)0 else 200_000,1_500_000),
                    mediaId=when(type){TrackType.VIDEO->video.id;TrackType.OVERLAY->overlay.id;TrackType.AUDIO->audio.id;TrackType.TEXT->null},
                    text=if(type==TrackType.TEXT)TextContent("Caption $index") else null,
                    transform=if(type==TrackType.OVERLAY)Transform(scaleX=.35f,scaleY=.35f) else Transform())
            }) }
            val project=Project(media=listOf(video,overlay,audio).associateBy{it.id},timeline=Timeline(tracks))
            val editor=EditorViewModel(project)
            compose.runOnUiThread{editor.seek(10_000_000);editor.endScrub()}
            var engine:PreviewEngine?=null
            if(native)compose.runOnUiThread { engine=PreviewEngine(app,app.compiler,10_000_000,renderTextInPlayer=false) }
            val commits=AtomicInteger()
            compose.runOnUiThread { compose.activity.setContent {
                val document by editor.document.collectAsState()
                SideEffect{commits.incrementAndGet()}
                DisposableEffect(editor,engine) {
                    editor.onSeek={engine?.seek(it)}
                    onDispose{editor.onSeek={}}
                }
                LaunchedEffect(document.project){engine?.updateProject(document.project)}
                EditorTheme {
                    EditorScreen(editor,if(assets)app.thumbnails else null,onBack={},onImport={},
                        waveforms=if(assets)app.waveforms else null,textBitmaps=app.textBitmaps,
                        preview={modifier->
                            if(engine!=null) {
                                val surface by engine!!.surface.collectAsState()
                                Box(modifier) {
                                    AndroidView(factory={PlayerView(it).apply{useController=false}},update={it.player=surface.player},
                                        onRelease={it.player=null},modifier=Modifier.fillMaxSize())
                                    TextCanvasLayer(editor,app.textBitmaps,Modifier.fillMaxSize())
                                }
                            } else EmptyPreview(modifier)
                        })
                }
            } }
            try {
                compose.waitForIdle()
                if(native)compose.waitUntil(30_000){engine!!.state.value.submittedPositionUs!=null && !engine!!.state.value.buffering}
                val frameThread=HandlerThread("FoundationFrameMetrics").apply{start()}
                val frames=mutableListOf<Long>();val gpu=mutableListOf<Long>();var drops=0
                val listener=Window.OnFrameMetricsAvailableListener { _,metrics,lost ->
                    synchronized(frames) {
                        drops+=lost
                        if(metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME)==0L) {
                            frames+=metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                            if(Build.VERSION.SDK_INT>=31)gpu+=metrics.getMetric(FrameMetrics.GPU_DURATION)
                        }
                    }
                }
                compose.runOnUiThread{compose.activity.window.addOnFrameMetricsAvailableListener(listener,Handler(frameThread.looper))}
                var startCpu=0L;var endCpu=0L
                val allocatedBefore=runtime("art.gc.bytes-allocated");val gcBefore=runtime("art.gc.gc-count")
                val commitsBefore=commits.get()
                compose.runOnUiThread{startCpu=Debug.threadCpuTimeNanos()}
                val timeline=compose.onNodeWithTag("timeline")
                timeline.performTouchInput{down(androidx.compose.ui.geometry.Offset(width*.65f,12f))}
                repeat(120){step->timeline.performTouchInput {
                    moveTo(androidx.compose.ui.geometry.Offset(width*.65f+(sin(step/8.0)*95).toFloat(),12f),16)
                }}
                timeline.performTouchInput{up()}
                compose.waitForIdle()
                compose.runOnUiThread{endCpu=Debug.threadCpuTimeNanos();compose.activity.window.removeOnFrameMetricsAvailableListener(listener)}
                frameThread.quitSafely();frameThread.join(2_000)
                val values=synchronized(frames){frames.filter{it>0}.sorted()}
                assertTrue("No actual window frames were captured",values.size>=50)
                val metrics=engine?.let{var result:com.bbvideoeditor.app.playback.PreviewMetrics?=null;compose.runOnUiThread{result=it.diagnostics.snapshot()};result}
                if(metrics!=null){assertEquals(1L,metrics.playerCreates);assertEquals(1L,metrics.prepares)}
                fun percentile(p:Double)=values[((values.size-1)*p).toInt()]/1e6
                val report=mapOf("clipsPerTrack" to count,"totalClips" to count*4,"assets" to assets,"native" to native,
                    "frames" to values.size,"p50Ms" to percentile(.5),"p95Ms" to percentile(.95),"maxMs" to values.last()/1e6,
                    "over16ms" to values.count{it>16_666_667},"over33ms" to values.count{it>33_333_334},"listenerDrops" to drops,
                    "mainCpuMs" to (endCpu-startCpu)/1e6,"allocatedMiB" to (runtime("art.gc.bytes-allocated")-allocatedBefore)/1048576.0,
                    "gcCount" to runtime("art.gc.gc-count")-gcBefore,"rootCommits" to commits.get()-commitsBefore,
                    "pssKb" to Debug.getPss(),"windowGpuMeanMs" to gpu.takeIf{it.isNotEmpty()}?.average()?.div(1e6),
                    "debuggable" to (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0),"device" to Build.MODEL,
                    "metrics" to metrics)
                InstrumentationRegistry.getInstrumentation().sendStatus(2,Bundle().apply{putString("foundation_metrics","timeline $report")})
            }finally {
                compose.runOnUiThread{compose.activity.setContent{ }}
                compose.runOnUiThread{engine?.close()}
            }
        }
    }
    private fun runtime(key:String)=Debug.getRuntimeStat(key)?.toLongOrNull() ?: 0L
}
