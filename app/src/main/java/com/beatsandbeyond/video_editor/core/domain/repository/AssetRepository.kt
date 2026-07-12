package com.beatsandbeyond.video_editor.core.domain.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing [Asset] lifecycle.
 *
 * Assets are the app's internal representation of imported media. They are persisted
 * in Room and referenced by [Clip]s on a [Timeline].
 *
 * This interface lives in the domain layer — the data layer provides the implementation.
 */
interface AssetRepository {

    /**
     * Returns all assets ordered by import time (newest first) as a reactive [Flow].
     * Emits whenever the assets table changes.
     */
    fun getAllAssets(): Flow<AppResult<List<Asset>>>

    /**
     * Returns all assets belonging to a specific project.
     */
    fun getAssetsByProject(projectId: String): Flow<AppResult<List<Asset>>>

    /**
     * Returns a single asset by its [id], or [AppResult.Error] if not found.
     */
    suspend fun getAssetById(id: String): AppResult<Asset>

    /**
     * Persists a list of [Asset]s associated with [projectId].
     * Uses REPLACE conflict strategy so re-importing the same URI updates the existing record.
     */
    suspend fun importAssets(assets: List<Asset>, projectId: String): AppResult<List<Asset>>

    /**
     * Updates a single [Asset] — used for toggling favorites and setting custom names.
     */
    suspend fun updateAsset(asset: Asset): AppResult<Asset>

    /**
     * Deletes a single asset by [id]. Does NOT delete the underlying media file.
     */
    suspend fun deleteAsset(id: String): AppResult<Unit>
}
