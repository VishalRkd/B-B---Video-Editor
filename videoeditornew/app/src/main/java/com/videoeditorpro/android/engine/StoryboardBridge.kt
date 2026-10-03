package com.videoeditorpro.android.engine

import android.util.Log
import com.videoeditorpro.android.domain.model.AudioClipItem
import com.videoeditorpro.android.domain.model.EditorProject
import com.videoeditorpro.android.domain.model.VideoClipItem
import xiaoying.engine.base.QRange
import xiaoying.engine.clip.QClip
import xiaoying.engine.clip.QMediaSource
import xiaoying.engine.storyboard.QStoryboard

/**
 * Bridges the pure Kotlin domain EditorProject model to the native QStoryboard and QClip instances.
 * Enables seamless multi-clip timelines, non-destructive trimming, speed scaling, and audio tracks.
 */
class StoryboardBridge {
    private val TAG = "StoryboardBridge"

    private var storyboard: QStoryboard? = null
    private val nativeClips = mutableListOf<QClip>()

    fun getStoryboard(): QStoryboard? = storyboard

    val clipCount: Int
        get() = storyboard?.clipCount ?: 0

    val durationMs: Int
        get() = storyboard?.duration ?: 0

    /**
     * Initializes or rebuilds the QStoryboard from the provided [project].
     */
    fun buildStoryboard(project: EditorProject): Boolean {
        val eng = EngineCore.getEngine() ?: run {
            Log.e(TAG, "Engine not initialized")
            return false
        }

        try {
            release()

            val sb = QStoryboard()
            val initRet = sb.init(eng, null)
            if (initRet != 0) {
                Log.e(TAG, "QStoryboard.init failed with error: $initRet")
                return false
            }

            storyboard = sb
            nativeClips.clear()

            // 1. Insert video clips sequentially into the main track
            for ((index, clipItem) in project.videoClips.withIndex()) {
                val clip = createAndConfigureClip(clipItem) ?: continue
                val insertRet = sb.insertClip(clip, index)
                if (insertRet == 0) {
                    nativeClips.add(clip)
                } else {
                    Log.e(TAG, "Failed to insert clip $index: $insertRet")
                }
            }

            // 2. Insert background audio tracks if present
            for (audioItem in project.audioClips) {
                insertAudioClipInternal(audioItem)
            }

            sb.applyTrim()
            Log.d(TAG, "Storyboard built with ${nativeClips.size} clips, duration=${sb.duration} ms")
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "buildStoryboard failed", e)
            return false
        }
    }

    private fun createAndConfigureClip(clipItem: VideoClipItem): QClip? {
        val eng = EngineCore.getEngine() ?: return null
        return try {
            val mediaSource = QMediaSource(QMediaSource.TYPE_FILE, false, clipItem.sourcePath)
            val clip = QClip()
            val ret = clip.init(eng, mediaSource)
            if (ret != 0) {
                Log.e(TAG, "QClip.init failed for ${clipItem.sourcePath}: $ret")
                return null
            }

            // Configure properties
            val trimLength = clipItem.rawTrimmedDurationMs
            clip.setProperty(QClip.PROP_TRIM_RANGE, QRange(clipItem.trimStartMs, trimLength))
            clip.setProperty(QClip.PROP_TIME_SCALE, clipItem.speed)
            clip.setProperty(QClip.PROP_CLIP_ROTATION, clipItem.rotation)
            clip.setProperty(QClip.PROP_AUDIO_DISABLED, clipItem.isMuted)

            clip
        } catch (e: Throwable) {
            Log.e(TAG, "Error creating clip for ${clipItem.sourcePath}", e)
            null
        }
    }

    /**
     * Updates trim range for a clip at [index] without full storyboard rebuild.
     */
    fun updateClipTrim(index: Int, startMs: Int, endMs: Int): Boolean {
        val sb = storyboard ?: return false
        val clip = nativeClips.getOrNull(index) ?: return false
        return try {
            val length = (endMs - startMs).coerceAtLeast(100)
            clip.setProperty(QClip.PROP_TRIM_RANGE, QRange(startMs, length))
            sb.applyTrim()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "updateClipTrim error", e)
            false
        }
    }

    /**
     * Updates playback speed for clip at [index].
     */
    fun updateClipSpeed(index: Int, speed: Float): Boolean {
        val sb = storyboard ?: return false
        val clip = nativeClips.getOrNull(index) ?: return false
        return try {
            clip.setProperty(QClip.PROP_TIME_SCALE, speed)
            sb.applyTrim()
            true
        } catch (e: Throwable) {
            Log.e(TAG, "updateClipSpeed error", e)
            false
        }
    }

    /**
     * Updates audio volume and mute state for clip at [index].
     */
    fun updateClipVolume(index: Int, volume: Int, isMuted: Boolean): Boolean {
        val clip = nativeClips.getOrNull(index) ?: return false
        return try {
            clip.setProperty(QClip.PROP_AUDIO_DISABLED, isMuted)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "updateClipVolume error", e)
            false
        }
    }

    /**
     * Updates rotation for clip at [index] (0, 90, 180, 270).
     */
    fun updateClipRotation(index: Int, rotationDegrees: Int): Boolean {
        val clip = nativeClips.getOrNull(index) ?: return false
        return try {
            clip.setProperty(QClip.PROP_CLIP_ROTATION, rotationDegrees)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "updateClipRotation error", e)
            false
        }
    }

    /**
     * Splits clip at [index] into two parts at [splitRelativeTimelineMs].
     */
    fun splitClip(index: Int, splitRelativeTimelineMs: Int, currentClipItem: VideoClipItem): Boolean {
        val sb = storyboard ?: return false
        val originalClip = nativeClips.getOrNull(index) ?: return false

        return try {
            val splitSourceTimeMs = currentClipItem.timelineTimeToSourceTimeMs(splitRelativeTimelineMs)
            val firstHalfLength = (splitSourceTimeMs - currentClipItem.trimStartMs).coerceAtLeast(100)
            val secondHalfLength = (currentClipItem.trimEndMs - splitSourceTimeMs).coerceAtLeast(100)

            // Adjust first half
            originalClip.setProperty(QClip.PROP_TRIM_RANGE, QRange(currentClipItem.trimStartMs, firstHalfLength))

            // Create second half clip
            val secondHalfItem = currentClipItem.copy(
                trimStartMs = splitSourceTimeMs,
                trimEndMs = currentClipItem.trimEndMs
            )
            val newClip = createAndConfigureClip(secondHalfItem) ?: return false

            val insertRet = sb.insertClip(newClip, index + 1)
            if (insertRet == 0) {
                nativeClips.add(index + 1, newClip)
                sb.applyTrim()
                true
            } else {
                Log.e(TAG, "sb.insertClip failed for split: $insertRet")
                false
            }
        } catch (e: Throwable) {
            Log.e(TAG, "splitClip error", e)
            false
        }
    }

    /**
     * Inserts an additional video clip into the storyboard at [index].
     */
    fun insertClip(index: Int, clipItem: VideoClipItem): Boolean {
        val sb = storyboard ?: return false
        val clip = createAndConfigureClip(clipItem) ?: return false
        val insertRet = sb.insertClip(clip, index)
        return if (insertRet == 0) {
            nativeClips.add(index, clip)
            sb.applyTrim()
            true
        } else false
    }

    /**
     * Removes clip at [index] from the storyboard.
     */
    fun removeClip(index: Int): Boolean {
        val sb = storyboard ?: return false
        val clip = nativeClips.getOrNull(index) ?: return false
        val removeRet = sb.removeClip(clip)
        return if (removeRet == 0) {
            nativeClips.removeAt(index)
            clip.unInit()
            sb.applyTrim()
            true
        } else false
    }

    /**
     * Inserts a background audio track.
     */
    fun addAudioTrack(audioItem: AudioClipItem): Boolean {
        return insertAudioClipInternal(audioItem)
    }

    private fun insertAudioClipInternal(audioItem: AudioClipItem): Boolean {
        val sb = storyboard ?: return false
        val eng = EngineCore.getEngine() ?: return false
        return try {
            val audioSource = QMediaSource(QMediaSource.TYPE_FILE, false, audioItem.sourcePath)
            val audioClip = QClip()
            val ret = audioClip.init(eng, audioSource)
            if (ret == 0) {
                val len = audioItem.durationMs
                audioClip.setProperty(QClip.PROP_TRIM_RANGE, QRange(audioItem.trimStartMs, len))
                audioClip.setProperty(QClip.PROP_AUDIO_DISABLED, audioItem.isMuted)
                sb.insertClip(audioClip, sb.clipCount)
                sb.applyTrim()
                true
            } else false
        } catch (e: Throwable) {
            Log.e(TAG, "Error inserting audio clip", e)
            false
        }
    }

    fun release() {
        try {
            for (clip in nativeClips) {
                clip.unInit()
            }
            nativeClips.clear()
            storyboard?.removeAllClip()
            storyboard?.unInit()
            storyboard = null
        } catch (e: Throwable) {
            Log.e(TAG, "Error releasing StoryboardBridge", e)
        }
    }
}
