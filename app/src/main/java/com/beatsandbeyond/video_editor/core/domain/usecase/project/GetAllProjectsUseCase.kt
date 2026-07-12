package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that returns all saved projects as a reactive stream.
 * Emits whenever the underlying Room table changes — no manual refresh needed.
 */
class GetAllProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    operator fun invoke(): Flow<AppResult<List<Project>>> =
        projectRepository.getAllProjects()
}
