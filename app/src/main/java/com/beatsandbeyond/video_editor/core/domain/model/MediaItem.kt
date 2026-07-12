package com.beatsandbeyond.video_editor.core.domain.model

import android.net.Uri

/**
 * Represents a piece of media on the device as exposed by MediaStore.
 *
 * [MediaItem] is a LIVE view of device media — it is NEVER persisted in Room.
 * It is always fetched from [MediaStore] on demand via [MediaStoreDataSource].
 *
 * When a user imports a [MediaItem] into the app, an [Asset] is created and
 * stored in Room with a reference back to [uri].
 *
 * @param id       The MediaStore content ID (unique per content URI base).
 * @param uri      Content URI to access this item (e.g., content://media/external/video/media/42).
 * @param name     Display name (filename without path).
 * @param mediaType The classification of this media (VIDEO, IMAGE, AUDIO).
 * @param mimeType Full MIME type string (e.g., "video/mp4", "image/jpeg").
 * @param durationMs Duration in milliseconds. 0 for images.
 * @param width    Frame width in pixels. 0 if unknown.
 * @param height   Frame height in pixels. 0 if unknown.
 * @param sizeBytes File size in bytes.
 * @param dateAddedMs Unix timestamp (ms) of when this item was added to device storage.
 * @param absolutePath Absolute file-system path (may be empty on API 29+ scoped storage).
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mediaType: MediaType,
    val mimeType: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val absolutePath: String,
) {
    /** True if this item has a non-zero duration (i.e., it is a video or audio file). */
    val hasAudio: Boolean get() = mediaType == MediaType.VIDEO || mediaType == MediaType.AUDIO

    /** Aspect ratio of the media frame. Returns 0 if dimensions are unknown. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 0f
}
