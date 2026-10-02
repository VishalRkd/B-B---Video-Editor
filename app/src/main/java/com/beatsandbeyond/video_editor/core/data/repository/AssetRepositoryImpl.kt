package com.beatsandbeyond.video_editor.core.data.repository

import android.content.Context
import android.net.Uri
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.local.db.dao.AssetDao
import com.beatsandbeyond.video_editor.core.data.local.db.mapper.AssetMapper
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import com.beatsandbeyond.video_editor.core.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [AssetRepository].
 * Delegates all persistence to Room's [AssetDao], while copying media assets
 * to a local persistent directory to guarantee lifetime access and ignore OS permission revokes.
 */
class AssetRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
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
                val localAssets = assets.map { asset ->
                    val localUri = copyUriToLocal(asset.mediaUri, asset.id, asset.displayName)
                    asset.copy(mediaUri = localUri)
                }
                val entities = localAssets.map { AssetMapper.toEntity(it, projectId) }
                assetDao.insertAssets(entities)
                AppResult.Success(localAssets)
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
                val entity = assetDao.getAssetById(id)
                if (entity != null) {
                    val uri = Uri.parse(entity.mediaUri)
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists() && file.absolutePath.contains(context.packageName)) {
                            file.delete()
                            Logger.d("AssetRepositoryImpl", "Deleted local copy for asset $id: ${file.absolutePath}")
                        }
                    }
                }
                assetDao.deleteAssetById(id)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    /**
     * Copies the content URI resource to the app's local persistent storage folder.
     * This avoids permission expiration issues common with modern Android SAF URIs.
     */
    private fun copyUriToLocal(originalUriStr: String, assetId: String, displayName: String): String {
        return try {
            val uri = Uri.parse(originalUriStr)
            
            // If it is already a local file inside our app package, do not copy
            if (uri.scheme == "file" && originalUriStr.contains(context.packageName)) {
                return originalUriStr
            }
            
            val importedAssetsDir = File(context.filesDir, "imported_assets").apply {
                if (!exists()) mkdirs()
            }
            
            val extension = displayName.substringAfterLast('.', "")
            val safeExtension = if (extension.isNotEmpty()) ".$extension" else ""
            val localFileName = "${assetId}$safeExtension"
            val destFile = File(importedAssetsDir, localFileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            Logger.e("AssetRepositoryImpl", "Failed to copy media file locally: $originalUriStr", e)
            originalUriStr // fallback
        }
    }
}
