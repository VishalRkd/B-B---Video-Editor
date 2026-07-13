package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SilenceMediaSource
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
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
    private var audioPlayer: ExoPlayer? = null
    private var timelineClips: List<Clip> = emptyList()
    private var timelineAudioClips: List<Clip> = emptyList()

    private val _activeClipFilter = MutableStateFlow<com.beatsandbeyond.video_editor.core.domain.model.VideoFilter>(com.beatsandbeyond.video_editor.core.domain.model.VideoFilter.None)
    override val activeClipFilter: Flow<com.beatsandbeyond.video_editor.core.domain.model.VideoFilter> = _activeClipFilter.asStateFlow()

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

    private fun ensureAudioPlayer(): ExoPlayer {
        return audioPlayer ?: ExoPlayer.Builder(context).build().also { exo ->
            audioPlayer = exo
            Logger.d(TAG, "ExoPlayer audio instance created")
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
            timelineAudioClips = emptyList()
            _playbackState.value = PlaybackState.Idle
            player?.clearMediaItems()
            audioPlayer?.clearMediaItems()
            return
        }

        timelineClips = clips

        val dataSourceFactory = DefaultDataSource.Factory(context)
        val videoConcatenatingSource = ConcatenatingMediaSource()

        for (clip in clips) {
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

            val videoMediaItem = MediaItem.Builder().setUri(uri).build()
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(videoMediaItem)
            val clippedVideo = ClippingMediaSource(
                videoSource,
                clip.trimStartMs * 1000L,
                clip.trimEndMs * 1000L
            )
            videoConcatenatingSource.addMediaSource(clippedVideo)
        }

        val audioTrack = timeline.tracks.firstOrNull { it.type == TrackType.AUDIO }
        val audioClips = audioTrack?.clips ?: emptyList()
        timelineAudioClips = audioClips

        val audioConcatenatingSource = ConcatenatingMediaSource()
        var currentAudioPosMs = 0L
        val sortedAudioClips = audioClips.sortedBy { it.timelinePositionMs }
        for (ac in sortedAudioClips) {
            val gapMs = ac.timelinePositionMs - currentAudioPosMs
            if (gapMs > 0) {
                val silence = SilenceMediaSource(gapMs * 1000L)
                audioConcatenatingSource.addMediaSource(silence)
            }

            val audioAssetResult = assetRepository.getAssetById(ac.assetId)
            if (audioAssetResult is AppResult.Success) {
                val audioAsset = audioAssetResult.data
                val audioUri = Uri.parse(audioAsset.mediaUri)
                val audioMediaItem = MediaItem.Builder().setUri(audioUri).build()
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(audioMediaItem)
                val clippedAudio = ClippingMediaSource(
                    audioSource,
                    ac.trimStartMs * 1000L,
                    ac.trimEndMs * 1000L
                )
                audioConcatenatingSource.addMediaSource(clippedAudio)
            }
            currentAudioPosMs = ac.timelineEndMs
        }

        withContext(dispatchers.main) {
            val exo = ensurePlayer()
            val exoAudio = ensureAudioPlayer()
            val previousPositionMs = _currentPositionMs.value
            val wasPlaying = exo.playWhenReady

            exo.setMediaSource(videoConcatenatingSource)
            exo.prepare()

            exoAudio.setMediaSource(audioConcatenatingSource)
            exoAudio.prepare()
            
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
                exoAudio.playWhenReady = true
                exoAudio.play()
                startPositionUpdates()
            } else {
                exo.playWhenReady = false
                exo.pause()
                exoAudio.playWhenReady = false
                exoAudio.pause()
                stopPositionUpdates()
            }

            _playbackState.value = if (wasPlaying) PlaybackState.Playing else PlaybackState.Ready
            Logger.d(TAG, "Loaded multi-track timeline with ${clips.size} video clips. Restored position: $previousPositionMs ms (playing: $wasPlaying)")
        }
    }

    override fun play() {
        player?.let { exo ->
            exo.playWhenReady = true
            exo.play()
            startPositionUpdates()
        }
        audioPlayer?.let { exo ->
            exo.playWhenReady = true
            exo.play()
        }
    }

    override fun pause() {
        player?.pause()
        audioPlayer?.pause()
        stopPositionUpdates()
    }

    override fun stop() {
        player?.let { exo ->
            exo.stop()
            exo.seekTo(0, 0)
        }
        audioPlayer?.let { exo ->
            exo.stop()
            exo.seekTo(0)
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
        val exoAudio = audioPlayer
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
            exoAudio?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
            Logger.d(TAG, "Seeked to clip $targetIndex at local offset $targetOffsetMs ms (absolute $positionMs ms)")
        } else if (positionMs >= (timelineClips.lastOrNull()?.timelineEndMs ?: 0L)) {
            // Seek to the very end of the playlist
            val lastIndex = timelineClips.size - 1
            val lastClip = timelineClips.lastOrNull()
            if (lastClip != null && lastIndex >= 0) {
                val lastClipLocalDuration = lastClip.trimEndMs - lastClip.trimStartMs
                exo.seekTo(lastIndex, lastClipLocalDuration)
                exoAudio?.seekTo(lastClip.timelineEndMs)
                _currentPositionMs.value = lastClip.timelineEndMs
            }
        } else {
            exo.seekTo(0, 0)
            exoAudio?.seekTo(0)
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
        audioPlayer?.let { exo ->
            exo.release()
            Logger.d(TAG, "ExoPlayer audio released")
        }
        audioPlayer = null
        _playbackState.value = PlaybackState.Idle
        _currentPositionMs.value = 0L
    }

    // ── Position Polling ────────────────────────────────────────────────────

    private fun updateVideoVolume(clip: Clip) {
        val player = player ?: return
        if (player.volume != clip.volume) {
            player.volume = clip.volume
        }
    }

    private fun updateAudioVolume(positionMs: Long) {
        val audioPlayer = audioPlayer ?: return
        val activeAudioClip = timelineAudioClips.firstOrNull {
            positionMs >= it.timelinePositionMs && positionMs < it.timelineEndMs
        }
        val targetVolume = activeAudioClip?.volume ?: 1.0f
        if (audioPlayer.volume != targetVolume) {
            audioPlayer.volume = targetVolume
        }
    }

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
                        
                        updateVideoVolume(currentClip)
                        updateAudioVolume(absolutePositionMs)
                        
                        if (_activeClipFilter.value != currentClip.filter) {
                            _activeClipFilter.value = currentClip.filter
                        }
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
            val exoAudio = audioPlayer
            if (isPlaying) {
                _playbackState.value = PlaybackState.Playing
                startPositionUpdates()
                if (exoAudio?.isPlaying == false) {
                    exoAudio.play()
                }
            } else {
                stopPositionUpdates()
                if (player?.playbackState == Player.STATE_READY) {
                    _playbackState.value = PlaybackState.Ready
                }
                if (exoAudio?.isPlaying == true) {
                    exoAudio.pause()
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
