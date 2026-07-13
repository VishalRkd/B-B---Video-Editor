package com.beatsandbeyond.video_editor.feature.projectdetail.view

/**
 * Metadata cache stored on a timeline clip view's tag to enable performance-optimal view recycling
 * and prevent redundant film strip thumbnail loads.
 */
data class ClipViewMetadata(
    val clipId: String,
    val timelinePositionMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val durationOnTimelineMs: Long,
    val isSelected: Boolean,
    val assetUri: String?,
)
