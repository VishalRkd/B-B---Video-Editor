package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Use case that permanently deletes a project and all its associated assets.
 *
 * Deletion is ordered: assets first, then the project. This order matters because
 * if the project is deleted first and the assets delete fails, the project reference
 * is lost but orphaned assets remain. Deleting assets first ensures no orphans even
 * if the project delete fails (the project can be re-tried).
 */
class DeleteProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val assetRepository: AssetRepository,
) {
    /**
     * @param projectId The ID of the project to delete.
     */
    suspend operator fun invoke(projectId: String): AppResult<Unit> {
        // Note: ProjectRepositoryImpl.deleteProject() already cascades to AssetDao.
        // This use case documents the intent — if the cascade strategy ever changes
        // to explicit, the logic lives here rather than scattered across the UI.
        return projectRepository.deleteProject(projectId)
    }
}
