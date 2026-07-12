package com.beatsandbeyond.video_editor.core.domain.engine

import android.graphics.Bitmap
import android.net.Uri
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.model.MediaMetadata

/**
 * Engine responsible for loading and inspecting media from device storage.
 *
 * Implementations will use [android.media.MediaMetadataRetriever] for metadata
 * and [android.media.ThumbnailUtils] or hardware-accelerated decoding for thumbnails.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement with MediaMetadataRetriever and hardware acceleration.
 */
interface MediaEngine {

    /**
     * Loads and validates a media item from the given URI.
     * Returns [AppResult.Error] if the URI is inaccessible or the format is unsupported.
     */
    suspend fun loadMedia(uri: Uri): AppResult<MediaItem>

    /**
     * Extracts detailed technical metadata from the media at [uri].
     * Provides codec, bitrate, HDR, and rotation info beyond what MediaStore exposes.
     */
    suspend fun extractMetadata(uri: Uri): AppResult<MediaMetadata>

    /**
     * Generates a thumbnail [Bitmap] from the media at the given timeline [positionMs].
     * For images, [positionMs] is ignored.
     *
     * @param uri        Content URI of the media file.
     * @param positionMs Position within the video to capture (milliseconds). Ignored for images.
     */
    suspend fun generateThumbnail(uri: Uri, positionMs: Long = 0L): AppResult<Bitmap>
}
