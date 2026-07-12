package com.beatsandbeyond.video_editor.core.domain.usecase.media

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that retrieves a filtered list of media items from device storage.
 *
 * This is a single-responsibility class that encapsulates the rule:
 * "Query device media and apply the user's filter."
 *
 * Delegates to [MediaRepository], which queries MediaStore directly.
 *
 * Usage:
 * ```
 * val flow = getMediaItemsUseCase(MediaFilter.VIDEOS_ONLY)
 * flow.collect { result -> ... }
 * ```
 */
class GetMediaItemsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
) {
    /**
     * @param filter The filter to apply. Defaults to all media, newest first.
     */
    operator fun invoke(
        filter: MediaFilter = MediaFilter.ALL,
    ): Flow<AppResult<List<MediaItem>>> = mediaRepository.getMediaItems(filter)
}
