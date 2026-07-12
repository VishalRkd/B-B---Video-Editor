package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import javax.inject.Inject

/** Use case that loads a single [Project] by its unique [id]. */
class GetProjectByIdUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Project> =
        projectRepository.getProjectById(id)
}
