package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.media.*
import com.bbvideoeditor.app.playback.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/** Opt-in, repeatable REAL-surface benchmark. Run with -e foundationPerformance true.
 * All source/pixel/lifecycle checks are assertions; timing/PSS observations describe this device
 * and build only. They are not universal device performance promises. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class FoundationPerformanceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val fixtures=TemporalFixtures(context)
    @Before fun optIn(){assumeTrue(InstrumentationRegistry.getArguments().getString("foundationPerformance")=="true")}

    @Test fun oneHundredTwentySeeksRetainExecutorAndConvergeToRealFrames() {
        val base=fixtures.video(source=1)
        val overlay=fixtures.video(fps=24,source=2)
        val audio=MediaFixtures(context).audio()
        for(mode in listOf("simple","layered","animated")) {
            val baseClips=(0 until 25).map { i -> Clip(mediaId=base.id,startUs=i*2_000_000L,sourceRange=TimeRange(300_000,1_000_000),
                transform=if(mode=="animated")Transform(scaleX=.6f,scaleY=.6f) else Transform(),
                properties=if(mode=="animated")listOf(AnimatedProperty("translationX",0f,listOf(Keyframe(300_000,-.3f),Keyframe(1_300_000,.3f)))) else emptyList()) }
            val tracks=listOf(Track(name="base",type=TrackType.VIDEO,clips=baseClips))+
                if(mode!="simple")listOf(
                    Track(name="overlay",type=TrackType.OVERLAY,clips=(0 until 25).map { i ->
                        Clip(mediaId=overlay.id,startUs=i*2_000_000L+500_000,sourceRange=TimeRange(200_000,800_000),transform=Transform(scaleX=.25f,scaleY=.25f,translationX=.65f)) }),
                    Track(name="audio",type=TrackType.AUDIO,clips=(0 until 25).map { i ->
                        Clip(mediaId=audio.id,startUs=i*2_000_000L,sourceRange=TimeRange(0,1_000_000)) }),
                    Track(name="captions",type=TrackType.TEXT,clips=(0 until 25).map { i ->
                        Clip(startUs=i*2_000_000L+1_500_000,sourceRange=TimeRange(0,300_000),text=TextContent("Gap $i")) })) else emptyList()
            val project=Project(canvas=CanvasSettings(320,180),media=listOf(base,overlay,audio).associateBy{it.id},timeline=Timeline(tracks))
            val startup=SystemClock.elapsedRealtime()
            PreviewTestSession(project).use{session->
                session.await("initial submitted frame"){session.engine.state.value.submittedPositionUs!=null}
                val firstFrameMs=SystemClock.elapsedRealtime()-startup
                val pssBefore=Debug.getPss()
                val latency=LongArray(120)
                repeat(120){index->
                    session.main {
                        val start=System.nanoTime()
                        session.engine.seek(index*80_000L)
                        latency[index]=System.nanoTime()-start
                    }
                    Thread.sleep(16)
                }
                val final=8_850_000L
                val convergence=SystemClock.elapsedRealtime()
                session.main{session.engine.seek(final)}
                assertFrame(session,project,final)
                val finalMs=SystemClock.elapsedRealtime()-convergence
                val metrics=session.main{session.engine.diagnostics.snapshot()}
                assertEquals("$mode recreated its player",1L,metrics.playerCreates)
                assertEquals(0L,metrics.playerReleases)
                assertEquals("$mode reinstalled the composition during transport",1L,metrics.compositionInstalls)
                assertEquals(1L,metrics.prepares)
                // Item switches/prewarming can legitimately replace codecs inside one retained
                // player. Bound both total churn and the currently retained native decoder set.
                assertTrue("$mode excessive native decoder churn: $metrics",metrics.decoderInitializations<=40)
                assertTrue("$mode retained too many native decoders: $metrics",metrics.decoderInitializations-metrics.nativeDecoderReleases<=4)
                latency.sort()
                report("preview",mapOf("mode" to mode,"firstFrameMs" to firstFrameMs,"offerP50Ms" to latency[59]/1e6,
                    "offerP95Ms" to latency[113]/1e6,"offerMaxMs" to latency.last()/1e6,"finalPixelMs" to finalMs,
                    "pssBeforeKb" to pssBefore,"pssAfterKb" to Debug.getPss(),"metrics" to metrics))
            }
        }
    }

    @Test fun original4kAndLayeredOptimizedPreviewPreserveNumberedSourceFrames()=runBlocking {
        val sourceFile=File(context.cacheDir,"foundation-4k30.mp4")
        assumeTrue("Install the host-generated numbered 4K fixture as documented in tools/FOUNDATION_BENCHMARKS.md",sourceFile.isFile)
        val source=MediaImporter(context).importMedia(listOf(Uri.fromFile(sourceFile))).media.single()
        try {
            assertEquals(3840,source.width);assertEquals(2160,source.height)
            val reader=android.media.MediaMetadataRetriever()
            try {
                reader.setDataSource(sourceFile.path)
                val bitmap=requireNotNull(reader.getFrameAtTime(850_000,android.media.MediaMetadataRetriever.OPTION_CLOSEST))
                try {
                    val identity=requireNotNull(fixtures.decode(bitmap))
                    assertEquals(3,identity.source)
                    assertTrue("4K fixture itself has wrong source time: $identity",abs(identity.frame-25)<=1)
                }finally{bitmap.recycle()}
            }finally{reader.release()}
            for(layers in 1..2) {
                val project=Project(canvas=CanvasSettings(1920,1080),media=mapOf(source.id to source),timeline=Timeline(
                    (0 until layers).map{layer->Track(name="4K $layer",type=if(layer==0)TrackType.VIDEO else TrackType.OVERLAY,
                        clips=listOf(Clip(mediaId=source.id,startUs=0,sourceRange=TimeRange(if(layer==0)0 else 400_000,2_400_000),
                            transform=if(layer==0)Transform() else Transform(scaleX=.35f,scaleY=.35f))))}))
                val started=SystemClock.elapsedRealtime()
                PreviewTestSession(project,90_000).use { session ->
                    session.await("4K first output submission",90_000){session.engine.state.value.submittedPositionUs!=null}
                    val first=SystemClock.elapsedRealtime()-started
                    repeat(60){index->session.main{session.engine.seek(index*31_000L%project.durationUs)};Thread.sleep(16)}
                    val final=850_000L;val at=SystemClock.elapsedRealtime()
                    session.main{session.engine.seek(final)}
                    assertFrame(session,project,final,sourceId=3)
                    val state=session.engine.state.value
                    val metrics=session.main{session.engine.diagnostics.snapshot()}
                    assertEquals(1L,metrics.playerCreates);assertEquals(1L,metrics.prepares)
                    assertTrue(state.admission!=null)
                    report("4k",mapOf("layers" to layers,"firstFrameMs" to first,"finalPixelMs" to SystemClock.elapsedRealtime()-at,
                        "pssKb" to Debug.getPss(),"admission" to state.admission,"metrics" to metrics))
                }
            }
            val thumbnails=ThumbnailCache(context)
            val start=SystemClock.elapsedRealtime()
            val thumbnail=thumbnails.thumbnail(source,700_000,192,192)
            assertNotNull(thumbnail)
            report("4k-thumbnail",mapOf("coldMs" to SystemClock.elapsedRealtime()-start,"bytes" to thumbnails.memoryBytes,"queue" to thumbnails.queueMetrics()))
            // Export resolves ORIGINAL source URIs, regardless of preview optimizer/cache state.
            val exportProject=Project(canvas=CanvasSettings(3840,2160),media=mapOf(source.id to source),timeline=Timeline(listOf(
                Track(name="Original 4K export",type=TrackType.VIDEO,clips=listOf(Clip(mediaId=source.id,startUs=0,sourceRange=TimeRange(0,1_000_000)))))))
            val output=File(context.cacheDir,"foundation-4k-export.mp4")
            try {
                val result=kotlinx.coroutines.withTimeout(120_000){
                    com.bbvideoeditor.app.export.Media3RenderEngine(context,CompositionCompiler(context)).render(exportProject,
                        com.bbvideoeditor.app.export.ExportSettings(resolution=2160,frameRate=30,bitrate=12_000_000),output)
                }
                assertTrue(result.bytes>0);assertNotNull(result.actualFrameRate)
                if(result.width!=3840 || result.height!=2160) {
                    assertFalse("Observed fallback is still advertised as supported",
                        com.bbvideoeditor.app.export.ExportSettings(resolution=2160,frameRate=30).supportedBy(exportProject,
                            com.bbvideoeditor.app.export.ExportCapabilities.inspect(exportProject)))
                }
                report("4k-export",mapOf("requestedWidth" to 3840,"requestedHeight" to 2160,"requestedFps" to 30,"actual" to result))
            }finally{output.delete()}
        } finally { File(Uri.parse(source.uri).path!!).takeIf{it!=sourceFile}?.delete() }
    }

    @Test fun repeatedLifecycleAndParameterStormKeepCurrentRevisionAndBoundResources() {
        val media=fixtures.video(source=1)
        val clip=Clip(mediaId=media.id,startUs=0,sourceRange=TimeRange(0,3_000_000),transform=Transform(scaleX=.5f,scaleY=.5f))
        val project=Project(canvas=CanvasSettings(320,180),media=mapOf(media.id to media),timeline=Timeline(listOf(Track(name="V",type=TrackType.VIDEO,clips=listOf(clip)))))
        PreviewTestSession(project).use{session->
            session.await("initial submission"){session.engine.state.value.submittedPositionUs!=null}
            session.main {
                repeat(120){index->
                    session.engine.updateProject(UpdateClip(clip.id,transform=clip.transform.copy(translationX=(index%10)/20f)).apply(project))
                    session.engine.seek(index*17_000L)
                }
                session.engine.updateProject(project);session.engine.seek(850_000)
            }
            assertFrame(session,project,850_000)
            // Structural revisions are allowed to reinstall a graph, but stale preparation must
            // never replace the newest document. Exercise real timing/source-range changes too.
            repeat(8) { index ->
                val edited=UpdateClip(clip.id,speed=if(index%2==0)1.25f else .8f).apply(project)
                session.main{session.engine.updateProject(edited);session.engine.seek(600_000+index*10_000L)}
                Thread.sleep(90)
            }
            session.main{session.engine.updateProject(project);session.engine.seek(850_000)}
            session.await("latest graph after structural edit storm"){!session.engine.state.value.buffering && session.engine.state.value.submittedPositionUs!=null}
            assertFrame(session,project,850_000)
            repeat(6) {
                session.main{session.engine.suspendPlayback()}
                assertNull(session.engine.state.value.player)
                session.main{session.engine.resume()}
                session.await("resumed current revision"){session.engine.state.value.submittedPositionUs!=null && session.engine.state.value.player!=null && !session.engine.state.value.buffering}
                assertFrame(session,project,850_000)
            }
            val app=context.applicationContext as EditorApplication
            session.main{app.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)}
            assertEquals(0,app.thumbnails.memoryBytes);assertEquals(0,app.textBitmaps.memoryBytes)
            val metrics=session.main{session.engine.diagnostics.snapshot()}
            assertEquals(1L,metrics.playerCreates-metrics.playerReleases)
            report("lifecycle",mapOf("pssKb" to Debug.getPss(),"metrics" to metrics))
        }
    }
    private fun assertFrame(session:PreviewTestSession,project:Project,timeUs:Long,sourceId:Int=1) {
        val evaluated=RenderPlan.derive(project).evaluateFrame(timeUs)
        session.await("numbered source pixels at $timeUs",15_000) {
            check(session.engine.state.value.error==null){"${session.engine.state.value}; events=${session.main{session.engine.diagnostics.events()}}; metrics=${session.main{session.engine.diagnostics.snapshot()}}"}
            val bitmap=session.frame() ?: return@await false
            try {
                evaluated.visuals.all { layer ->
                    val transform=layer.transform
                    val left=(1-transform.scaleX)/2+transform.translationX/2
                    val top=(1-transform.scaleY)/2-transform.translationY/2
                    val actual=fixtures.decode(bitmap,left,top,transform.scaleX,transform.scaleY)
                    val expectedSource=if(layer.clip.mediaId==project.media.values.first().id)sourceId else 2
                    val fps=project.media[layer.clip.mediaId]?.frameRate?.takeIf{it>0} ?: 30f
                    session.observation="layer=${layer.clip.id} actual=$actual expectedSource=$expectedSource sourceTime=${layer.time.source.us}"
                    actual?.source==expectedSource && abs(actual.frame-(layer.time.source.us*fps/1_000_000).toInt())<=1
                }
            }finally{bitmap.recycle()}
        }
    }
    private fun report(kind:String,values:Map<String,Any?>) {
        instrumentation.sendStatus(2,Bundle().apply{putString("foundation_metrics","$kind $values")})
    }
}
