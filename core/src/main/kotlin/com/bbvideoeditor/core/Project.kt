package com.bbvideoeditor.core

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.roundToLong

const val MICROS_PER_SECOND: Long = 1_000_000L
const val CURRENT_PROJECT_SCHEMA = 2

fun newId(): String = UUID.randomUUID().toString()

/** All edit times use signed 64-bit microseconds. Source media is never modified. */
@Serializable
data class TimeRange(val startUs: Long = 0, val durationUs: Long) {
    init {
        require(startUs >= 0) { "A time range cannot start before zero" }
        require(durationUs > 0) { "A time range must have positive duration" }
        require(startUs <= Long.MAX_VALUE - durationUs) { "Time range exceeds supported duration" }
    }
    val endUs: Long get() = startUs + durationUs
    operator fun contains(timeUs: Long): Boolean = timeUs >= startUs && timeUs < endUs
}

@Serializable
enum class MediaKind { VIDEO, AUDIO, IMAGE }

@Serializable
data class MediaReference(
    val id: String = newId(),
    val uri: String,
    val displayName: String,
    val kind: MediaKind,
    val durationUs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val rotationDegrees: Int = 0,
    val frameRate: Float = 0f,
    val hasAudio: Boolean = false,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
) {
    init {
        require(id.isNotBlank() && uri.isNotBlank()) { "Media requires an identifier and URI" }
        require(durationUs >= 0 && width >= 0 && height >= 0 && sizeBytes >= 0)
        require(frameRate.isFinite() && frameRate >= 0)
    }
    val displayWidth: Int get() = if (rotationDegrees.mod(180) == 90) height else width
    val displayHeight: Int get() = if (rotationDegrees.mod(180) == 90) width else height
}

/** Translation uses GL normalized coordinates (-1 to +1 reaches the canvas edges); rotation is counterclockwise. */
@Serializable
data class Transform(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val opacity: Float = 1f,
) {
    init {
        require(scaleX.isFinite() && scaleY.isFinite() && scaleX > 0 && scaleY > 0)
        require(rotationDegrees.isFinite() && translationX.isFinite() && translationY.isFinite())
        require(opacity.isFinite() && opacity in 0f..1f)
    }
}

@Serializable
enum class Interpolation { HOLD, LINEAR }

/** Source-time keyframes survive trims and splits without retiming or loss of precision. */
@Serializable
data class Keyframe(val sourceTimeUs: Long, val value: Float, val interpolation: Interpolation = Interpolation.LINEAR) {
    init { require(sourceTimeUs >= 0 && value.isFinite()) }
}

@Serializable
data class AnimatedProperty(val name: String, val value: Float, val keyframes: List<Keyframe> = emptyList()) {
    init {
        require(name.isNotBlank() && value.isFinite())
        require(keyframes.zipWithNext().all { (left, right) -> left.sourceTimeUs < right.sourceTimeUs })
    }
    fun valueAt(sourceTimeUs: Long): Float {
        if (keyframes.isEmpty()) return value
        val next = keyframes.binarySearch { it.sourceTimeUs.compareTo(sourceTimeUs) }
        if (next >= 0) return keyframes[next].value
        val insertion = -next - 1
        if (insertion == 0) return keyframes.first().value
        if (insertion == keyframes.size) return keyframes.last().value
        val left = keyframes[insertion - 1]
        val right = keyframes[insertion]
        if (left.interpolation == Interpolation.HOLD) return left.value
        val fraction = (sourceTimeUs - left.sourceTimeUs).toDouble() / (right.sourceTimeUs - left.sourceTimeUs)
        return (left.value.toDouble() + (right.value.toDouble() - left.value.toDouble()) * fraction).toFloat()
    }
}

/** Backend extensions register a factory by type. Unknown effects must not be silently discarded. */
interface EffectDescriptor { val type: String }

@Serializable
data class EffectSpec(
    val id: String = newId(),
    override val type: String,
    val parameters: Map<String, Float> = emptyMap(),
    val properties: List<AnimatedProperty> = emptyList(),
    val enabled: Boolean = true,
) : EffectDescriptor

interface TransitionDescriptor { val type: String; val durationUs: Long }

@Serializable
data class TransitionSpec(
    val id: String = newId(),
    override val type: String,
    override val durationUs: Long,
    val fromClipId: String,
    val toClipId: String,
    val parameters: Map<String, Float> = emptyMap(),
) : TransitionDescriptor

@Serializable
data class Clip(
    val id: String = newId(),
    val mediaId: String? = null,
    val startUs: Long,
    val sourceRange: TimeRange,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val transform: Transform = Transform(),
    val effects: List<EffectSpec> = emptyList(),
    val properties: List<AnimatedProperty> = emptyList(),
    /** Exact split boundary; rounding is bounded by one source microsecond plus integer time rounding. */
    val durationOverrideUs: Long? = null,
    /** Carries fractional source microseconds through repeated splits, preventing cumulative drift. */
    val sourceRoundingOffsetUs: Double = 0.0,
    val text: TextContent? = null,
) {
    init {
        require(id.isNotBlank())
        require((mediaId != null) != (text != null)) { "A clip must contain either media or text" }
        require(mediaId == null || mediaId.isNotBlank())
        require(text == null || (speed == 1f && volume == 1f)) { "Text uses its timeline duration directly" }
        require(startUs >= 0) { "A clip cannot start before the timeline" }
        require(speed.isFinite() && speed in 0.1f..8f) { "Speed must be between 0.1 and 8" }
        require(volume.isFinite() && volume in 0f..4f) { "Volume must be between 0 and 4" }
        require(sourceRange.durationUs.toDouble() / speed < Long.MAX_VALUE.toDouble())
        require(sourceRoundingOffsetUs.isFinite() && sourceRoundingOffsetUs in -0.500001..0.500001)
        require(durationOverrideUs == null ||
            (durationOverrideUs > 0 && kotlin.math.abs(durationOverrideUs - (sourceRange.durationUs.toDouble() / speed).toLong()) <= kotlin.math.ceil(1.0 / speed).toLong() + 1))
        require(durationUs > 0 && startUs <= Long.MAX_VALUE - durationUs)
    }
    val durationUs: Long get() = durationOverrideUs ?: (sourceRange.durationUs.toDouble() / speed).toLong()
    val endUs: Long get() = startUs + durationUs
    fun sourceTimeAt(timelineTimeUs: Long): Long =
        (sourceRange.startUs + ((timelineTimeUs.coerceIn(startUs, endUs) - startUs).toDouble() * speed + sourceRoundingOffsetUs).roundToLong())
            .coerceIn(sourceRange.startUs, sourceRange.endUs)
    fun timelineTimeAt(sourceTimeUs: Long): Long =
        (startUs + (((sourceTimeUs.coerceIn(sourceRange.startUs, sourceRange.endUs) - sourceRange.startUs).toDouble() - sourceRoundingOffsetUs) / speed).roundToLong())
            .coerceIn(startUs, endUs)
}

@Serializable
enum class TrackType {
    VIDEO, AUDIO, OVERLAY, TEXT;
    fun accepts(kind: MediaKind): Boolean = when (this) {
        VIDEO, OVERLAY -> kind == MediaKind.VIDEO || kind == MediaKind.IMAGE
        AUDIO -> kind == MediaKind.AUDIO || kind == MediaKind.VIDEO
        TEXT -> false
    }
    fun accepts(clip: Clip, media: Map<String, MediaReference>): Boolean =
        if (clip.text != null) this == TEXT else media[clip.mediaId]?.let { accepts(it.kind) } == true
}

@Serializable
data class Track(
    val id: String = newId(),
    val name: String,
    val type: TrackType,
    val clips: List<Clip> = emptyList(),
    val muted: Boolean = false,
    val locked: Boolean = false,
) {
    val durationUs: Long get() = clips.maxOfOrNull { it.endUs } ?: 0L
}

@Serializable
data class Timeline(
    val tracks: List<Track> = defaultTracks(),
    val transitions: List<TransitionSpec> = emptyList(),
) {
    val durationUs: Long get() = tracks.maxOfOrNull { it.durationUs } ?: 0L
    val clips: List<Clip> get() = tracks.flatMap { it.clips }
    companion object {
        fun defaultTracks(): List<Track> = listOf(
            Track(name = "Video", type = TrackType.VIDEO),
            Track(name = "Overlay", type = TrackType.OVERLAY),
            Track(name = "Audio", type = TrackType.AUDIO),
        )
    }
}

@Serializable
data class CanvasSettings(val width: Int = 1920, val height: Int = 1080, val frameRate: Int = 30) {
    init {
        require(width in 2..8192 && height in 2..8192 && width % 2 == 0 && height % 2 == 0)
        require(frameRate in 1..120)
    }
    val aspectRatio: Float get() = width.toFloat() / height
    val frameDurationUs: Long get() = MICROS_PER_SECOND / frameRate
}

@Serializable
data class Project(
    val id: String = newId(),
    val name: String = "Untitled project",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val canvas: CanvasSettings = CanvasSettings(),
    val timeline: Timeline = Timeline(),
    val media: Map<String, MediaReference> = emptyMap(),
    val schemaVersion: Int = CURRENT_PROJECT_SCHEMA,
) {
    val durationUs: Long get() = timeline.durationUs
    fun findClip(id: String): Clip? = timeline.tracks.firstNotNullOfOrNull { track -> track.clips.firstOrNull { it.id == id } }
    fun findTrackForClip(id: String): Track? = timeline.tracks.firstOrNull { track -> track.clips.any { it.id == id } }
    fun findTrack(id: String): Track? = timeline.tracks.firstOrNull { it.id == id }

    /** Version 1 contains only media clips; their source timing is unchanged by this migration. */
    fun upgradeSchema(): Project {
        require(schemaVersion in 1..CURRENT_PROJECT_SCHEMA) { "Unsupported project schema $schemaVersion" }
        if (schemaVersion == 1) require(timeline.clips.none { it.text != null }) { "Text is not supported by schema 1" }
        return copy(schemaVersion = CURRENT_PROJECT_SCHEMA).validate()
    }

    /** Validate at the persistence and command boundaries. No overlapping clips on one track. */
    fun validate(): Project {
        require(schemaVersion == CURRENT_PROJECT_SCHEMA) { "Unsupported project schema $schemaVersion" }
        require(id.isNotBlank() && name.isNotBlank()) { "Project name and ID are required" }
        require(createdAt >= 0 && updatedAt >= 0)
        require(timeline.tracks.map { it.id }.distinct().size == timeline.tracks.size) { "Duplicate track ID" }
        val seen = mutableSetOf<String>()
        media.forEach { (key, ref) -> require(key == ref.id) { "Media key does not match its ID" } }
        timeline.tracks.forEach { track ->
            require(track.id.isNotBlank() && track.name.isNotBlank())
            val ordered = track.clips.sortedBy { it.startUs }
            ordered.forEach { clip ->
                require(seen.add(clip.id)) { "Duplicate clip ID" }
                require(track.type.accepts(clip, media)) { "Clip is incompatible with ${track.type}" }
                if (clip.text == null) {
                    val source = requireNotNull(media[clip.mediaId]) { "Clip references unknown media ${clip.mediaId}" }
                    if (source.kind != MediaKind.IMAGE) {
                        require(clip.sourceRange.endUs <= source.durationUs) { "Trim extends beyond source media" }
                    }
                }
            }
            require(ordered.zipWithNext().all { (left, right) -> left.endUs <= right.startUs }) {
                "Clips cannot overlap within ${track.name}; move the clip to another track"
            }
        }
        timeline.transitions.forEach { transition ->
            require(transition.durationUs > 0 && transition.fromClipId in seen && transition.toClipId in seen)
        }
        return this
    }
}
