package com.videoeditorpro.android.domain.model

import java.util.UUID

/**
 * Represents a background music or sound effect segment on the project timeline.
 */
data class AudioClipItem(
    val id: String = UUID.randomUUID().toString(),
    val sourcePath: String,
    val title: String,
    val timelineStartMs: Int = 0,
    val sourceDurationMs: Int,
    val trimStartMs: Int = 0,
    val trimEndMs: Int = sourceDurationMs,
    val volume: Int = 100, // 0 to 200
    val isMuted: Boolean = false
) {
    val durationMs: Int
        get() = (trimEndMs - trimStartMs).coerceAtLeast(100)

    val timelineEndMs: Int
        get() = timelineStartMs + durationMs
}
