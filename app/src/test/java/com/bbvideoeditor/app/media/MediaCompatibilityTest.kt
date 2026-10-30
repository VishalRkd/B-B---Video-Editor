package com.bbvideoeditor.app.media

import com.bbvideoeditor.app.playback.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MediaCompatibilityTest {
    private val budget=EngineResourceBudget(1,1,1,1,64L*1024*1024,4)
    private fun media(id:String="a",mime:String="video/avc",width:Int=320,height:Int=180)=MediaReference(id=id,uri="file:/$id.mp4",
        displayName=id,kind=MediaKind.VIDEO,durationUs=2_000_000,width=width,height=height,frameRate=30f,mimeType="video/mp4",codecMimeType=mime,
        frameRateProvenance=FrameRateProvenance.TRACK_DECLARED)
    private fun project(sources:List<MediaReference>,layers:Boolean=false)=Project(media=sources.associateBy{it.id},timeline=Timeline(
        if(layers)sources.mapIndexed{index,m->Track(name=m.id,type=if(index==0)TrackType.VIDEO else TrackType.OVERLAY,
            clips=listOf(Clip(mediaId=m.id,startUs=0,sourceRange=TimeRange(0,1_000_000))))}
        else listOf(Track(name="Sequential",type=TrackType.VIDEO,clips=sources.mapIndexed{i,m->Clip(mediaId=m.id,startUs=i*2_000_000L,sourceRange=TimeRange(0,1_000_000))}))))
    private class Backend(var support:DeclaredSupport=DeclaredSupport.SUPPORTED):CompatibilityBackend {
        override var deviceBackend="device-a"
        var version="version-1";var alternate=false;var timeout=false;var allFail=false
        val probes=AtomicInteger();val live=AtomicInteger();val maximum=AtomicInteger()
        var blocked=false
        override suspend fun inspect(media:MediaReference):SourceEvidence {
            val value=if(media.id=="difficult")DeclaredSupport.UNSUPPORTED else support
            val candidates=listOf(DecoderCandidate("primary",value,hardware=true),DecoderCandidate("alternate",DeclaredSupport.UNKNOWN,software=true))
            return SourceEvidence(SourceFormatDescriptor(media,version,"csd"),if(value==DeclaredSupport.NO_CANDIDATE)emptyList() else candidates,value,"primary")
        }
        override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate):DecoderObservation {
            probes.incrementAndGet();val count=live.incrementAndGet();maximum.accumulateAndGet(count,::maxOf)
            try {
                if(timeout || blocked)delay(60_000) else delay(5)
                return DecoderObservation(candidate.name,if(allFail || (alternate && candidate.name=="primary"))ProbeStatus.CONFIGURATION_FAILED else ProbeStatus.OUTPUT,1,0)
            }finally{live.decrementAndGet()}
        }
    }
    private class Store:OptimizedMediaStore {
        var cached=false;var fail=false;var generated=0;var closed=0;var invalidated=emptySet<String>();var lastIds=emptySet<String>();var decoder:String?=null
        private fun part(project:Project,ids:Set<String>)=ResolvedPreviewMedia(ids.associateWith { id -> project.media.getValue(id).copy(uri="file:/prepared-$id.mp4",width=640,height=360,
            codecMimeType="video/avc",colorTransfer=3,frameRate=30f,frameRateProvenance=FrameRateProvenance.MEASURED) }){closed++}
        override suspend fun cached(project:Project,mediaId:String)=if(cached)part(project,setOf(mediaId)) else null
        override suspend fun invalidate(project:Project,mediaId:String) { invalidated=invalidated+mediaId;cached=false }
        override suspend fun resolve(project:Project,requiredMediaIds:Set<String>,preferredDecoders:Map<String,String>,onProgress:suspend (MediaPreparation)->Unit):ResolvedPreviewMedia {
            generated++;lastIds=requiredMediaIds;decoder=preferredDecoders.values.firstOrNull()
            if(fail)throw sourceFailure(project.media.getValue(requiredMediaIds.first()),MediaFailureKind.DECODE,"Observed normalization failure",MediaRetry.PREPARE)
            return part(project,requiredMediaIds)
        }
    }
    private suspend fun prepare(backend:Backend,store:Store,project:Project,b:EngineResourceBudget=budget):PreparedPreview =
        MediaCompatibilityService(backend).use{service->PreviewSourcePreparer(service,PreviewResourcePolicy(b),store).prepare(RenderPlan.derive(project),PreviewQuality.BALANCED)}

    @Test fun supportedAvcDoesNotProbeOrNormalize()=runBlocking {
        val backend=Backend();val store=Store();val source=media()
        val result=prepare(backend,store,project(listOf(source)))
        assertEquals("ORIGINAL_REPORTED",result.routes[source.id]);assertEquals(0,backend.probes.get());assertEquals(0,store.generated);result.lease.close()
    }
    @Test fun reportedNoWithActualDefaultOutputIsNotTerminal()=runBlocking {
        val backend=Backend(DeclaredSupport.UNSUPPORTED);val store=Store()
        val result=prepare(backend,store,project(listOf(media())))
        assertEquals("ORIGINAL_OBSERVED",result.routes["a"]);assertTrue(backend.probes.get()>0);assertEquals(0,store.generated);result.lease.close()
    }
    @Test fun queryErrorsAreEvidenceAndOtherCandidateCanRecover()=runBlocking {
        val backend=Backend(DeclaredSupport.QUERY_ERROR).apply{alternate=true};val store=Store()
        val result=prepare(backend,store,project(listOf(media())))
        assertEquals("OPTIMIZED_GENERATED",result.routes["a"]);assertEquals("alternate",store.decoder);assertEquals(1,store.generated)
        result.lease.close();result.lease.close();assertEquals(1,store.closed)
    }
    @Test fun alternateSuccessGeneratesAndSubstitutesOnlyRequiredSource()=runBlocking {
        val backend=Backend().apply{alternate=true};val store=Store()
        val sources=(0 until 12).map{media(if(it==7)"difficult" else "supported-$it")}
        val project=project(sources);val result=prepare(backend,store,project)
        assertEquals(setOf("difficult"),store.lastIds);assertEquals(setOf("difficult"),result.lease.sources.keys)
        assertEquals(1,result.admission.peakActiveVideos);assertEquals(1,result.admission.declaredVideoInputs)
        assertEquals(1,result.admission.graphInputs);assertEquals(12,project.media.size)
        assertTrue(project.media.values.all{!it.uri.contains("prepared")});result.lease.close()
    }
    @Test fun allCandidatesAndNormalizationFailTerminalWithoutRetry()=runBlocking {
        val backend=Backend(DeclaredSupport.UNSUPPORTED).apply{allFail=true};val store=Store().apply{fail=true}
        try{prepare(backend,store,project(listOf(media())));fail("Expected terminal failure")}
        catch(e:MediaCompatibilityException){assertFalse(e.failure.canRetry);assertEquals("a",e.failure.sourceMediaId)}
        assertEquals(2,backend.probes.get());assertEquals(1,store.generated);assertEquals(0,backend.live.get())
    }
    @Test fun cachedVariantRescuesEvenNoOriginalDecoderWithoutOriginalProbe()=runBlocking {
        val backend=Backend(DeclaredSupport.NO_CANDIDATE);val store=Store().apply{cached=true}
        val result=prepare(backend,store,project(listOf(media())))
        assertEquals("OPTIMIZED_CACHE",result.routes["a"]);assertEquals(0,backend.probes.get());assertEquals(0,store.generated)
        result.lease.close();assertEquals(1,store.closed)
    }
    @Test fun retryOfFailedPreparedSourceInvalidatesCacheAndActuallyRegenerates()=runBlocking {
        val backend=Backend();val store=Store().apply{cached=true}
        MediaCompatibilityService(backend).use { service ->
            val result=PreviewSourcePreparer(service,PreviewResourcePolicy(budget),store)
                .prepare(RenderPlan.derive(project(listOf(media()))),PreviewQuality.BALANCED,regenerate=setOf("a"))
            assertEquals(setOf("a"),store.invalidated);assertEquals(1,store.generated)
            assertEquals("OPTIMIZED_GENERATED",result.routes["a"]);result.lease.close()
        }
    }
    @Test fun oneCandidateQueryErrorDoesNotOverrideDefaultSupportedCandidate()=runBlocking {
        val backend=object:CompatibilityBackend {
            override val deviceBackend="device"
            override suspend fun inspect(media:MediaReference)=SourceEvidence(SourceFormatDescriptor(media,"v","csd"),listOf(
                DecoderCandidate("bad",DeclaredSupport.QUERY_ERROR,queryError="VendorException"),DecoderCandidate("good",DeclaredSupport.SUPPORTED,hardware=true)),DeclaredSupport.SUPPORTED,"good")
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate):DecoderObservation=error("Must not probe")
        }
        MediaCompatibilityService(backend).use{service->
            val result=PreviewSourcePreparer(service,PreviewResourcePolicy(budget),Store()).prepare(RenderPlan.derive(project(listOf(media()))),PreviewQuality.BALANCED)
            assertEquals("ORIGINAL_REPORTED",result.routes["a"]);result.lease.close()
        }
    }
    @Test fun lowMemoryUsesNormalizedSourcesAndRetainsEveryOverlappingLayer()=runBlocking {
        val source=listOf(media("base",width=1920,height=1080),media("overlay",width=1920,height=1080))
        val result=prepare(Backend(),Store(),project(source,true),budget.copy(decoderWorkingBytes=48L*1024*1024))
        assertEquals(2,result.admission.peakActiveVideos);assertEquals(setOf("base","overlay"),result.lease.sources.keys);result.lease.close()
    }
    @Test fun graphLimitRemainsHardBeforeProbeOrGeneration()=runBlocking {
        val backend=Backend();val store=Store()
        try{prepare(backend,store,project((0..2).map{media("$it")},true),budget.copy(maximumVideoInputs=2));fail("Expected graph limit")}
        catch(e:MediaCompatibilityException){assertEquals(MediaFailureKind.GRAPH_LIMIT,e.failure.kind);assertFalse(e.failure.canRetry)}
        assertEquals(0,backend.probes.get());assertEquals(0,store.generated)
    }
    @Test fun preparedGraphStillOverFixedBudgetRequiresAnEditInsteadOfIdenticalRetry()=runBlocking {
        val store=Store()
        try { prepare(Backend(),store,project(listOf(media())),budget.copy(decoderWorkingBytes=1L shl 20));fail("Expected memory limit") }
        catch(error:MediaCompatibilityException) { assertEquals(MediaFailureKind.RESOURCE,error.failure.kind);assertFalse(error.failure.canRetry) }
        assertEquals(1,store.generated);assertEquals(1,store.closed)
    }
    @Test fun failureEvidenceDoesNotPoisonSameSizeContainerDifferentSourceCodec()=runBlocking {
        val backend=Backend();MediaCompatibilityService(backend).use{service->
            val a=service.inspect(media("a"));val b=service.inspect(media("b","video/hevc"))
            service.recordFailure(a);assertTrue(service.failed(a));assertFalse(service.failed(b));assertNotEquals(service.key(a),service.key(b))
            backend.version="changed";assertFalse(service.failed(service.inspect(media("a"))))
        }
    }
    @Test fun probeTimeoutIsTransientAndNotPermanentlyCached()=runBlocking {
        val backend=Backend().apply{timeout=true}
        MediaCompatibilityService(backend,probeTimeoutMs=15).use{service->
            val evidence=service.inspect(media());val timed=service.verify(evidence)
            assertTrue(timed.transient);assertTrue(timed.attempts.all{it.status==ProbeStatus.TIMEOUT})
            backend.timeout=false;assertNotNull(service.verify(evidence).success);assertEquals(0,backend.live.get());assertFalse(service.failed(evidence))
        }
    }
    @Test fun transientCandidatePageIsRetriedBeforeDeclaringAllCandidatesExhausted()=runBlocking {
        var transient=true
        val backend=object:CompatibilityBackend {
            override val deviceBackend="paged-probe"
            override suspend fun inspect(media:MediaReference)=SourceEvidence(SourceFormatDescriptor(media,"v","csd"),
                (0..3).map{DecoderCandidate("decoder-$it",DeclaredSupport.UNKNOWN)},DeclaredSupport.UNKNOWN)
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate)=DecoderObservation(candidate.name,
                if(transient)ProbeStatus.TIMEOUT else if(candidate.name=="decoder-0")ProbeStatus.OUTPUT else ProbeStatus.CONFIGURATION_FAILED)
        }
        MediaCompatibilityService(backend).use { service ->
            val evidence=service.inspect(media())
            assertTrue(service.verify(evidence).transient)
            transient=false
            assertEquals("decoder-0",service.verify(evidence).success?.decoder)
        }
    }
    @Test fun cancellingOldGenerationReleasesProbeSlotAndNextGenerationWorks()=runBlocking {
        val backend=Backend().apply{blocked=true}
        MediaCompatibilityService(backend).use{service->
            val e=service.inspect(media());val job=launch{service.verify(e)}
            withTimeout(2_000){while(backend.live.get()==0)delay(5)}
            job.cancelAndJoin();withTimeout(2_000){while(service.queueMetrics().running!=0)delay(5)}
            assertEquals(0,backend.live.get());backend.blocked=false;assertNotNull(service.verify(e).success)
        }
    }
    @Test fun concurrentSourcesUseOneProbeWorkerAndBoundedQueue()=runBlocking {
        val backend=Backend();MediaCompatibilityService(backend).use{service->
            coroutineScope{(0 until 20).map{i->async{service.verify(service.inspect(media("$i")))}}.awaitAll()}
            assertEquals(1,backend.maximum.get());assertEquals(0,backend.live.get());assertTrue(service.queueMetrics().queued<=24)
        }
    }
    @Test fun main10HdrHlgAndUnknownMetadataRemainDistinctEvidence() {
        val base=media("hevc","video/hevc").copy(codecProfile=null,codecLevel=null,bitDepth=null,frameRate=0f,frameRateProvenance=FrameRateProvenance.UNKNOWN)
        assertNull(base.bitDepth);assertEquals(FrameRateProvenance.UNKNOWN,base.frameRateProvenance)
        fun fingerprint(m:MediaReference,device:String="d")=SourceFormatDescriptor(m,"v","csd").fingerprint(device)
        val main=base.copy(codecProfile=1);val main10=base.copy(codecProfile=2)
        assertNull(main10.bitDepth);assertNotEquals(fingerprint(main),fingerprint(main10))
        assertNotEquals(fingerprint(main10.copy(colorTransfer=6)),fingerprint(main10.copy(colorTransfer=7)))
        assertNotEquals(fingerprint(main10),fingerprint(main10,"other-device"))
        assertNotEquals(fingerprint(main10),fingerprint(main10.copy(bitDepth=10)))
    }
    @Test fun successfulEvidenceExpiresAndExplicitRetryInvalidatesOnlyItsSource()=runBlocking {
        val backend=Backend();var clock=1_000L
        MediaCompatibilityService(backend,now={clock}).use{service->
            val a=service.inspect(media("a"));val b=service.inspect(media("b"))
            service.verify(a);service.verify(b);assertEquals(2,backend.probes.get())
            service.verify(a);assertEquals(2,backend.probes.get())
            service.invalidate(a);service.verify(a);service.verify(b);assertEquals(3,backend.probes.get())
            service.recordFailure(a);assertTrue(service.failed(a));assertFalse(service.failed(b))
            clock+=60_001;assertFalse(service.failed(a))
            clock+=300_001;service.verify(b);assertEquals(4,backend.probes.get())
        }
    }
    @Test fun closingServiceCancelsAnActiveProbeAndReleasesItsWorker()=runBlocking {
        val backend=Backend().apply{blocked=true};val service=MediaCompatibilityService(backend)
        val evidence=service.inspect(media())
        val work=launch{service.verify(evidence)}
        withTimeout(2_000){while(backend.live.get()==0)delay(5)}
        service.close();withTimeout(2_000){work.join();while(backend.live.get()!=0)delay(5)}
        assertEquals(0,backend.live.get())
    }
    @Test fun aLaterSourceFailureReleasesEarlierPreparedLeaseWithoutMutatingOriginals()=runBlocking {
        val sources=listOf(media("difficult"),media("not-supported"))
        val backend=object:CompatibilityBackend {
            override val deviceBackend="lease-cleanup"
            override suspend fun inspect(media:MediaReference)=SourceEvidence(SourceFormatDescriptor(media,"v","csd"),
                if(media.id=="difficult")listOf(DecoderCandidate("software",DeclaredSupport.UNSUPPORTED,software=true)) else emptyList(),
                if(media.id=="difficult")DeclaredSupport.UNSUPPORTED else DeclaredSupport.NO_CANDIDATE)
            override suspend fun probe(source:SourceFormatDescriptor,candidate:DecoderCandidate)=DecoderObservation(candidate.name,ProbeStatus.OUTPUT,1,0)
        }
        val store=Store();MediaCompatibilityService(backend).use{service->
            try{PreviewSourcePreparer(service,PreviewResourcePolicy(budget),store).prepare(RenderPlan.derive(project(sources)),PreviewQuality.BALANCED);fail("Expected source-specific failure")}
            catch(error:MediaCompatibilityException){assertEquals("not-supported",error.failure.sourceMediaId)}
        }
        assertEquals(1,store.generated);assertEquals(1,store.closed);assertTrue(sources.all{!it.uri.contains("prepared")})
    }

}
