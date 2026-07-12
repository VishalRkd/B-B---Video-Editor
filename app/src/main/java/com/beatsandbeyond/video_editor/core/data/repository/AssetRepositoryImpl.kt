package com.beatsandbeyond.video_editor.core.data.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.local.db.dao.AssetDao
import com.beatsandbeyond.video_editor.core.data.local.db.mapper.AssetMapper
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [AssetRepository].
 * Delegates all persistence to Room's [AssetDao].
 */
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
    private val dispatchers: CoroutineDispatchers,
) : AssetRepository {

    override fun getAllAssets(): Flow<AppResult<List<Asset>>> =
        assetDao.getAllAssets()
            .map<_, AppResult<List<Asset>>> { entities ->
                AppResult.Success(AssetMapper.toDomainList(entities))
            }
            .catch { e -> emit(AppResult.Error(e)) }
            .flowOn(dispatchers.io)

    override fun getAssetsByProject(projectId: String): Flow<AppResult<List<Asset>>> =
        assetDao.getAssetsByProject(projectId)
            .map<_, AppResult<List<Asset>>> { entities ->
                AppResult.Success(AssetMapper.toDomainList(entities))
            }
            .catch { e -> emit(AppResult.Error(e)) }
            .flowOn(dispatchers.io)

    override suspend fun getAssetById(id: String): AppResult<Asset> =
        withContext(dispatchers.io) {
            try {
                val entity = assetDao.getAssetById(id)
                if (entity != null) AppResult.Success(AssetMapper.toDomain(entity))
                else AppResult.Error(NoSuchElementException("Asset not found: $id"))
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun importAssets(assets: List<Asset>, projectId: String): AppResult<List<Asset>> =
        withContext(dispatchers.io) {
            try {
                val entities = assets.map { AssetMapper.toEntity(it, projectId) }
                assetDao.insertAssets(entities)
                AppResult.Success(assets)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun updateAsset(asset: Asset): AppResult<Asset> =
        withContext(dispatchers.io) {
            try {
                assetDao.updateAsset(AssetMapper.toEntity(asset))
                AppResult.Success(asset)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun deleteAsset(id: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            try {
                assetDao.deleteAssetById(id)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }
}
