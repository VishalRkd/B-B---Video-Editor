package com.beatsandbeyond.video_editor.core.data.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.datasource.MediaStoreDataSource
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.repository.MediaRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Implementation of [MediaRepository].
 *
 * Delegates all media queries to [MediaStoreDataSource], which is the sole
 * source of truth for device media. No caching occurs here.
 *
 * Future enhancement: register a [ContentObserver] on MediaStore to
 * automatically re-emit when the device gallery changes.
 */
class MediaRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val dispatchers: CoroutineDispatchers,
) : MediaRepository {

    override fun getMediaItems(filter: MediaFilter): Flow<AppResult<List<MediaItem>>> = flow {
        emit(AppResult.Loading)
        try {
            val items = mediaStoreDataSource.queryMedia(filter)
            emit(AppResult.Success(items))
        } catch (e: Exception) {
            emit(AppResult.Error(e))
        }
    }.flowOn(dispatchers.io)

    override suspend fun getMediaItemById(id: Long): AppResult<MediaItem> {
        return try {
            val item = mediaStoreDataSource.queryMediaById(id)
            if (item != null) {
                AppResult.Success(item)
            } else {
                AppResult.Error(NoSuchElementException("No media item found with id=$id"))
            }
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}
