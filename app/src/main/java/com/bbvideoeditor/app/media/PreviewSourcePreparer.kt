package com.bbvideoeditor.app.media

import com.bbvideoeditor.app.playback.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.*

data class PreparedPreview(val lease: ResolvedPreviewMedia, val admission: PreviewAdmission,
    val evidence: Map<String, SourceEvidence>, val routes: Map<String, String>)

/** One finite preparation pass per graph. No recursive player retries and no editing semantics. */
class PreviewSourcePreparer(private val compatibility: MediaCompatibilityService, private val resources: PreviewResourcePolicy,
    private val store: OptimizedMediaStore) {
    suspend fun prepare(plan: RenderPlan, quality: PreviewQuality, forced: Set<String> = emptySet(), positionUs: Long = 0, probeIds: Set<String> = emptySet(),
        onStage: suspend (MediaPreparation?) -> Unit = {}, onEvent: (String, String) -> Unit = { _, _ -> }, onEvidence: (SourceEvidence) -> Unit = {},
        regenerate: Set<String> = emptySet()): PreparedPreview {
        val initial = resources.assess(plan, quality)
        if (initial.kind == PreviewAdmissionKind.UNSUPPORTED) throw MediaCompatibilityException(PreviewFailure(MediaFailureKind.GRAPH_LIMIT,
            userMessage = initial.reasons.joinToString(" "), technicalReason = "Static graph / peak-video budget"))
        val parts = mutableListOf<ResolvedPreviewMedia>(); val evidence = mutableMapOf<String, SourceEvidence>()
        val routes = mutableMapOf<String, String>(); val selected = mutableMapOf<String, MediaReference>()
        val active = plan.visuals.mapNotNull { it.clipAt(positionUs)?.mediaId }.toSet()
        val sources = plan.visualClips.mapNotNull { plan.project.media[it.mediaId] }.distinctBy { it.id }.sortedBy { if (it.id in active) 0 else 1 }
        try {
            for (original in sources) {
                currentCoroutineContext().ensureActive()
                val stage = { phase: PreparationStage -> MediaPreparation(phase, original.id, original.displayName) }
                if (original.kind == MediaKind.IMAGE) {
                    if (original.id in initial.requiredMediaIds) {
                        val part = store.cached(plan.project, original.id) ?: store.resolve(plan.project, setOf(original.id), onProgress = { onStage(it) })
                        parts += part; selected += part.sources; routes[original.id] = "OPTIMIZED"
                    }
                    continue
                }
                onStage(stage(PreparationStage.CHECKING)); val source = compatibility.inspect(original); evidence[original.id] = source; onEvidence(source)
                onEvent("COMPAT_QUERY_${source.support}", original.id)
                val softwareHeavy = source.candidates.firstOrNull{it.name==source.defaultDecoderName}?.software == true &&
                    (source.format.media.width.toLong()*source.format.media.height>1280L*720 || source.format.media.frameRate>60)
                val required = softwareHeavy || source.format.encrypted==true || original.id in initial.requiredMediaIds || original.id in forced || original.id in regenerate || compatibility.failed(source) || source.format.hdr || original.id in resources.assess(plan, quality, mapOf(original.id to source.format.media)).requiredMediaIds
                if (source.directReported && !required && original.id !in probeIds) { selected[original.id] = source.format.media; routes[original.id] = "ORIGINAL_REPORTED"; continue }
                onStage(stage(PreparationStage.CACHED))
                if (original.id in regenerate) store.invalidate(plan.project, original.id)
                val cached = store.cached(plan.project, original.id)
                if (cached != null) {
                    parts += cached; selected += cached.sources; routes[original.id] = "OPTIMIZED_CACHE"; onEvent("OPTIMIZED_CACHE_HIT", original.id); continue
                }
                if (source.format.encrypted == true) throw sourceFailure(original, MediaFailureKind.PROTECTED, "Protected video requires unavailable DRM ownership")
                if (source.candidates.isEmpty()) {
                    if (source.support == DeclaredSupport.NO_CANDIDATE) throw sourceFailure(original, MediaFailureKind.NO_DECODER, "No regular decoder candidate; no prepared cache")
                    throw sourceFailure(original, MediaFailureKind.RESOURCE, "Decoder enumeration unavailable", MediaRetry.REPROBE)
                }
                onStage(stage(PreparationStage.VERIFYING)); onEvent("DECODER_PROBE_START", original.id)
                val observed = compatibility.verify(source, if (original.id in active) AssetPriority.VISIBLE else AssetPriority.NEAR_VIEWPORT)
                onEvent(if (observed.success != null) "DECODER_PROBE_SUCCESS" else "DECODER_PROBE_FAIL", original.id)
                val success = observed.success
                val candidate = source.candidates.firstOrNull { it.name == success?.decoder }
                // Functional output is not a throughput benchmark. Software/alternate/HDR/high-cost routes normalize.
                val sameDefault = success != null && success.decoder == source.defaultDecoderName && candidate?.hardware == true
                if (sameDefault && !required && source.format.media.frameRate in 1f..60f) {
                    selected[original.id] = source.format.media; routes[original.id] = "ORIGINAL_OBSERVED"; continue
                }
                if (success == null && observed.transient) {
                    val first = observed.attempts.first { it.status in listOf(ProbeStatus.TIMEOUT, ProbeStatus.RESOURCE_LIMIT, ProbeStatus.ACCESS_FAILED) }
                    val kind = when (first.status) { ProbeStatus.TIMEOUT -> MediaFailureKind.PROBE_TIMEOUT; ProbeStatus.ACCESS_FAILED -> MediaFailureKind.ACCESS; else -> MediaFailureKind.RESOURCE }
                    throw sourceFailure(original, kind, first.detail ?: first.status.name, MediaRetry.REPROBE, first.cause)
                }
                // A surface-probe configuration failure is not universal impossibility. Try the supported
                // Transformer decoder route once; only corroborated decoding failures may become terminal.
                try {
                    onEvent("OPTIMIZED_GENERATION_START", original.id)
                    val part = store.resolve(plan.project, setOf(original.id), success?.let { mapOf(original.id to it.decoder) }.orEmpty(),
                        onProgress = { onStage(it) })
                    parts += part; selected += part.sources; routes[original.id] = "OPTIMIZED_GENERATED"; onEvent("OPTIMIZED_GENERATION_SUCCESS", original.id)
                } catch (error: MediaCompatibilityException) {
                    onEvent("OPTIMIZED_GENERATION_FAIL", original.id)
                    if (success == null && observed.exhausted && !observed.transient && error.failure.kind in listOf(MediaFailureKind.DECODER_INIT, MediaFailureKind.DECODE, MediaFailureKind.NO_DECODER)) {
                        onEvent("TERMINAL_UNSUPPORTED", original.id)
                        throw sourceFailure(original, error.failure.kind, "All bounded candidates and normalization decode failed", cause = error)
                    }
                    throw error
                }
            }
            val variants = parts.flatMap { it.sources.entries }.associate { it.toPair() }
            val effective = if (variants.isNotEmpty()) PreviewQuality.FAST else initial.quality
            val final = resources.assess(plan, effective, selected)
            if (!final.admitted) throw MediaCompatibilityException(PreviewFailure(MediaFailureKind.RESOURCE,
                userMessage = "These video layers exceed this device's preview memory limit. Use smaller sources or fewer simultaneous layers.",
                technicalReason = "Selected source graph still exceeds the fixed budget after preparation"))
            onStage(null)
            return PreparedPreview(ResolvedPreviewMedia.combine(parts), if (variants.isNotEmpty()) final.copy(kind = PreviewAdmissionKind.OPTIMIZED_MEDIA_REQUIRED,
                reasons = initial.reasons + "Using verified prepared video. Original audio and export are preserved.") else final, evidence, routes)
        } catch (error: Throwable) { parts.asReversed().forEach { it.close() }; throw error }
    }
}
