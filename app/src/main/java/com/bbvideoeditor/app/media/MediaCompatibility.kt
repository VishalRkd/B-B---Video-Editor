package com.bbvideoeditor.app.media

import com.bbvideoeditor.core.*
import java.security.MessageDigest

enum class DeclaredSupport { SUPPORTED, UNSUPPORTED, UNKNOWN, QUERY_ERROR, NO_CANDIDATE }
enum class ProbeStatus { OUTPUT, CONFIGURATION_FAILED, DECODE_FAILED, NO_OUTPUT, TIMEOUT, RESOURCE_LIMIT, ACCESS_FAILED, CANCELLED }
enum class MediaFailureKind { ACCESS, MALFORMED, PROTECTED, NO_DECODER, DECODER_INIT, DECODE, PROBE_TIMEOUT, RESOURCE,
    GRAPH_LIMIT, ENCODER, STORAGE, OUTPUT_VERIFICATION, PREPARATION_TIMEOUT, AMBIGUOUS_GRAPH_DECODER_FAILURE, UNKNOWN }
enum class MediaRetry { NONE, RECHECK, REPROBE, PREPARE, RESOURCES }

data class PreviewFailure(val kind: MediaFailureKind, val sourceMediaId: String? = null,
    val userMessage: String, val technicalReason: String, val retry: MediaRetry = MediaRetry.NONE,
    val candidateSourceIds: Set<String> = emptySet(), val cause: Throwable? = null) {
    val canRetry get() = retry != MediaRetry.NONE
}
class MediaCompatibilityException(val failure: PreviewFailure) : Exception(failure.userMessage, failure.cause)
enum class PreparationStage { CHECKING, VERIFYING, CACHED, GENERATING }
data class MediaPreparation(val stage: PreparationStage, val sourceMediaId: String, val displayName: String,
    val progress: Int? = null)

/** Normalized, stable metadata only. Low-level CSD/MediaFormat remains inside the platform backend. */
data class SourceFormatDescriptor(val media: MediaReference, val version: String, val configurationDigest: String,
    val encrypted: Boolean? = null, val hasHdrStaticInfo: Boolean? = null) {
    val hdr get() = media.colorTransfer in listOf(6, 7) || hasHdrStaticInfo == true
    fun fingerprint(deviceBackend: String): String = compatibilityHash("compat-v4|$deviceBackend|${media.id}|${media.uri}|" +
        "${media.codecMimeType}|${media.width}|${media.height}|${media.rotationDegrees}|${media.frameRate}|${media.frameRateProvenance}|" +
        "${media.codecProfile}|${media.codecLevel}|${media.bitDepth}|${media.colorTransfer}|${media.colorStandard}|${media.colorRange}|" +
        "${media.durationUs}|${media.sizeBytes}|$version|$configurationDigest|$encrypted|$hasHdrStaticInfo")
}
data class DecoderCandidate(val name: String, val support: DeclaredSupport, val sizeSupported: Boolean? = null,
    val rateSupported: Boolean? = null, val fullFormatSupported: Boolean? = null, val hardware: Boolean? = null,
    val software: Boolean? = null, val vendor: Boolean? = null, val profiles: List<Pair<Int, Int>> = emptyList(),
    val queryError: String? = null)
data class SourceEvidence(val format: SourceFormatDescriptor, val candidates: List<DecoderCandidate>,
    val support: DeclaredSupport, val defaultDecoderName: String? = null, val queryError: String? = null) {
    val directReported get() = support == DeclaredSupport.SUPPORTED &&
        candidates.firstOrNull { it.name == defaultDecoderName }?.support == DeclaredSupport.SUPPORTED
}
data class DecoderObservation(val decoder: String, val status: ProbeStatus, val samples: Int = 0,
    val outputTimeUs: Long? = null, val detail: String? = null, val cause: Throwable? = null)
data class ProbeEvidence(val attempts: List<DecoderObservation>, val exhausted: Boolean) {
    val success get() = attempts.firstOrNull { it.status == ProbeStatus.OUTPUT }
    val transient get() = attempts.any { it.status in listOf(ProbeStatus.TIMEOUT, ProbeStatus.RESOURCE_LIMIT, ProbeStatus.ACCESS_FAILED) }
}
interface CompatibilityBackend {
    val deviceBackend: String
    suspend fun inspect(media: MediaReference): SourceEvidence
    suspend fun probe(source: SourceFormatDescriptor, candidate: DecoderCandidate): DecoderObservation
}

/** Close owns leases, never original media. Combined ownership remains idempotent. */
data class ResolvedPreviewMedia(val sources: Map<String, MediaReference>, private val release: () -> Unit) : AutoCloseable {
    private val closed = java.util.concurrent.atomic.AtomicBoolean()
    override fun close() { if (closed.compareAndSet(false, true)) release() }
    companion object {
        fun combine(parts: List<ResolvedPreviewMedia>) = ResolvedPreviewMedia(parts.flatMap { it.sources.entries }.associate { it.toPair() }) {
            parts.asReversed().forEach { it.close() }
        }
    }
}
interface OptimizedMediaStore {
    /** Invalidate derived media only after playback has relinquished its lease. */
    suspend fun invalidate(project: Project, mediaId: String)
    suspend fun cached(project: Project, mediaId: String): ResolvedPreviewMedia?
    suspend fun resolve(project: Project, requiredMediaIds: Set<String>, preferredDecoders: Map<String, String> = emptyMap(),
        onProgress: suspend (MediaPreparation) -> Unit = {}): ResolvedPreviewMedia
}
internal fun compatibilityHash(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

internal fun sourceFailure(media: MediaReference, kind: MediaFailureKind, detail: String,
    retry: MediaRetry = MediaRetry.NONE, cause: Throwable? = null): MediaCompatibilityException {
    val message = when (kind) {
        MediaFailureKind.ACCESS -> "Can't access this video. Restore access to ${media.displayName}."
        MediaFailureKind.PROBE_TIMEOUT, MediaFailureKind.PREPARATION_TIMEOUT -> "Preparing ${media.displayName} took too long. You can try again."
        MediaFailureKind.RESOURCE -> "Not enough available resources to prepare ${media.displayName}. Close other playback and try again."
        MediaFailureKind.STORAGE -> "Not enough available storage to prepare ${media.displayName}."
        MediaFailureKind.ENCODER, MediaFailureKind.OUTPUT_VERIFICATION -> "Couldn't prepare ${media.displayName} for editing. You can try again."
        MediaFailureKind.UNKNOWN -> "Couldn't load ${media.displayName} right now. Check the source and try again."
        MediaFailureKind.DECODE, MediaFailureKind.DECODER_INIT -> if (retry == MediaRetry.NONE) "${media.displayName} can't be edited on this device."
            else "Couldn't prepare ${media.displayName} for editing. You can try again."
        else -> "${media.displayName} can't be edited on this device."
    }
    return MediaCompatibilityException(PreviewFailure(kind, media.id, message, detail, retry, cause = cause))
}
