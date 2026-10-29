package com.bbvideoeditor.app.playback

import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.Choreographer
import androidx.annotation.MainThread
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MultipleInputVideoGraph
import androidx.media3.effect.SingleInputVideoGraph
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.transformer.CompositionPlayer
import com.bbvideoeditor.app.render.*
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.media.OptimizedPreviewMedia
import com.bbvideoeditor.app.media.ResolvedPreviewMedia
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** SUBMITTED means accepted by the surface writer, not physical display scanout. */
enum class PreviewPhase { EMPTY, COMPILING, REQUESTED, APPLIED, BUFFERING, SUBMITTED, PLAYING, SUSPENDED, FAILED }
data class PreviewState(
    val positionUs: Long = 0,
    val playing: Boolean = false,
    val playWhenReady: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    val player: Player? = null,
    val warnings: List<String> = emptyList(),
    val quality: PreviewQuality = PreviewQuality.BALANCED,
    val appliedPositionUs: Long? = null,
    val submittedPositionUs: Long? = null,
    val requestedGeneration: Long = 0,
    val appliedGeneration: Long = 0,
    val submittedGeneration: Long = 0,
    val graphRevision: Long = 0,
    val phase: PreviewPhase = PreviewPhase.EMPTY,
    val admission: PreviewAdmission? = null,
)

data class PreviewSurfaceState(val player: Player?, val buffering: Boolean, val error: String?,
    val effectiveQuality:PreviewQuality=PreviewQuality.BALANCED,val optimized:Boolean=false)

/** Main owns player calls and small request admission. Immutable plan/graph preparation runs on
 * workers. A frame-paced mailbox owns one in-flight seek and the newest pending target. */
@UnstableApi
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
@MainThread
class PreviewEngine(
    context: Context,
    private val compiler: CompositionCompiler,
    initialPositionUs: Long = 0,
    initialQuality: PreviewQuality = PreviewQuality.BALANCED,
    private val renderTextInPlayer: Boolean = true,
) : AutoCloseable {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PreviewState(positionUs = initialPositionUs.coerceAtLeast(0), quality = initialQuality))
    val state: StateFlow<PreviewState> = mutableState.asStateFlow()
    val surface: StateFlow<PreviewSurfaceState> = state.map { PreviewSurfaceState(it.player,it.buffering,it.error,it.admission?.quality ?: it.quality,it.admission?.kind==PreviewAdmissionKind.OPTIMIZED_MEDIA_REQUIRED) }
        .distinctUntilChanged().stateIn(scope,SharingStarted.Eagerly,PreviewSurfaceState(null,false,null))
    val diagnostics = PreviewDiagnostics()
    private val requests = PreviewRequests()
    private val policy=(this.context as? EditorApplication)?.previewPolicy ?: PreviewResourcePolicy(this.context,EngineResourceBudget.inspect(this.context))
    private val optimizer=(this.context as? EditorApplication)?.optimizedMedia ?: OptimizedPreviewMedia(this.context,compiler)
    private var optimized:ResolvedPreviewMedia?=null
    private var project: Project? = null
    private var durationUs = 0L
    private var player: CompositionPlayer? = null
    private var pipeline: OutputFramePipeline? = null
    private var binding: RuntimeRenderBinding? = null
    private var compileJob: Job? = null
    private var clockJob: Job? = null
    private var seekPreparationJob: Job? = null
    private var acknowledgementJob: Job? = null
    private var generation = 0L
    private var installedRevision = -1L
    private var submittedRevision = -1L
    private var redrawParameters = false
    private var awaitingInstalledFrame = false
    private var executorEpoch = 0L
    private var active = true
    private var closed = false
    private var requestedPlaying = false
    private var requiresCompositor = true
    private var segmentIds: List<String?> = emptyList()
    private var framePosted = false
    private var convergenceRetries = 0
    private var sourceCorrections=0
    private var recoveryAnchor=false
    private var flushSeekGate=false
    private var outputWidth = 0
    private var outputHeight = 720
    private var lastTextWindow = Long.MIN_VALUE
    private val textWindows = kotlinx.coroutines.channels.Channel<Triple<RenderPlan,Long,Int>>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val textWorker = scope.launch { for((plan,time,height) in textWindows) if(active)compiler.prefetchText(plan,time,height) }
    private val choreographer by lazy { Choreographer.getInstance() }
    private val admission = Choreographer.FrameCallback {
        framePosted = false
        applyNewestRequest()
    }
    fun updateProject(project: Project) {
        checkMainThread()
        if (closed) return
        if (this.project == project) return
        val previous = this.project
        this.project = project
        durationUs = project.durationUs
        mutableState.value = state.value.copy(positionUs = state.value.positionUs.coerceIn(0, project.durationUs))
        // Names, history timestamps, selection and locks do not change rendered content.
        if (previous != null && previous.canvas == project.canvas && previous.media == project.media &&
            previous.timeline.transitions == project.timeline.transitions &&
            previous.timeline.tracks.size == project.timeline.tracks.size &&
            previous.timeline.tracks.zip(project.timeline.tracks).all { (a, b) ->
                a.type == b.type && a.muted == b.muted && a.clips == b.clips
            }) return
        // The editor's canvas adapter renders the shared text bitmaps directly for under-finger
        // movement. Text-only changes do not require a decoder rebuild while duration is stable.
        if (!renderTextInPlayer && previous != null && previous.canvas == project.canvas &&
            previous.durationUs == project.durationUs && previous.media == project.media &&
            previous.timeline.tracks.filter { it.type != TrackType.TEXT } == project.timeline.tracks.filter { it.type != TrackType.TEXT } &&
            previous.timeline.transitions == project.timeline.transitions &&
            project.timeline.tracks.filter { it.type == TrackType.TEXT }.flatMap { it.clips }.all {
                it.effects.none { effect -> effect.enabled } && it.properties.isEmpty()
            }) return
        if (active) compileProject()
    }

    fun setQuality(quality: PreviewQuality) {
        checkMainThread()
        if (closed || state.value.quality == quality) return
        mutableState.value = state.value.copy(quality = quality)
        if (active) compileProject(forceCompile = true)
    }
    fun resume() {
        checkMainThread()
        if (closed || (active && player != null && state.value.error == null)) return
        active = true
        compileProject(forceCompile = true)
    }
    fun play() {
        checkMainThread()
        if (!active || closed || durationUs == 0L) return
        requestedPlaying = true
        mutableState.value = state.value.copy(playWhenReady = true)
        if (state.value.positionUs >= durationUs) seek(0)
        if (state.value.error != null) compileProject(forceCompile = true)
        else if (installedRevision == generation && !requests.busy) player?.play()
    }
    fun pause() {
        checkMainThread()
        requestedPlaying = false
        player?.pause()
        mutableState.value = state.value.copy(playing = false, playWhenReady = false)
    }
    fun seek(positionUs: Long) {
        checkMainThread()
        if (closed) return
        val begin = System.nanoTime()
        val target = positionUs.coerceIn(0, durationUs)
        val request = requests.offer(target, generation)
        diagnostics.requests++
        diagnostics.coalesced = requests.coalesced
        diagnostics.record(EngineEvent("SeekRequested",generation,request.id,target))
        convergenceRetries = 0
        mutableState.value = state.value.copy(positionUs = target, requestedGeneration = request.id,
            phase = PreviewPhase.REQUESTED, buffering = active, error=null)
        postAdmission()
        diagnostics.maxOfferNs = maxOf(diagnostics.maxOfferNs, System.nanoTime()-begin)
    }
    private fun postAdmission() {
        if (framePosted || awaitingInstalledFrame || !active || closed || installedRevision != generation || requests.inFlight != null) return
        framePosted = true
        choreographer.postFrameCallback(admission)
    }
    private fun applyNewestRequest() {
        val output = player ?: return
        if (!active || closed || installedRevision != generation) return
        val request = requests.take(generation) ?: return
        sourceCorrections=0;recoveryAnchor=false
        if(submittedRevision==generation && state.value.submittedPositionUs==request.positionUs && !redrawParameters) {
            // Reuse an acknowledged surface submission. Seeking an unchanged paused position
            // would flush Media3's input reader although its decoder has no new frame to emit.
            requests.submitted(generation,request.positionUs,0)
            mutableState.value=state.value.copy(appliedPositionUs=request.positionUs,appliedGeneration=request.id,
                submittedGeneration=request.id,buffering=false,phase=PreviewPhase.SUBMITTED)
            diagnostics.record(EngineEvent("SubmittedFrameReused",generation,request.id,request.positionUs))
            if(requestedPlaying)output.play()
            return
        }
        seekPreparationJob=scope.launch {
            val plan=binding?.plan
            if(renderTextInPlayer && plan!=null)compiler.prefetchText(plan,request.positionUs,outputHeight)
            if(!active || generation!=request.revision || player!==output)return@launch
            if(requests.pending!=null){requests.abandon();postAdmission();return@launch}
            applyNativeRequest(output,request)
        }
    }
    private fun applyNativeRequest(output:CompositionPlayer, request:PreviewRequest) {
        val plan = binding?.plan
        val segments = plan?.visuals?.map { it.clipAt(request.positionUs)?.id }.orEmpty()
        if (pipeline == null && requiresCompositor && (segments != segmentIds || plan?.visuals?.any{it.clipAt(request.positionUs)?.properties?.isNotEmpty()==true}==true)) {
            // Public synchronized packets require API 28. The legacy API 26/27 compatibility
            // path retains its explicit boundary recovery rather than showing stale layers.
            diagnostics.recoveries++
            releasePlayer()
            compileProject(forceCompile = true)
            return
        }
        segmentIds = segments
        if(output.playWhenReady)output.pause() // Play intent is reapplied after completion.
        val redraw=redrawParameters && pipeline!=null && state.value.submittedPositionUs==request.positionUs
        redrawParameters=false
        if(!redraw)diagnostics.seeks++
        diagnostics.record(EngineEvent("SeekApplied",generation,request.id,request.positionUs))
        mutableState.value = state.value.copy(appliedPositionUs = request.positionUs,
            appliedGeneration = request.id, phase = PreviewPhase.BUFFERING)
        val nativeStart=System.nanoTime()
        if(redraw){diagnostics.redraws++;output.experimentalRedrawLastFrame()}
        else if(pipeline!=null && flushSeekGate) {
            flushSeekGate=false
            seekRecovery(output,request.positionUs.coerceAtMost((durationUs-1).coerceAtLeast(0)))
        }
        else output.seekTo(request.positionUs.coerceAtMost((durationUs-1).coerceAtLeast(0))/1000)
        diagnostics.nativeCall(System.nanoTime()-nativeStart)
        if(pipeline==null) {
            // Legacy metadata is source/renderer scheduled time, not final output submission.
            // Keep that weaker API 26/27 guarantee explicit rather than manufacturing a receipt.
            requests.abandon()
            mutableState.value=state.value.copy(buffering=output.playbackState==Player.STATE_BUFFERING,phase=PreviewPhase.APPLIED)
            postAdmission()
            if(requestedPlaying)output.play()
            return
        }
        armAcknowledgement(request)
    }
    private fun armAcknowledgement(request:PreviewRequest) {
        acknowledgementJob?.cancel()
        acknowledgementJob = scope.launch {
            var waited=0
            while(waited<2_500) {
                delay(100);waited+=100
                if(requests.inFlight!=request || !active || installedRevision!=request.revision)return@launch
                if(requests.pending!=null) {
                    // An obsolete blocked sample must not hold the newest finger target for the
                    // full failure timeout. This cancels only our request; the next public seek
                    // explicitly flushes Media3's own seek debounce gate on the same executor.
                    requests.abandon();recoveryAnchor=false;flushSeekGate=true;postAdmission();return@launch
                }
            }
            if (requests.inFlight != request || !active || installedRevision != request.revision) return@launch
            diagnostics.record(EngineEvent("AcknowledgementTimeout",generation,request.id,request.positionUs))
            if (requests.pending != null) {requests.abandon();recoveryAnchor=false;flushSeekGate=true;postAdmission()}
            else if (++convergenceRetries <= 2) {
                diagnostics.recoveries++
                // A seek to the same paused target can flush its reader without making a
                // decoder emit a replacement. Move to a distinct sample, then return to the
                // exact latest target after its receipt, keeping this request in flight.
                recoveryAnchor=true
                val step=maxOf(100_000L,(project?.canvas?.frameDurationUs ?: 33_333L)*2)
                val anchor=if(request.positionUs+step<durationUs)request.positionUs+step else (request.positionUs-step).coerceAtLeast(0)
                val start=System.nanoTime()
                diagnostics.seeks++;player?.let{seekRecovery(it,anchor)};diagnostics.nativeCall(System.nanoTime()-start)
                mutableState.value=state.value.copy(appliedPositionUs=anchor,buffering=true,phase=PreviewPhase.BUFFERING)
                armAcknowledgement(request)
            } else {
                mutableState.value = state.value.copy(buffering=false, phase=PreviewPhase.FAILED,
                    error="Preview did not acknowledge the requested frame. Retry playback or use a lower preview quality.")
            }
        }
    }
    private fun recoverSource(epoch:Long,mismatch:FrameInputMismatch) {
        if(!active || closed || epoch!=executorEpoch || mismatch.revision!=generation)return
        diagnostics.record(EngineEvent("InputMismatch:${mismatch.sequence}:${mismatch.item}",generation,timeUs=mismatch.timeUs))
        if(!awaitingInstalledFrame && !requests.busy)return
        if(awaitingInstalledFrame){awaitingInstalledFrame=false;acknowledgementJob?.cancel()}
        if(requests.inFlight==null) {
            if(requests.pending==null)requests.offer(state.value.positionUs,generation)
            requests.take(generation)
        }
        val request=requests.inFlight ?: return
        if(requests.pending!=null){requests.abandon();recoveryAnchor=false;flushSeekGate=true;postAdmission();return}
        if(++sourceCorrections>2){fail("Preview could not obtain the correct source frame. Retry preview.");return}
        diagnostics.recoveries++
        recoveryAnchor=true
        val anchor=if(sourceCorrections%2==1 && request.positionUs+1_000<durationUs)request.positionUs+1_000
            else (request.positionUs-1_000).coerceAtLeast(0)
        val output=player ?: return
        val start=System.nanoTime()
        diagnostics.seeks++;seekRecovery(output,anchor);diagnostics.nativeCall(System.nanoTime()-start)
        mutableState.value=state.value.copy(appliedPositionUs=anchor,buffering=true,phase=PreviewPhase.BUFFERING)
        armAcknowledgement(request)
    }
    private fun seekRecovery(output:CompositionPlayer,timeUs:Long) {
        // Media3 also debounces seeks until an output is processed. A rejected input or a lost
        // paused replacement cannot clear that gate. Ending its public scrubbing mode flushes
        // that pending seek without replacing/preparing the player or touching private state.
        if(output.playWhenReady)output.pause()
        output.setScrubbingModeEnabled(true)
        output.seekTo(timeUs/1000)
        output.setScrubbingModeEnabled(false)
    }
    private fun onSubmitted(epoch: Long, frame: FrameSubmission) {
        if (!active || closed || epoch != executorEpoch) return
        diagnostics.record(EngineEvent("OutputSubmitted",frame.revision ?: -1,timeUs=frame.timeUs))
        if (installedRevision != generation || frame.revision != generation) return
        if(awaitingInstalledFrame){awaitingInstalledFrame=false;acknowledgementJob?.cancel();acknowledgementJob=null}
        submittedRevision=generation
        diagnostics.frames++
        if(recoveryAnchor) {
            recoveryAnchor=false
            flushSeekGate=true
            if(requests.pending!=null){requests.abandon();postAdmission();return}
            requests.inFlight?.let{applyNativeRequest(requireNotNull(player),it)}
            return
        }
        mutableState.value = state.value.copy(submittedPositionUs=frame.timeUs)
        // Decoder cadence may differ from output cadence. The request is still represented by
        // output time; exact pixel/source identity is independently checked by conformance tests.
        val tolerance = maxOf(2_000L, (project?.canvas?.frameDurationUs ?: 33_333L)+1_000L)
        val completed = requests.submitted(generation,frame.timeUs,tolerance)
        if (completed != null) {
            acknowledgementJob?.cancel(); acknowledgementJob=null
            diagnostics.record(EngineEvent("FrameSubmitted",generation,completed.id,frame.timeUs))
            mutableState.value = state.value.copy(submittedGeneration=completed.id,
                buffering=requests.pending != null, phase=PreviewPhase.SUBMITTED)
            if (requests.pending != null) postAdmission() else if (requestedPlaying) player?.play()
        } else if (requests.pending!=null && requests.inFlight==null) {
            postAdmission()
        } else if (!requests.busy) {
            mutableState.value = state.value.copy(buffering=false, phase=if(requestedPlaying) PreviewPhase.PLAYING else PreviewPhase.SUBMITTED)
        }
    }
    /** Foreground ownership ends here. A subsequent resume gets a fresh executor epoch. */
    fun suspendPlayback() {
        checkMainThread()
        if (!active && player == null) return
        if (!requests.busy && installedRevision == generation) player?.let {
            mutableState.value=state.value.copy(positionUs=it.currentPosition.times(1000).coerceIn(0,durationUs))
        }
        active=false; requestedPlaying=false; generation++
        compileJob?.cancel(); compileJob=null
        requests.clear()
        if(framePosted){choreographer.removeFrameCallback(admission);framePosted=false}
        releasePlayer()
        mutableState.value=state.value.copy(player=null,playing=false,playWhenReady=false,buffering=false,phase=PreviewPhase.SUSPENDED)
    }
    override fun close() {
        checkMainThread()
        if(closed)return
        suspendPlayback();closed=true;project=null;binding=null;scope.cancel()
    }
    private fun compileProject(forceCompile: Boolean = false) {
        if(!active || closed)return
        val snapshot=project ?: return
        durationUs=snapshot.durationUs
        val revision=++generation
        compileJob?.cancel(); seekPreparationJob?.cancel(); acknowledgementJob?.cancel(); requests.clear()
        player?.pause()
        if(durationUs==0L) {
            requestedPlaying=false;releasePlayer();binding=null
            mutableState.value=PreviewState(quality=state.value.quality)
            return
        }
        mutableState.value=state.value.copy(playing=false,buffering=true,error=null,graphRevision=revision,phase=PreviewPhase.COMPILING)
        compileJob=scope.launch {
            var unownedPipeline:OutputFramePipeline?=null
            var unownedSources:ResolvedPreviewMedia?=null
            try {
                delay(60) // Graph edit batching, independent of frame-paced transport admission.
                val plan=withContext(Dispatchers.Default){RenderPlan.derive(snapshot)}
                compiler.validate(plan)
                val existing=binding
                val parametersOnly=!forceCompile && existing != null && pipeline != null &&
                    withContext(Dispatchers.Default){ existing.acceptsParameters(plan) }
                if(parametersOnly) {
                    if(generation!=revision || !active)return@launch
                    existing!!.updateParameters(plan,revision)
                    pipeline?.expectedRevision=revision
                    redrawParameters=true
                    installedRevision=revision
                    diagnostics.parameters++
                    diagnostics.record(EngineEvent("ParametersUpdated",revision))
                    seek(state.value.positionUs)
                    return@launch
                }
                val admission=policy.assess(plan,state.value.quality)
                require(admission.kind!=PreviewAdmissionKind.UNSUPPORTED){admission.reasons.joinToString(" ")}
                diagnostics.record(EngineEvent("Admission:${admission.kind}",revision))
                var sources=emptyMap<String,MediaReference>()
                if(admission.kind==PreviewAdmissionKind.OPTIMIZED_MEDIA_REQUIRED) {
                    require(admission.declaredVideoInputs<=EngineResourceBudget.inspect(context).maximumVideoInputs) {
                        "This preview exceeds the device's safe number of video inputs. No layer was omitted."
                    }
                    if(player!=null && optimized==null) {
                        releasePlayer()
                        mutableState.value=state.value.copy(player=null)
                    }
                    unownedSources=optimizer.resolve(snapshot)
                    sources=unownedSources.sources
                    val optimizedProject=snapshot.copy(media=snapshot.media+sources)
                    val reassessed=policy.assess(withContext(Dispatchers.Default){RenderPlan.derive(optimizedProject)},PreviewQuality.FAST)
                    require(reassessed.admitted){"The optimized preview still exceeds this device's safe resource budget. No layer was omitted."}
                }
                if(generation!=revision || !active)return@launch
                val (width,height)=admission.quality.dimensions(snapshot.canvas)
                val compiled=compiler.compile(plan,width,height,
                    preferSingleVisualInput=Build.VERSION.SDK_INT<28,renderText=renderTextInPlayer,textPositionUs=state.value.positionUs,observeOutputRevision=true,previewSources=sources,normalizeAudioDuration=false)
                if(generation!=revision || !active)return@launch
                if(player!=null && (compiled.width!=outputWidth || compiled.height!=outputHeight)) {
                    releasePlayer();mutableState.value=state.value.copy(player=null)
                }
                if(player==null && Build.VERSION.SDK_INT>=28) {
                    val epoch=++executorEpoch
                    unownedPipeline=OutputFramePipeline.create(context,onInputMismatch={mismatch->
                        scope.launch{recoverSource(epoch,mismatch)}
                    }) { frame ->
                        scope.launch{onSubmitted(epoch,frame)}
                    }
                    if(generation!=revision || !active)return@launch
                    pipeline=unownedPipeline;unownedPipeline=null
                }
                if(player!=null && pipeline==null && (requiresCompositor || requiresCompositor!=compiled.requiresCompositor))releasePlayer()
                requiresCompositor=compiled.requiresCompositor
                val output=player ?: createPlayer().also{player=it}
                pipeline?.expectedRevision=revision
                compiled.binding.revision=revision
                binding=compiled.binding;installedRevision=revision;outputWidth=compiled.width;outputHeight=compiled.height;redrawParameters=false;awaitingInstalledFrame=true
                segmentIds=plan.visuals.map{it.clipAt(state.value.positionUs)?.id}
                diagnostics.installs++;diagnostics.prepares++;diagnostics.rebuilds++
                diagnostics.record(EngineEvent("CompositionPrepare",revision))
                val oldSources=optimized
                optimized=unownedSources;unownedSources=null
                val installStart=System.nanoTime()
                output.setComposition(compiled.composition,state.value.positionUs.coerceAtMost(durationUs-1)/1000)
                output.prepare()
                diagnostics.maxInstallNs=maxOf(diagnostics.maxInstallNs,System.nanoTime()-installStart)
                oldSources?.close()
                acknowledgementJob=scope.launch {
                    delay(10_000)
                    if(awaitingInstalledFrame && generation==revision && active)fail("Preview could not submit its initial frame. Retry with a lower preview quality.")
                }
                mutableState.value=state.value.copy(player=output,warnings=compiled.warnings+admission.reasons,error=null,admission=admission)
                // Compilation may have swallowed requests from an older revision; re-admit the
                // latest UI position after install, never an old decoder position.
                seek(state.value.positionUs)
                startClock()
            } catch(cancelled:CancellationException){throw cancelled}
              catch(error:Exception){if(generation==revision)fail(error.message ?: "This project cannot be previewed.")}
            finally{unownedPipeline?.close();unownedSources?.close()}
        }
    }
    private fun createPlayer():CompositionPlayer=CompositionPlayer.Builder(context).apply {
        val frames=pipeline
        if(Build.VERSION.SDK_INT>=28 && frames!=null) {
            setFrameProcessorFactory(frames.factory);setNativeHardwareBufferHelpers(androidx.media3.effect.HardwareBufferJni.INSTANCE)
        } else setVideoGraphFactory(if(requiresCompositor)MultipleInputVideoGraph.Factory() else SingleInputVideoGraph.Factory())
    }
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true)
        .build().apply {
            diagnostics.creates++
            val owner=this
            addAnalyticsListener(object:AnalyticsListener {
                override fun onVideoEnabled(eventTime:AnalyticsListener.EventTime,counters:androidx.media3.exoplayer.DecoderCounters) {
                    if(owner===player)diagnostics.observeCounters(counters)
                }
            })
            addListener(object:Player.Listener {
                override fun onPlaybackStateChanged(playbackState:Int) {
                    if(owner!==player || installedRevision!=generation || !active)return
                    if(pipeline==null && playbackState==Player.STATE_READY && awaitingInstalledFrame) {
                        awaitingInstalledFrame=false;acknowledgementJob?.cancel();postAdmission()
                    }
                    if(playbackState==Player.STATE_BUFFERING)diagnostics.buffering++
                    if(playbackState==Player.STATE_ENDED && !requests.busy) {
                        requestedPlaying=false
                        mutableState.value=state.value.copy(positionUs=durationUs)
                    }
                    publishPlayerState()
                }
                override fun onPlayerError(error:PlaybackException) {
                    if(owner===player && active) {
                        if(error.errorCode==PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)project?.let(policy::recordFailure)
                        fail("Preview failed (${error.errorCodeName}). ${error.cause?.cause?.message ?: error.cause?.message ?: error.message.orEmpty()}")
                    }
                }
            })
            if(pipeline==null) {
                // Strongest legacy public signal: scheduled frame metadata, not display scanout.
                setVideoFrameMetadataListener { timeUs,_,_,_ ->
                    scope.launch{diagnostics.record(EngineEvent("LegacyFrameScheduled",installedRevision,timeUs=timeUs))}
                }
            }
        }
    private fun startClock() {
        if(clockJob?.isActive==true)return
        clockJob=scope.launch{while(isActive && active){publishPlayerState();delay(if(player?.isPlaying==true)33 else 100)}}
    }
    private fun publishPlayerState() {
        val output=player ?: return
        if(installedRevision!=generation || !active)return
        val old=state.value
        if(renderTextInPlayer && old.positionUs/500_000 != lastTextWindow) {
            lastTextWindow=old.positionUs/500_000
            binding?.plan?.takeIf{it.text.isNotEmpty()}?.let{textWindows.trySend(Triple(it,old.positionUs,outputHeight))}
        }
        val position=if(requests.busy || !requestedPlaying)old.positionUs else {
            val clock=output.currentPosition.times(1000).coerceIn(0,durationUs)
            // The audio/player clock can run ahead while a required visual input is late.
            // During natural playback, do not claim a playhead beyond the latest composition
            // accepted by the writer. Seek intent remains immediate and independent above.
            val accepted=if(pipeline!=null)old.submittedPositionUs ?: old.positionUs else clock
            maxOf(old.positionUs,minOf(clock,accepted))
        }
        if(pipeline==null && requiresCompositor && output.isPlaying) {
            val next=binding?.plan?.visuals?.map{it.clipAt(position)?.id}.orEmpty()
            if(next.indices.any{next[it]!=null && next[it]!=segmentIds.getOrNull(it)}){seek(position);return}
        }
        if(position>=durationUs && requestedPlaying){requestedPlaying=false;output.pause()}
        mutableState.value=old.copy(positionUs=position,playing=output.isPlaying,playWhenReady=requestedPlaying,
            buffering=requests.busy || output.playbackState==Player.STATE_BUFFERING)
    }
    private fun fail(message:String) {
        requestedPlaying=false;requests.clear();releasePlayer()
        mutableState.value=state.value.copy(player=null,playing=false,playWhenReady=false,buffering=false,error=message,phase=PreviewPhase.FAILED)
    }
    private fun releasePlayer() {
        seekPreparationJob?.cancel();seekPreparationJob=null
        clockJob?.cancel();clockJob=null;acknowledgementJob?.cancel();acknowledgementJob=null
        requests.abandon()
        val previous=player;player=null;installedRevision=-1;submittedRevision=-1;redrawParameters=false;awaitingInstalledFrame=false;flushSeekGate=false
        if(previous!=null){diagnostics.releases++;previous.release()}
        pipeline?.close();pipeline=null;optimized?.close();optimized=null;executorEpoch++
    }
    private fun checkMainThread(){check(Looper.myLooper()==Looper.getMainLooper()){ "PreviewEngine must be accessed on the main thread." }}
}
