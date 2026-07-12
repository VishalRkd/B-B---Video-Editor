package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.beatsandbeyond.video_editor.core.domain.engine.PreviewEngine
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Concrete implementation of [PreviewEngine] backed by AndroidX Media3 ExoPlayer.
 *
 * Phase 2 scope: single-clip preview from the primary video track.
 * The engine resolves clip [assetId]s → media URIs via [AssetRepository].
 * Multi-clip composition is Phase 3.
 *
 * Architecture note: ExoPlayer lives in the data layer, behind the [PreviewEngine]
 * interface. No ViewModel or Fragment imports ExoPlayer or Media3 classes directly.
 *
 * Lifecycle: [release()] must be called when the owning ViewModel is cleared.
 */
class ExoPlayerPreviewEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val assetRepository: AssetRepository,
) : PreviewEngine {

    companion object {
        private const val TAG = "ExoPlayerPreviewEngine"
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: Flow<Long> = _currentPositionMs.asStateFlow()

    private var player: ExoPlayer? = null

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
        val firstClip = primaryTrack?.clips?.firstOrNull()

        if (firstClip == null) {
            Logger.w(TAG, "Timeline has no clips — nothing to preview")
            _playbackState.value = PlaybackState.Idle
            return
        }

        // Resolve assetId → URI via the AssetRepository
        val assetResult = assetRepository.getAssetById(firstClip.assetId)
        if (assetResult is AppResult.Error) {
            Logger.e(TAG, "Asset not found for clip: ${firstClip.assetId}")
            _playbackState.value = PlaybackState.Error(
                Exception("Asset not found"), "Cannot load preview"
            )
            return
        }
        val asset = (assetResult as AppResult.Success).data
        val uri = Uri.parse(asset.mediaUri)

        val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
        val exo = ensurePlayer()
        exo.setMediaItem(mediaItem)
        exo.prepare()

        _playbackState.value = PlaybackState.Ready
        Logger.d(TAG, "Timeline loaded: uri=$uri")
    }

    override fun play() {
        player?.let { exo ->
            exo.playWhenReady = true
            exo.play()
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.let { exo ->
            exo.stop()
            exo.seekTo(0)
        }
        _currentPositionMs.value = 0L
        _playbackState.value = PlaybackState.Ready
    }

    override suspend fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    override fun release() {
        player?.let { exo ->
            exo.release()
            Logger.d(TAG, "ExoPlayer released")
        }
        player = null
        _playbackState.value = PlaybackState.Idle
        _currentPositionMs.value = 0L
    }

    // ── Player listener ─────────────────────────────────────────────────────

    private fun createPlayerListener() = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = when (playbackState) {
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> {
                    if (player?.playWhenReady == true) PlaybackState.Playing
                    else PlaybackState.Ready
                }
                Player.STATE_ENDED -> PlaybackState.Ended
                Player.STATE_IDLE -> PlaybackState.Idle
                else -> PlaybackState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playbackState.value = PlaybackState.Playing
            } else if (player?.playbackState == Player.STATE_READY) {
                _playbackState.value = PlaybackState.Ready
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Logger.e(TAG, "ExoPlayer error: ${error.message}", error)
            _playbackState.value = PlaybackState.Error(error, error.message)
        }
    }
}
