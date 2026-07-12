package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Detailed metadata about a media file, extracted by [MediaEngine].
 * Extends beyond what MediaStore exposes (e.g., codec, bitrate, HDR info).
 */
data class MediaMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val bitrateBps: Long,
    val videoCodec: String?,
    val audioCodec: String?,
    val hasAudio: Boolean,
    val isHdr: Boolean,
    val rotation: Int,
)
