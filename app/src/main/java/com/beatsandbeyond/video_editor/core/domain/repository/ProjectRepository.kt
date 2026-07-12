package com.beatsandbeyond.video_editor.core.domain.repository

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for managing [Project] lifecycle.
 *
 * Projects are persisted in Room. This repository abstracts all database
 * access, keeping the use cases and ViewModels independent of Room internals.
 */
interface ProjectRepository {

    /**
     * Returns all projects as a [Flow] ordered by last updated time (newest first).
     * Emits whenever the underlying data changes (Room's reactivity).
     */
    fun getAllProjects(): Flow<AppResult<List<Project>>>

    /**
     * Returns a single [Project] by its [id].
     * Returns [AppResult.Error] if not found.
     */
    suspend fun getProjectById(id: String): AppResult<Project>

    /**
     * Persists a new [Project] to the database.
     * Returns [AppResult.Success] with the saved project on success.
     */
    suspend fun createProject(project: Project): AppResult<Project>

    /**
     * Updates an existing [Project]. Updates the [Project.updatedAt] timestamp automatically.
     * Returns [AppResult.Error] if the project does not exist.
     */
    suspend fun updateProject(project: Project): AppResult<Project>

    /**
     * Permanently deletes the project with the given [id].
     * Associated [Asset] entries are also removed.
     */
    suspend fun deleteProject(id: String): AppResult<Unit>
}
