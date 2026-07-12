package com.beatsandbeyond.video_editor.core.data.datasource

import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem

/**
 * Data source contract for reading media directly from the device via MediaStore.
 *
 * Implementations use [android.content.ContentResolver] to query MediaStore URIs.
 * This interface allows the data source to be replaced with a fake/mock in tests
 * without any Android emulator requirement.
 */
interface MediaStoreDataSource {

    /**
     * Queries device media matching the given [filter].
     * Returns an empty list if no media matches or storage permission is not granted.
     */
    suspend fun queryMedia(filter: MediaFilter): List<MediaItem>

    /**
     * Queries a single media item by its MediaStore [id].
     * Returns null if no item with that ID exists.
     */
    suspend fun queryMediaById(id: Long): MediaItem?
}
