package com.beatsandbeyond.video_editor.core.data.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.MediaEngine
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.model.MediaMetadata
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [MediaEngine] using [MediaMetadataRetriever].
 */
@Singleton
class AndroidMediaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : MediaEngine {

    companion object {
        private const val TAG = "AndroidMediaEngine"
    }

    override suspend fun loadMedia(uri: Uri): AppResult<MediaItem> = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null
            val mediaType = if (hasVideo) MediaType.VIDEO else MediaType.AUDIO
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"
            val displayName = uri.lastPathSegment ?: "unknown"

            AppResult.Success(
                MediaItem(
                    id = uri.toString().hashCode().toLong(),
                    uri = uri,
                    name = displayName,
                    mediaType = mediaType,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    width = 0,
                    height = 0,
                    sizeBytes = 0L,
                    dateAddedMs = System.currentTimeMillis(),
                    absolutePath = ""
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load media: $uri", e)
            AppResult.Error(e)
        } finally {
            retriever.release()
        }
    }

    override suspend fun extractMetadata(uri: Uri): AppResult<MediaMetadata> = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0

            AppResult.Success(
                MediaMetadata(
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    frameRate = 30.0f,
                    bitrateBps = bitrate.toLong(),
                    videoCodec = "video/avc",
                    audioCodec = "audio/mp4a-latm",
                    hasAudio = true,
                    isHdr = false,
                    rotation = rotation
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract metadata: $uri", e)
            AppResult.Error(e)
        } finally {
            retriever.release()
        }
    }

    override suspend fun generateThumbnail(uri: Uri, positionMs: Long): AppResult<Bitmap> = withContext(dispatchers.io) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(
                positionMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            if (frame != null) {
                AppResult.Success(frame)
            } else {
                AppResult.Error(Exception("Could not extract frame at $positionMs ms"))
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to generate thumbnail: $uri at $positionMs ms", e)
            AppResult.Error(e)
        } finally {
            retriever.release()
        }
    }
}
