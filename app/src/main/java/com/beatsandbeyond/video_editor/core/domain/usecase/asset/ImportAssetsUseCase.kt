package com.beatsandbeyond.video_editor.core.domain.usecase.asset

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case responsible for converting [MediaItem]s (from MediaStore) into [Asset]s
 * (app-owned, persisted in Room) and associating them with a project.
 *
 * Business rules:
 * - Each [MediaItem] becomes one [Asset] with a freshly generated UUID.
 * - Assets store a URI reference to the original device file — never a copy.
 * - The import timestamp is the current wall-clock time.
 * - Assets are associated with [projectId] so they can be queried per-project.
 *
 * This is the bridge between the read-only MediaStore world and the
 * mutable app editing world.
 */
class ImportAssetsUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
) {
    /**
     * @param mediaItems The selected [MediaItem]s from the media picker.
     * @param projectId  The ID of the project these assets belong to.
     * @return [AppResult.Success] with the persisted [Asset] list, or [AppResult.Error].
     */
    suspend operator fun invoke(
        mediaItems: List<MediaItem>,
        projectId: String,
    ): AppResult<List<Asset>> {
        val now = System.currentTimeMillis()

        val assets = mediaItems.map { item ->
            Asset(
                id = UUID.randomUUID().toString(),
                mediaUri = item.uri.toString(),
                mediaType = item.mediaType,
                displayName = item.name,
                durationMs = item.durationMs,
                width = item.width,
                height = item.height,
                sizeBytes = item.sizeBytes,
                importedAt = now,
            )
        }

        return assetRepository.importAssets(assets, projectId)
    }
}
