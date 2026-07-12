package com.beatsandbeyond.video_editor.core.domain.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for accessing device media.
 *
 * MediaStore is the sole source of truth for device media.
 * Implementations query MediaStore directly — no caching in Room.
 *
 * The repository pattern allows the data source to be swapped (e.g., cloud storage
 * in a future phase) without changing any use cases or ViewModels.
 */
interface MediaRepository {

    /**
     * Returns a [Flow] of [AppResult] wrapping the list of media items matching [filter].
     * Emits [AppResult.Loading] immediately, then [AppResult.Success] or [AppResult.Error].
     *
     * @param filter Scoping filter (types, sort order, search query).
     */
    fun getMediaItems(filter: MediaFilter): Flow<AppResult<List<MediaItem>>>

    /**
     * Returns a single [MediaItem] by its MediaStore [id].
     * Returns [AppResult.Error] if no item with that ID exists.
     */
    suspend fun getMediaItemById(id: Long): AppResult<MediaItem>
}
