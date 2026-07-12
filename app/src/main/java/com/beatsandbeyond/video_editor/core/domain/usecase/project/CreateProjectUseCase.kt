package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case responsible for creating a new [Project].
 *
 * This encapsulates the business rules for project creation:
 * - Generates a unique ID
 * - Creates an empty [Timeline] with a primary video track
 * - Sets default resolution and frame rate
 * - Records creation timestamp
 *
 * Phase 1: Use case is defined and wired — UI flow for project creation is Phase 2.
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    /**
     * Creates a new project with the given [name] and default settings.
     *
     * @param name       User-visible name for the project.
     * @param resolution Output resolution. Defaults to 1920×1080.
     * @param frameRate  Output frame rate. Defaults to 30.0 fps.
     */
    suspend operator fun invoke(
        name: String,
        resolution: Resolution = Resolution.FHD,
        frameRate: Float = 30.0f,
    ): AppResult<Project> {
        val projectId = UUID.randomUUID().toString()
        val timelineId = UUID.randomUUID().toString()
        val primaryTrackId = UUID.randomUUID().toString()

        val project = Project(
            id = projectId,
            name = name.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            timeline = Timeline.empty(timelineId, primaryTrackId),
            resolution = resolution,
            frameRate = frameRate,
        )

        return projectRepository.createProject(project)
    }
}
