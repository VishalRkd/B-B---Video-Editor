package com.bbvideoeditor.app.render

import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.export.*
import com.bbvideoeditor.app.media.*
import com.bbvideoeditor.app.playback.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real extractor/codec/Transformer/PixelCopy tests. Fault injection changes evidence or the first
 * codec name only; successful frames and all normalization are produced by the platform. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class MediaCompatibilityInstrumentedTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val app=context.applicationContext as EditorApplication
    private fun project(source:MediaReference)=Project(canvas=CanvasSettings(320,180),media=mapOf(source.id to source),
        timeline=Timeline(listOf(Track(name="video",type=TrackType.VIDEO,clips=listOf(
            Clip(mediaId=source.id,startUs=0,sourceRange=TimeRange(0,minOf(source.durationUs,1_500_000))))))))
    private fun video()=MediaFixtures(context).video("compat-${newId()}",0f,1f,0f)
    private fun assertReleased(backend:AndroidMediaCompatibilityBackend) {
        assertEquals(0,backend.liveCodecs.get());assertEquals(0,backend.liveReaders.get());assertEquals(0,backend.liveExtractors.get())
    }
    @Test fun actualProbeProducesSurfaceOutputAndReleasesAllResources()=runBlocking {
        val backend=AndroidMediaCompatibilityBackend(context,app.resourceBudget)
        MediaCompatibilityService(backend).use { service ->
            val evidence=service.inspect(video())
            assertTrue(evidence.candidates.isNotEmpty())
            val result=service.verify(evidence)
            assertNotNull("No real decoded surface output: $result",result.success)
            assertTrue(result.success!!.samples>0);assertNotNull(result.success!!.outputTimeUs)
            assertReleased(backend)
            val runs=service.probeRuns.get();assertEquals(result,service.verify(evidence));assertEquals(runs,service.probeRuns.get())
        }
    }
    @Test fun reportedNoStillGeneratesVerifiedProxyAndDisplaysRealFramesWithoutHotPathQueries()=runBlocking {
        val source=video();val project=project(source)
        val backend=AndroidMediaCompatibilityBackend(context,app.resourceBudget)
        val negative=object:CompatibilityBackend {
            override val deviceBackend=backend.deviceBackend
            override suspend fun inspect(media:MediaReference)=backend.inspect(media).let { e ->
                e.copy(candidates=e.candidates.map{it.copy(support=DeclaredSupport.UNSUPPORTED)},support=DeclaredSupport.UNSUPPORTED,
                    defaultDecoderName="deliberately-not-the-observed-decoder") }
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate)=backend.probe(source,candidate)
        }
        val leases=app.optimizedMedia.liveLeases
        MediaCompatibilityService(negative).use { service ->
            PreviewTestSession(project,90_000,engineFactory={PreviewEngine(context,CompositionCompiler(context),compatibilityOverride=service)}).use { session ->
                requireNotNull(app.optimizedMedia.cached(project,source.id)).use { prepared ->
                    val reader=android.media.MediaMetadataRetriever()
                    try {
                        reader.setDataSource(context,Uri.parse(prepared.sources.getValue(source.id).uri))
                        val bitmap=requireNotNull(reader.getFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST))
                        try { assertTrue("Prepared first frame is not green",Color.green(bitmap.getPixel(bitmap.width/2,bitmap.height/2))>180) }
                        finally { bitmap.recycle() }
                    } finally { reader.release() }
                }
                session.await("visible green frame") { session.frame()?.let{bitmap->try{
                    val pixel=bitmap.getPixel(bitmap.width/2,bitmap.height/2)
                    session.observation="center=${Integer.toHexString(pixel)} size=${bitmap.width}x${bitmap.height}"
                    Color.green(pixel)>180
                }finally{bitmap.recycle()}}==true }
                val queries=service.inspections.get();val probes=service.probeRuns.get()
                repeat(20){i->session.main{session.engine.seek(i*40_000L)};Thread.sleep(16)}
                session.main{session.engine.seek(850_000)}
                session.await("final submitted seek"){session.engine.state.value.submittedPositionUs?.let{kotlin.math.abs(it-850_000)<80_000}==true}
                assertEquals(queries,service.inspections.get());assertEquals(probes,service.probeRuns.get())
                val metrics=session.main{session.engine.diagnostics.snapshot()}
                assertEquals(1L,metrics.playerCreates);assertEquals(1L,metrics.compositionInstalls);assertEquals(0L,metrics.playerReleases)
                assertTrue(app.optimizedMedia.liveLeases>leases)
            }
            assertReleased(backend)
        }
        assertEquals(leases,app.optimizedMedia.liveLeases)
        assertEquals(source.uri,project.media[source.id]!!.uri)
    }
    @Test fun verifiedCacheRescuesNoOriginalCandidateAndCorruptionRegenerates()=runBlocking {
        val source=video();val project=project(source);val before=app.optimizedMedia.liveLeases
        val first=app.optimizedMedia.resolve(project,setOf(source.id));val variant=first.sources.getValue(source.id)
        val file=File(requireNotNull(Uri.parse(variant.uri).path));first.close();first.close()
        val actual=MediaImporter(context).readAudioVideo(Uri.fromFile(file),"actual","video/mp4",file.length())
        assertEquals(actual.codecMimeType,variant.codecMimeType);assertEquals(actual.width,variant.width)
        assertEquals(actual.height,variant.height);assertEquals(actual.rotationDegrees,variant.rotationDegrees)
        assertEquals(actual.hasAudio,variant.hasAudio);assertEquals(actual.colorTransfer,variant.colorTransfer)
        assertEquals(FrameRateProvenance.MEASURED,variant.frameRateProvenance)
        assertTrue(kotlin.math.abs(variant.durationUs-source.durationUs)<=100_000)
        val backend=object:CompatibilityBackend {
            override val deviceBackend="cache-rescue-test"
            override suspend fun inspect(media:MediaReference)=app.mediaCompatibilityBackend.inspect(media).copy(candidates=emptyList(),support=DeclaredSupport.NO_CANDIDATE)
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate):DecoderObservation=error("Original must not be probed after verified cache hit")
        }
        MediaCompatibilityService(backend).use { service ->
            PreviewTestSession(project,90_000,engineFactory={PreviewEngine(context,CompositionCompiler(context),compatibilityOverride=service)}).use { session ->
                assertNull(session.engine.state.value.failure);assertEquals(0L,service.probeRuns.get())
            }
        }
        file.writeText("deliberately corrupt this test's own derived cache")
        assertNull(app.optimizedMedia.cached(project,source.id))
        app.optimizedMedia.resolve(project,setOf(source.id)).use{assertTrue(File(requireNotNull(Uri.parse(it.sources.getValue(source.id).uri).path)).length()>100)}
        app.optimizedMedia.invalidate(project,source.id)
        assertFalse("Retry kept the failed prepared file",file.exists())
        app.optimizedMedia.resolve(project,setOf(source.id)).use { assertTrue(file.isFile) }
        assertEquals(before,app.optimizedMedia.liveLeases)
    }
    @Test fun transformerActuallyFallsBackAfterFirstDecoderCannotBeCreated()=runBlocking {
        val source=video();val project=project(source);val output=File(context.cacheDir,"decoder-fallback-${newId()}.mp4")
        val selector=MediaCodecSelector { mime,secure,tunneling ->
            val real=MediaCodecSelector.DEFAULT.getDecoderInfos(mime,secure,tunneling)
            if(mime.startsWith("video/") && real.isNotEmpty()) {
                val first=real.first()
                listOf(MediaCodecInfo.newInstance("c2.nonexistent.compatibility-test",first.mimeType,first.codecMimeType,
                    first.capabilities,first.hardwareAccelerated,first.softwareOnly,first.vendor))+real.take(2)
            } else real.take(3)
        }
        try {
            val result=withTimeout(90_000){Media3RenderEngine(context,CompositionCompiler(context),decoderSelectorOverride=selector)
                .render(project,ExportSettings(resolution=720),output)}
            assertTrue(result.videoFrames>0);assertTrue(output.length()>100)
            assertTrue("No genuine decoder fallback: ${result.decoderSelections}",result.decoderSelections.any{it.initializationFailures>0})
        }finally{output.delete()}
    }
    @Test fun terminalFailureDoesNotLoopOnPlayResumeOrRetry()=runBlocking {
        val source=video();val project=project(source)
        val backend=object:CompatibilityBackend {
            override val deviceBackend="terminal-test"
            override suspend fun inspect(media:MediaReference)=SourceEvidence(SourceFormatDescriptor(media,"test","test"),emptyList(),DeclaredSupport.NO_CANDIDATE)
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate):DecoderObservation=error("No candidate")
        }
        val store=object:OptimizedMediaStore {
            override suspend fun invalidate(project:Project,mediaId:String):Unit=error("No derived media to invalidate")
            override suspend fun cached(project:Project,mediaId:String):ResolvedPreviewMedia?=null
            override suspend fun resolve(project:Project,requiredMediaIds:Set<String>,preferredDecoders:Map<String,String>,onProgress:suspend(MediaPreparation)->Unit):ResolvedPreviewMedia=error("No decoder cannot generate")
        }
        MediaCompatibilityService(backend).use{service->
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            lateinit var engine:PreviewEngine
            instrumentation.runOnMainSync{engine=PreviewEngine(context,CompositionCompiler(context),compatibilityOverride=service,optimizedStoreOverride=store);engine.updateProject(project)}
            try {
                withTimeout(15_000){while(engine.state.value.failure==null)delay(20)}
                assertFalse(engine.state.value.failure!!.canRetry)
                val inspections=service.inspections.get()
                instrumentation.runOnMainSync{repeat(3){engine.retry();engine.resume();engine.play()}}
                delay(300)
                assertEquals(inspections,service.inspections.get());assertEquals(0L,engine.diagnostics.snapshot().playerCreates)
                instrumentation.runOnMainSync {
                    engine.suspendPlayback()
                    engine.updateProject(project.copy(media=mapOf(source.id to source.copy(sizeBytes=source.sizeBytes+1))))
                    engine.resume()
                }
                withTimeout(15_000){while(service.inspections.get()==inspections || engine.state.value.failure==null)delay(20)}
                assertEquals("Changed media retained the old terminal verdict",inspections+1,service.inspections.get())
            }finally{instrumentation.runOnMainSync{engine.close()}}
        }
    }
    @Test fun proxyKeepsOriginalAudioAndExportUrisAndSourceTime()=runBlocking {
        val source=TemporalFixtures(context).video(seconds=2,audio=true)
        val project=project(source).let{it.copy(timeline=Timeline(listOf(it.timeline.tracks.single().copy(clips=listOf(
            it.timeline.clips.single().copy(sourceRange=TimeRange(200_000,1_000_000),speed=1.25f))))))}
        app.optimizedMedia.resolve(project,setOf(source.id)).use { lease ->
            val compiler=CompositionCompiler(context)
            val preview=compiler.compile(project,previewSources=lease.sources)
            val audio=preview.composition.sequences.filter{C.TRACK_TYPE_AUDIO in it.trackTypes}.flatMap{it.editedMediaItems}
            assertTrue(audio.any{it.mediaItem.localConfiguration?.uri.toString()==source.uri})
            val export=compiler.compile(project)
            assertFalse(export.composition.sequences.flatMap{it.editedMediaItems}.any{it.mediaItem.localConfiguration?.uri.toString()==lease.sources.getValue(source.id).uri})
            assertEquals(project.durationUs,preview.durationUs)
            assertEquals(TimeRange(200_000,1_000_000),project.timeline.clips.single().sourceRange)
        }
    }
    @Test fun twelveDistinctSequentialSourcesPrepareOnlyOneAndKeepOneNativePlayer()=runBlocking {
        val base=video()
        val sources=(0 until 12).map { index ->
            val file=File(context.cacheDir,"sequential-$index-${newId()}.mp4")
            File(requireNotNull(Uri.parse(base.uri).path)).copyTo(file)
            base.copy(id=newId(),uri=Uri.fromFile(file).toString(),displayName="Sequential $index")
        }
        val difficult=sources[7].id
        val project=Project(canvas=CanvasSettings(320,180),media=sources.associateBy{it.id},timeline=Timeline(listOf(
            Track(name="sequential",type=TrackType.VIDEO,clips=sources.mapIndexed{index,source->
                Clip(mediaId=source.id,startUs=index*200_000L,sourceRange=TimeRange(0,200_000))}))))
        val backend=object:CompatibilityBackend {
            override val deviceBackend=app.mediaCompatibilityBackend.deviceBackend
            override suspend fun inspect(media:MediaReference)=app.mediaCompatibilityBackend.inspect(media).let{e->
                if(media.id==difficult)e.copy(support=DeclaredSupport.UNSUPPORTED,defaultDecoderName="force-alternate",
                    candidates=e.candidates.map{it.copy(support=DeclaredSupport.UNSUPPORTED)}) else e}
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate)=app.mediaCompatibilityBackend.probe(source,candidate)
        }
        val generated=mutableSetOf<String>()
        val store=object:OptimizedMediaStore {
            override suspend fun invalidate(project:Project,mediaId:String)=app.optimizedMedia.invalidate(project,mediaId)
            override suspend fun cached(project:Project,mediaId:String)=app.optimizedMedia.cached(project,mediaId)
            override suspend fun resolve(project:Project,requiredMediaIds:Set<String>,preferredDecoders:Map<String,String>,onProgress:suspend(MediaPreparation)->Unit):ResolvedPreviewMedia {
                generated+=requiredMediaIds
                return app.optimizedMedia.resolve(project,requiredMediaIds,preferredDecoders,onProgress)
            }
        }
        MediaCompatibilityService(backend).use{service->
            PreviewTestSession(project,90_000,engineFactory={PreviewEngine(context,CompositionCompiler(context),compatibilityOverride=service,optimizedStoreOverride=store)}).use{session->
                assertEquals(setOf(difficult),generated)
                assertEquals(12L,service.inspections.get())
                assertEquals(1,session.engine.state.value.admission!!.peakActiveVideos)
                session.main{session.engine.seek(2_250_000)}
                session.await("last of twelve sources submitted"){session.engine.state.value.submittedPositionUs?.let{kotlin.math.abs(it-2_250_000)<80_000}==true}
                session.await("last source visible"){session.frame()?.let{b->try{Color.green(b.getPixel(b.width/2,b.height/2))>180}finally{b.recycle()}}==true}
                assertEquals(1L,session.main{session.engine.diagnostics.snapshot().playerCreates})
                assertTrue(project.media.values.all{!it.uri.contains("optimized_preview")})
            }
        }
    }
    @Test fun cancellingRealNormalizationRemovesPartialAndReleasesLease()=runBlocking {
        val source=MediaFixtures(context).video("cancel-preparation-${newId()}",0f,1f,0f,frames=600)
        val project=project(source);val baseline=app.optimizedMedia.liveLeases
        val reached=CompletableDeferred<Unit>()
        var updates=0
        val work=launch {
            app.optimizedMedia.resolve(project,setOf(source.id),onProgress={
                // The first callback announces generation before Transformer starts. Wait
                // for its polling callbacks so cancellation exercises an active native render.
                if(++updates>=3) { reached.complete(Unit);awaitCancellation() }
            }).close()
        }
        withTimeout(30_000){reached.await()}
        work.cancelAndJoin()
        assertEquals(baseline,app.optimizedMedia.liveLeases)
        val key=app.mediaCompatibility.key(app.mediaCompatibility.inspect(source))
        assertTrue(File(context.cacheDir,"optimized_preview_v4").listFiles().orEmpty().none{it.name.startsWith(key) && it.name.contains(".partial")})
        assertTrue(File(requireNotNull(Uri.parse(source.uri).path)).length()>0)
        assertReleased(app.mediaCompatibilityBackend)
    }

    /** Host-generated, independently decoded fixtures; explicitly opted in, never a silent pass. */
    @Test fun controlledHevcAnd4kMatrixUsesActualPixelsAndExportsOriginals()=runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("compatibilityMatrix")=="true")
        for((name,identity) in listOf("media-compat-avc1080.mp4" to 0,"foundation-4k30.mp4" to 3,
            "media-compat-hevc1080.mp4" to 1,"media-compat-hevc4k.mp4" to 2)) {
            val file=File(context.cacheDir,name);assertTrue("Required controlled fixture missing: $name",file.isFile)
            val source=MediaImporter(context).readAudioVideo(Uri.fromFile(file),name,"video/mp4",file.length())
            val project=project(source)
            val evidence=app.mediaCompatibility.inspect(source)
            val probe=app.mediaCompatibility.verify(evidence)
            assertNotNull("$name probe: $probe",probe.success)
            val started=SystemClock.elapsedRealtime()
            PreviewTestSession(project,120_000).use{session->
                session.main{session.engine.seek(850_000)}
                session.await("$name final barcode",60_000){session.frame()?.let{bitmap->try{
                    TemporalFixtures(context).decode(bitmap)?.let{it.source==identity && kotlin.math.abs(it.frame-25)<=2}
                }finally{bitmap.recycle()}}==true}
                val playableMs=SystemClock.elapsedRealtime()-started
                val routes=session.main{session.engine.diagnostics.snapshot()}
                val result=File(context.cacheDir,"matrix-export-${newId()}.mp4")
                try {
                    val rendered=withTimeout(120_000){Media3RenderEngine(context,CompositionCompiler(context)).render(project,ExportSettings(resolution=720),result)}
                    assertTrue(rendered.videoFrames>0)
                    val retriever=android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(result.path)
                        val bitmap=requireNotNull(retriever.getFrameAtTime(850_000,android.media.MediaMetadataRetriever.OPTION_CLOSEST))
                        try {
                            val decoded=TemporalFixtures(context).decode(bitmap)
                            assertNotNull("Export missing barcode",decoded);assertEquals(identity,decoded!!.source)
                            assertTrue("Export source time changed: $decoded",kotlin.math.abs(decoded.frame-25)<=2)
                        }finally{bitmap.recycle()}
                    }finally{retriever.release()}
                    val events=session.main{session.engine.diagnostics.events().map{it.kind}.distinct()}
                    InstrumentationRegistry.getInstrumentation().sendStatus(0,android.os.Bundle().apply {
                        putString("compatibility_matrix","$name declared=${evidence.support} candidates=${evidence.candidates.map{it.name to it.support}} probe=${probe.success} previewMs=$playableMs admission=${session.engine.state.value.admission} render=$rendered metrics=$routes events=$events")
                    })
                }finally{result.delete()}
            }
            assertReleased(app.mediaCompatibilityBackend)
        }
    }

}
