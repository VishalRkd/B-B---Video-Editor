package com.beatsandbeyond.video_editor.core.data.local.db.dao

import androidx.room.*
import com.beatsandbeyond.video_editor.core.data.local.db.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [ProjectEntity] CRUD operations.
 *
 * All queries that return lists are exposed as [Flow] so that Room automatically
 * emits updates whenever the underlying table changes. This drives reactive UI
 * without manual refresh calls.
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>
}
