package com.beatsandbeyond.video_editor.core.domain.usecase.media

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.repository.MediaRepository
import javax.inject.Inject

/**
 * Use case that retrieves a single [MediaItem] by its MediaStore ID.
 *
 * Used when navigating to the detail view of a specific media item,
 * or when the editing engine needs to resolve an asset URI from a stored ID.
 */
class GetMediaItemByIdUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
) {
    suspend operator fun invoke(id: Long): AppResult<MediaItem> =
        mediaRepository.getMediaItemById(id)
}
