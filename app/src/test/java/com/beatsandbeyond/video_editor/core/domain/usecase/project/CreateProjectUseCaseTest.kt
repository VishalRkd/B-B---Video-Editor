package com.beatsandbeyond.video_editor.core.domain.usecase.project

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.model.Timeline
import com.beatsandbeyond.video_editor.core.domain.repository.ProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [CreateProjectUseCase].
 *
 * Uses a Fake repository (not a Mock) per project testing rules to avoid
 * brittle test coupling to implementation details.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateProjectUseCaseTest {

    private lateinit var fakeProjectRepository: FakeProjectRepository
    private lateinit var useCase: CreateProjectUseCase

    @Before
    fun setUp() {
        fakeProjectRepository = FakeProjectRepository()
        useCase = CreateProjectUseCase(fakeProjectRepository)
    }

    @Test
    fun `invoke creates project with trimmed name`() = runTest(UnconfinedTestDispatcher()) {
        val result = useCase(name = "  My Project  ")

        assertTrue(result is AppResult.Success)
        val project = (result as AppResult.Success).data
        assertEquals("My Project", project.name)
    }

    @Test
    fun `invoke creates project with a non-empty unique ID`() = runTest(UnconfinedTestDispatcher()) {
        val result1 = useCase(name = "Project A")
        val result2 = useCase(name = "Project B")

        assertTrue(result1 is AppResult.Success)
        assertTrue(result2 is AppResult.Success)
        val id1 = (result1 as AppResult.Success).data.id
        val id2 = (result2 as AppResult.Success).data.id
        assertTrue(id1.isNotBlank())
        assertTrue(id2.isNotBlank())
        assertTrue("IDs must be unique", id1 != id2)
    }

    @Test
    fun `invoke creates project with empty timeline`() = runTest(UnconfinedTestDispatcher()) {
        val result = useCase(name = "New Project")

        val project = (result as AppResult.Success).data
        assertTrue(project.timeline.isEmpty)
    }

    @Test
    fun `invoke uses default FHD resolution`() = runTest(UnconfinedTestDispatcher()) {
        val result = useCase(name = "HD Project")

        val project = (result as AppResult.Success).data
        assertEquals(Resolution.FHD, project.resolution)
    }

    @Test
    fun `invoke accepts custom resolution`() = runTest(UnconfinedTestDispatcher()) {
        val result = useCase(name = "4K Project", resolution = Resolution.UHD_4K)

        val project = (result as AppResult.Success).data
        assertEquals(Resolution.UHD_4K, project.resolution)
    }

    @Test
    fun `invoke propagates repository error`() = runTest(UnconfinedTestDispatcher()) {
        fakeProjectRepository.shouldReturnError = true

        val result = useCase(name = "Fail Project")

        assertTrue(result is AppResult.Error)
    }
}

// ── Fake Repository ──────────────────────────────────────────────────────────

class FakeProjectRepository : ProjectRepository {
    var shouldReturnError = false
    private val projects = mutableListOf<Project>()

    override fun getAllProjects() = kotlinx.coroutines.flow.flow<AppResult<List<Project>>> {
        emit(AppResult.Success(projects.toList()))
    }

    override suspend fun getProjectById(id: String): AppResult<Project> {
        val project = projects.find { it.id == id }
        return if (project != null) AppResult.Success(project)
        else AppResult.Error(NoSuchElementException("Project not found"))
    }

    override suspend fun createProject(project: Project): AppResult<Project> {
        if (shouldReturnError) return AppResult.Error(Exception("Fake error"))
        projects.add(project)
        return AppResult.Success(project)
    }

    override suspend fun updateProject(project: Project): AppResult<Project> {
        val index = projects.indexOfFirst { it.id == project.id }
        return if (index >= 0) {
            projects[index] = project
            AppResult.Success(project)
        } else AppResult.Error(NoSuchElementException("Project not found"))
    }

    override suspend fun deleteProject(id: String): AppResult<Unit> {
        projects.removeIf { it.id == id }
        return AppResult.Success(Unit)
    }
}
