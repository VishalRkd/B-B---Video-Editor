package com.beatsandbeyond.video_editor.core.data.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.data.local.db.dao.AssetDao
import com.beatsandbeyond.video_editor.core.data.local.db.dao.ProjectDao
import com.beatsandbeyond.video_editor.core.data.local.db.mapper.ProjectMapper
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import com.beatsandbeyond.video_editor.core.utils.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [ProjectRepository].
 *
 * Persists and retrieves [Project] data using Room's [ProjectDao].
 * All database operations are dispatched to [dispatchers.io].
 */
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val assetDao: AssetDao,
    private val dispatchers: CoroutineDispatchers,
) : ProjectRepository {

    override fun getAllProjects(): Flow<AppResult<List<Project>>> =
        projectDao.getAllProjects()
            .map<_, AppResult<List<Project>>> { entities ->
                AppResult.Success(entities.map { ProjectMapper.toDomain(it) })
            }
            .catch { e -> emit(AppResult.Error(e as Exception)) }
            .flowOn(dispatchers.io)

    override suspend fun getProjectById(id: String): AppResult<Project> =
        withContext(dispatchers.io) {
            try {
                val entity = projectDao.getProjectById(id)
                if (entity != null) {
                    AppResult.Success(ProjectMapper.toDomain(entity))
                } else {
                    AppResult.Error(NoSuchElementException("Project not found: $id"))
                }
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun createProject(project: Project): AppResult<Project> =
        withContext(dispatchers.io) {
            try {
                projectDao.insertProject(ProjectMapper.toEntity(project))
                AppResult.Success(project)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun updateProject(project: Project): AppResult<Project> =
        withContext(dispatchers.io) {
            try {
                val updated = project.copy(updatedAt = System.currentTimeMillis())
                projectDao.updateProject(ProjectMapper.toEntity(updated))
                AppResult.Success(updated)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }

    override suspend fun deleteProject(id: String): AppResult<Unit> =
        withContext(dispatchers.io) {
            try {
                projectDao.deleteProjectById(id)
                assetDao.deleteAssetsByProject(id)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(e)
            }
        }
}
