package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * Saves an updated [Project] back to Room. Called after every editing operation.
 */
class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    suspend operator fun invoke(project: Project): AppResult<Project> =
        projectRepository.updateProject(project)
}
