package com.beatsandbeyond.video_editor.core.data.local.db.dao

import androidx.room.*
import com.beatsandbeyond.video_editor.core.data.local.db.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [AssetEntity] CRUD operations.
 */
@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY importedAt DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE projectId = :projectId ORDER BY importedAt DESC")
    fun getAssetsByProject(projectId: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE isFavorite = 1 ORDER BY importedAt DESC")
    fun getFavoriteAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)

    @Query("DELETE FROM assets WHERE projectId = :projectId")
    suspend fun deleteAssetsByProject(projectId: String)
}
