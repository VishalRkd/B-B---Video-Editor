package com.videoeditorpro.android.domain.model

import java.util.UUID

enum class CanvasAspectRatio(val ratioW: Int, val ratioH: Int, val label: String) {
    RATIO_16_9(16, 9, "16:9 Landscape"),
    RATIO_9_16(9, 16, "9:16 Portrait (Reels/TikTok)"),
    RATIO_1_1(1, 1, "1:1 Square"),
    RATIO_4_5(4, 5, "4:5 Portrait Post")
}

/**
 * Top-level immutable domain container representing an entire editing project.
 * Pure data model decoupled from JNI, easily serializable to JSON for draft saves.
 */
data class EditorProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Project",
    val videoClips: List<VideoClipItem> = emptyList(),
    val audioClips: List<AudioClipItem> = emptyList(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val aspectRatio: CanvasAspectRatio = CanvasAspectRatio.RATIO_16_9,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) {
    /**
     * Total project timeline duration summed across all sequential video clips.
     */
    val totalDurationMs: Int
        get() = videoClips.sumOf { it.effectiveDurationMs }

    /**
     * Resolves which clip is at the given timeline timestamp,
     * along with the timestamp relative to the start of that clip.
     */
    fun findClipAtTimelineTime(timelineTimeMs: Int): Pair<VideoClipItem, Int>? {
        if (videoClips.isEmpty()) return null
        var accumulatedMs = 0
        for (clip in videoClips) {
            val clipEnd = accumulatedMs + clip.effectiveDurationMs
            if (timelineTimeMs in accumulatedMs until clipEnd) {
                val clipRelativeMs = timelineTimeMs - accumulatedMs
                return Pair(clip, clipRelativeMs)
            }
            accumulatedMs = clipEnd
        }
        // If past end, return last clip
        val last = videoClips.last()
        return Pair(last, last.effectiveDurationMs)
    }

    /**
     * Calculates the exact start time of a specific clip on the project timeline.
     */
    fun getClipTimelineStartMs(clipId: String): Int {
        var accumulated = 0
        for (clip in videoClips) {
            if (clip.id == clipId) return accumulated
            accumulated += clip.effectiveDurationMs
        }
        return 0
    }

    fun findClipById(clipId: String): VideoClipItem? {
        return videoClips.find { it.id == clipId }
    }
}
