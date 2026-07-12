package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents a piece of media that has been imported into the B&B Video Editor.
 *
 * An [Asset] is the app's internal representation of a device media file.
 * It is persisted in Room and contains:
 *   - A reference back to the original device media via [mediaUri] (NOT a copy).
 *   - App-specific metadata: when it was imported, user favorites, custom names.
 *
 * The [Asset] bridges the world of device storage (MediaStore → [MediaItem]) and
 * the editing world (Timeline → [Clip] → [Asset]).
 *
 * @param id          UUID generated at import time.
 * @param mediaUri    URI string of the original MediaStore content (e.g., content://media/...)
 * @param mediaType   Classification of the asset.
 * @param displayName The user-visible name (defaults to the filename).
 * @param durationMs  Duration in ms. 0 for images.
 * @param width       Frame width in pixels.
 * @param height      Frame height in pixels.
 * @param sizeBytes   File size in bytes.
 * @param importedAt  Unix timestamp (ms) when the user imported this asset.
 * @param isFavorite  Whether the user has marked this asset as a favorite.
 * @param customName  Optional user-provided name override.
 */
data class Asset(
    val id: String,
    val mediaUri: String,
    val mediaType: MediaType,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val importedAt: Long,
    val isFavorite: Boolean = false,
    val customName: String? = null,
) {
    /** Returns [customName] if set, otherwise [displayName]. */
    val name: String get() = customName ?: displayName

    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 0f
}
