package com.beatsandbeyond.video_editor.core.domain.model

import java.util.UUID

/**
 * Inserts [newClip] into [Track], resolving any overlaps by trimming or splitting
 * existing clips and shifting subsequent clips to the right.
 */
fun Track.insertAndResolveOverlaps(newClip: Clip): Track {
    val newStart = newClip.timelinePositionMs
    val durationMs = newClip.durationOnTimelineMs
    val newEnd = newStart + durationMs

    val newClips = mutableListOf<Clip>()

    for (c in clips) {
        val clipStart = c.timelinePositionMs
        val clipEnd = c.timelineEndMs

        if (clipEnd <= newStart) {
            newClips.add(c)
        } else if (clipStart >= newEnd) {
            newClips.add(c.copy(timelinePositionMs = clipStart + durationMs))
        } else {
            if (clipStart < newStart && clipEnd > newEnd) {
                val leftTimelineDuration = newStart - clipStart
                val leftTrimDuration = (leftTimelineDuration * c.speedFactor).toLong()
                val leftTrimEnd = (c.trimStartMs + leftTrimDuration).coerceAtMost(c.trimEndMs)
                val leftClip = c.copy(trimEndMs = leftTrimEnd)

                val rightTimelineDuration = clipEnd - newEnd
                val rightTrimDuration = (rightTimelineDuration * c.speedFactor).toLong()
                val rightTrimStart = (c.trimEndMs - rightTrimDuration).coerceAtLeast(c.trimStartMs)
                val rightClip = c.copy(
                    id = UUID.randomUUID().toString(),
                    timelinePositionMs = newEnd,
                    trimStartMs = rightTrimStart,
                    trimEndMs = c.trimEndMs
                )

                if (leftClip.trimEndMs > leftClip.trimStartMs) {
                    newClips.add(leftClip)
                }
                if (rightClip.trimEndMs > rightClip.trimStartMs) {
                    newClips.add(rightClip)
                }
            } else if (clipStart < newStart && clipEnd <= newEnd) {
                val leftTimelineDuration = newStart - clipStart
                val leftTrimDuration = (leftTimelineDuration * c.speedFactor).toLong()
                val leftTrimEnd = (c.trimStartMs + leftTrimDuration).coerceAtMost(c.trimEndMs)
                val leftClip = c.copy(trimEndMs = leftTrimEnd)
                if (leftClip.trimEndMs > leftClip.trimStartMs) {
                    newClips.add(leftClip)
                }
            } else if (clipStart >= newStart && clipEnd > newEnd) {
                val rightTimelineDuration = clipEnd - newEnd
                val rightTrimDuration = (rightTimelineDuration * c.speedFactor).toLong()
                val rightTrimStart = (c.trimEndMs - rightTrimDuration).coerceAtLeast(c.trimStartMs)
                val rightClip = c.copy(
                    timelinePositionMs = newEnd,
                    trimStartMs = rightTrimStart,
                    trimEndMs = c.trimEndMs
                )
                if (rightClip.trimEndMs > rightClip.trimStartMs) {
                    newClips.add(rightClip)
                }
            }
        }
    }
    newClips.add(newClip)
    return this.copy(clips = newClips.sortedBy { it.timelinePositionMs })
}
