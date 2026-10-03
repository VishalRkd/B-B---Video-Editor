package com.videoeditorpro.android.domain.command

import com.videoeditorpro.android.domain.model.AudioClipItem
import com.videoeditorpro.android.domain.model.EditorProject
import com.videoeditorpro.android.domain.model.FilterItem
import com.videoeditorpro.android.domain.model.VideoClipItem
import java.util.UUID

/**
 * Splits a clip at [splitRelativeTimelineMs] into two sequential clips.
 */
class SplitClipCommand(
    private val targetClipId: String,
    private val splitRelativeTimelineMs: Int
) : EditCommand {
    override val description: String = "Split Clip"

    private var generatedSecondClipId: String = UUID.randomUUID().toString()

    override fun execute(project: EditorProject): EditorProject {
        val index = project.videoClips.indexOfFirst { it.id == targetClipId }
        if (index == -1) return project

        val originalClip = project.videoClips[index]
        // Convert the timeline-relative split point to the source timestamp
        val splitSourceTimeMs = originalClip.timelineTimeToSourceTimeMs(splitRelativeTimelineMs)

        // Safety check: both halves must be at least 200ms
        if (splitSourceTimeMs <= originalClip.trimStartMs + 200 || splitSourceTimeMs >= originalClip.trimEndMs - 200) {
            return project
        }

        val firstHalf = originalClip.copy(
            trimEndMs = splitSourceTimeMs
        )
        val secondHalf = originalClip.copy(
            id = generatedSecondClipId,
            trimStartMs = splitSourceTimeMs
        )

        val updatedClips = project.videoClips.toMutableList()
        updatedClips[index] = firstHalf
        updatedClips.add(index + 1, secondHalf)

        return project.copy(
            videoClips = updatedClips,
            lastModified = System.currentTimeMillis()
        )
    }

    override fun undo(project: EditorProject): EditorProject {
        val index1 = project.videoClips.indexOfFirst { it.id == targetClipId }
        val index2 = project.videoClips.indexOfFirst { it.id == generatedSecondClipId }
        if (index1 == -1 || index2 == -1) return project

        val firstHalf = project.videoClips[index1]
        val secondHalf = project.videoClips[index2]

        val restoredClip = firstHalf.copy(
            trimEndMs = secondHalf.trimEndMs
        )

        val updatedClips = project.videoClips.toMutableList()
        updatedClips.removeAt(index2)
        updatedClips[index1] = restoredClip

        return project.copy(
            videoClips = updatedClips,
            lastModified = System.currentTimeMillis()
        )
    }
}

/**
 * Trims a clip's in/out points.
 */
class TrimClipCommand(
    private val clipId: String,
    private val oldTrimStartMs: Int,
    private val oldTrimEndMs: Int,
    private val newTrimStartMs: Int,
    private val newTrimEndMs: Int
) : EditCommand {
    override val description: String = "Trim Clip"

    override fun execute(project: EditorProject): EditorProject = updateTrim(project, newTrimStartMs, newTrimEndMs)

    override fun undo(project: EditorProject): EditorProject = updateTrim(project, oldTrimStartMs, oldTrimEndMs)

    private fun updateTrim(project: EditorProject, startMs: Int, endMs: Int): EditorProject {
        val updated = project.videoClips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(trimStartMs = startMs, trimEndMs = endMs)
            } else clip
        }
        return project.copy(videoClips = updated, lastModified = System.currentTimeMillis())
    }
}

/**
 * Changes playback speed of a clip.
 */
class ChangeSpeedCommand(
    private val clipId: String,
    private val oldSpeed: Float,
    private val newSpeed: Float
) : EditCommand {
    override val description: String = "Change Speed to ${newSpeed}x"

    override fun execute(project: EditorProject): EditorProject = setSpeed(project, newSpeed)

    override fun undo(project: EditorProject): EditorProject = setSpeed(project, oldSpeed)

    private fun setSpeed(project: EditorProject, speed: Float): EditorProject {
        val updated = project.videoClips.map { clip ->
            if (clip.id == clipId) clip.copy(speed = speed) else clip
        }
        return project.copy(videoClips = updated, lastModified = System.currentTimeMillis())
    }
}

/**
 * Changes volume or mute state of a video clip.
 */
class ChangeVolumeCommand(
    private val clipId: String,
    private val oldVolume: Int,
    private val oldMuted: Boolean,
    private val newVolume: Int,
    private val newMuted: Boolean
) : EditCommand {
    override val description: String = "Adjust Volume"

    override fun execute(project: EditorProject): EditorProject = setVolume(project, newVolume, newMuted)

    override fun undo(project: EditorProject): EditorProject = setVolume(project, oldVolume, oldMuted)

    private fun setVolume(project: EditorProject, volume: Int, muted: Boolean): EditorProject {
        val updated = project.videoClips.map { clip ->
            if (clip.id == clipId) clip.copy(volume = volume, isMuted = muted) else clip
        }
        return project.copy(videoClips = updated, lastModified = System.currentTimeMillis())
    }
}

/**
 * Applies color adjustment or shader filter to a clip.
 */
class ApplyFilterCommand(
    private val clipId: String,
    private val oldFilter: FilterItem,
    private val newFilter: FilterItem
) : EditCommand {
    override val description: String = "Apply Filter"

    override fun execute(project: EditorProject): EditorProject = setFilter(project, newFilter)

    override fun undo(project: EditorProject): EditorProject = setFilter(project, oldFilter)

    private fun setFilter(project: EditorProject, filter: FilterItem): EditorProject {
        val updated = project.videoClips.map { clip ->
            if (clip.id == clipId) clip.copy(filter = filter) else clip
        }
        return project.copy(videoClips = updated, lastModified = System.currentTimeMillis())
    }
}

/**
 * Adds an audio / music track to the project.
 */
class AddAudioCommand(
    private val audioClip: AudioClipItem
) : EditCommand {
    override val description: String = "Add Audio Track"

    override fun execute(project: EditorProject): EditorProject {
        return project.copy(
            audioClips = project.audioClips + audioClip,
            lastModified = System.currentTimeMillis()
        )
    }

    override fun undo(project: EditorProject): EditorProject {
        return project.copy(
            audioClips = project.audioClips.filter { it.id != audioClip.id },
            lastModified = System.currentTimeMillis()
        )
    }
}

/**
 * Removes an audio / music track from the project.
 */
class RemoveAudioCommand(
    private val audioClip: AudioClipItem
) : EditCommand {
    override val description: String = "Remove Audio Track"

    override fun execute(project: EditorProject): EditorProject {
        return project.copy(
            audioClips = project.audioClips.filter { it.id != audioClip.id },
            lastModified = System.currentTimeMillis()
        )
    }

    override fun undo(project: EditorProject): EditorProject {
        return project.copy(
            audioClips = project.audioClips + audioClip,
            lastModified = System.currentTimeMillis()
        )
    }
}
