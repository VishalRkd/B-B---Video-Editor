package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [PreviewEngine] backed by AndroidX Media3 ExoPlayer.
 *
 * Phase 3 scope:
 * - Load all clips from the primary video track sequentially.
 * - Map speed multipliers to ExoPlayer.
 * - Precise absolute timeline seeking.
 * - Periodic timeline position updates.
 */
@Singleton
class ExoPlayerPreviewEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
    private val dispatchers: CoroutineDispatchers,
) : PreviewEngine {

    companion object {
        private const val TAG = "ExoPlayerPreviewEngine"
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: Flow<Long> = _currentPositionMs.asStateFlow()

    private var player: ExoPlayer? = null
    private var timelineClips: List<Clip> = emptyList()

    // Seek requested before the player finished preparing. Applied once STATE_READY.
    private var pendingSeekMs: Long? = null

    private val scope = CoroutineScope(dispatchers.main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    // ── Player lifecycle ────────────────────────────────────────────────────

    private fun ensurePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { exo ->
            exo.addListener(createPlayerListener())
            player = exo
            Logger.d(TAG, "ExoPlayer instance created")
        }
    }

    override fun attach(surface: Surface) {
        ensurePlayer().setVideoSurface(surface)
        Logger.d(TAG, "Surface attached")
    }

    override fun detach() {
        player?.clearVideoSurface()
        Logger.d(TAG, "Surface detached")
    }

    override suspend fun loadTimeline(timeline: Timeline) {
        val primaryTrack = timeline.primaryVideoTrack
        val clips = primaryTrack?.clips ?: emptyList()

        if (clips.isEmpty()) {
            Logger.w(TAG, "Timeline has no clips — nothing to preview")
            timelineClips = emptyList()
            _playbackState.value = PlaybackState.Idle
            player?.clearMediaItems()
            return
        }

        timelineClips = clips

        val mediaItems = mutableListOf<MediaItem>()
        for (clip in clips) {
            // Resolve assetId -> URI via AssetRepository
            val assetResult = assetRepository.getAssetById(clip.assetId)
            if (assetResult is AppResult.Error) {
                Logger.e(TAG, "Asset not found for clip: ${clip.assetId}")
                _playbackState.value = PlaybackState.Error(
                    Exception("Asset not found"), "Cannot load preview"
                )
                return
            }
            val asset = (assetResult as AppResult.Success).data
            val uri = Uri.parse(asset.mediaUri)

            val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimEndMs)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(clippingConfiguration)
                .build()
            mediaItems.add(mediaItem)
        }

        withContext(dispatchers.main) {
            val exo = ensurePlayer()
            val previousPositionMs = _currentPositionMs.value
            val wasPlaying = exo.playWhenReady

            exo.setMediaItems(mediaItems)
            exo.prepare()
            
            // Find target clip and offset for current timeline position
            var targetIndex = 0
            var targetOffsetMs = 0L
            for (i in clips.indices) {
                val clip = clips[i]
                if (previousPositionMs >= clip.timelinePositionMs && previousPositionMs <= clip.timelineEndMs) {
                    targetIndex = i
                    val relativeTimelineOffsetMs = previousPositionMs - clip.timelinePositionMs
                    targetOffsetMs = (relativeTimelineOffsetMs * clip.speedFactor).toLong()
                    break
                }
            }
            
            val activeClip = clips.getOrNull(targetIndex) ?: clips.firstOrNull()
            if (activeClip != null) {
                exo.setPlaybackSpeed(activeClip.speedFactor)
            }

            // Defer the seek until the player is STATE_READY. Seeking immediately after
            // prepare() is a race — the player ignores it or applies it to the wrong window.
            // BUT if the player is ALREADY ready (e.g. a second loadTimeline without release),
            // onPlaybackStateChanged won't re-fire, so apply the seek inline here.
            if (exo.playbackState == Player.STATE_READY) {
                seekToInternal(previousPositionMs)
                pendingSeekMs = null
            } else {
                pendingSeekMs = previousPositionMs
                _currentPositionMs.value = previousPositionMs
            }

            if (wasPlaying) {
                exo.playWhenReady = true
                exo.play()
                startPositionUpdates()
            } else {
                exo.playWhenReady = false
                exo.pause()
                stopPositionUpdates()
            }

            _playbackState.value = if (wasPlaying) PlaybackState.Playing else PlaybackState.Ready
            Logger.d(TAG, "Loaded playlist with ${mediaItems.size} clips. Restored position: $previousPositionMs ms (playing: $wasPlaying)")
        }
    }

    override fun play() {
        player?.let { exo ->
            exo.playWhenReady = true
            exo.play()
            startPositionUpdates()
        }
    }

    override fun pause() {
        player?.pause()
        stopPositionUpdates()
    }

    override fun stop() {
        player?.let { exo ->
            exo.stop()
            exo.seekTo(0, 0)
        }
        stopPositionUpdates()
        _currentPositionMs.value = 0L
        _playbackState.value = PlaybackState.Ready
    }

    override suspend fun seekTo(positionMs: Long) {
        withContext(dispatchers.main) {
            val exo = ensurePlayer()
            // If the player isn't ready yet, defer until STATE_READY (see onPlaybackStateChanged).
            if (exo.playbackState != Player.STATE_READY) {
                pendingSeekMs = positionMs
                _currentPositionMs.value = positionMs
                return@withContext
            }
            seekToInternal(positionMs)
        }
    }

    /** Non-suspending core seek. Safe to call from the player listener. */
    private fun seekToInternal(positionMs: Long) {
        val exo = player ?: return
        // Find the clip containing this absolute timeline position
        var targetIndex = -1
        var targetOffsetMs = 0L
        for (i in timelineClips.indices) {
            val clip = timelineClips[i]
            if (positionMs >= clip.timelinePositionMs && positionMs <= clip.timelineEndMs) {
                targetIndex = i
                val relativeTimelineOffsetMs = positionMs - clip.timelinePositionMs
                targetOffsetMs = (relativeTimelineOffsetMs * clip.speedFactor).toLong()
                break
            }
        }

        if (targetIndex != -1) {
            exo.seekTo(targetIndex, targetOffsetMs)
            _currentPositionMs.value = positionMs
            Logger.d(TAG, "Seeked to clip $targetIndex at local offset $targetOffsetMs ms (absolute $positionMs ms)")
        } else if (positionMs >= (timelineClips.lastOrNull()?.timelineEndMs ?: 0L)) {
            // Seek to the very end of the playlist
            val lastIndex = timelineClips.size - 1
            val lastClip = timelineClips.lastOrNull()
            if (lastClip != null && lastIndex >= 0) {
                val lastClipLocalDuration = lastClip.trimEndMs - lastClip.trimStartMs
                exo.seekTo(lastIndex, lastClipLocalDuration)
                _currentPositionMs.value = lastClip.timelineEndMs
            }
        } else {
            exo.seekTo(0, 0)
            _currentPositionMs.value = 0L
        }
    }

    override fun release() {
        stopPositionUpdates()
        pendingSeekMs = null
        player?.let { exo ->
            exo.release()
            Logger.d(TAG, "ExoPlayer released")
        }
        player = null
        _playbackState.value = PlaybackState.Idle
        _currentPositionMs.value = 0L
    }

    // ── Position Polling ────────────────────────────────────────────────────

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch(dispatchers.main) {
            while (isActive) {
                player?.let { exo ->
                    val currentIdx = exo.currentMediaItemIndex
                    val currentClip = timelineClips.getOrNull(currentIdx)
                    if (currentClip != null) {
                        val localOffsetMs = exo.currentPosition
                        val absolutePositionMs = currentClip.timelinePositionMs + 
                            (localOffsetMs / currentClip.speedFactor).toLong()
                        _currentPositionMs.value = absolutePositionMs
                    }
                }
                delay(33) // ~30 fps updates
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // ── Player listener ─────────────────────────────────────────────────────

    private fun createPlayerListener() = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = when (playbackState) {
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> {
                    // Apply any seek that was requested before the player finished preparing.
                    pendingSeekMs?.let { seekToInternal(it) }
                    pendingSeekMs = null
                    if (player?.playWhenReady == true) PlaybackState.Playing
                    else PlaybackState.Ready
                }
                Player.STATE_ENDED -> {
                    stopPositionUpdates()
                    PlaybackState.Ended
                }
                Player.STATE_IDLE -> {
                    stopPositionUpdates()
                    PlaybackState.Idle
                }
                else -> PlaybackState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playbackState.value = PlaybackState.Playing
                startPositionUpdates()
            } else {
                stopPositionUpdates()
                if (player?.playbackState == Player.STATE_READY) {
                    _playbackState.value = PlaybackState.Ready
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val exo = player ?: return
            val currentIdx = exo.currentMediaItemIndex
            val currentClip = timelineClips.getOrNull(currentIdx)
            if (currentClip != null) {
                exo.setPlaybackSpeed(currentClip.speedFactor)
                Logger.d(TAG, "Transitioned to clip $currentIdx, speed set to ${currentClip.speedFactor}x")
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            stopPositionUpdates()
            Logger.e(TAG, "ExoPlayer error: ${error.message}", error)
            _playbackState.value = PlaybackState.Error(error, error.message)
        }
    }
}
