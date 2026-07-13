package com.beatsandbeyond.video_editor.core.domain.usecase.asset

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GetAssetsByProjectIdUseCase].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetAssetsByProjectIdUseCaseTest {

    private lateinit var fakeAssetRepository: FakeAssetRepository
    private lateinit var useCase: GetAssetsByProjectIdUseCase

    @Before
    fun setUp() {
        fakeAssetRepository = FakeAssetRepository()
        useCase = GetAssetsByProjectIdUseCase(fakeAssetRepository)
    }

    @Test
    fun `invoke returns assets filtered by project ID`() = runTest(UnconfinedTestDispatcher()) {
        val project1Id = "project_1"
        val project2Id = "project_2"
        
        val asset1 = createFakeAsset("1")
        val asset2 = createFakeAsset("2")
        val asset3 = createFakeAsset("3")
        
        fakeAssetRepository.importAssets(listOf(asset1, asset2), project1Id)
        fakeAssetRepository.importAssets(listOf(asset3), project2Id)

        val resultFlow = useCase(project1Id)
        val result = resultFlow.first()

        assertTrue(result is AppResult.Success)
        val list = (result as AppResult.Success).data
        assertEquals(2, list.size)
        assertEquals("1", list[0].id)
        assertEquals("2", list[1].id)
    }

    @Test
    fun `invoke propagates error from repository`() = runTest(UnconfinedTestDispatcher()) {
        fakeAssetRepository.shouldReturnError = true
        val resultFlow = useCase("project_1")
        val result = resultFlow.first()

        assertTrue(result is AppResult.Error)
    }

    private fun createFakeAsset(id: String): Asset {
        return Asset(
            id = id,
            mediaUri = "content://media/$id",
            mediaType = MediaType.VIDEO,
            displayName = "video_$id.mp4",
            durationMs = 5000L,
            width = 1920,
            height = 1080,
            sizeBytes = 1024L,
            importedAt = System.currentTimeMillis(),
            isFavorite = false,
            customName = null
        )
    }
}

// ── Fake Repository ──────────────────────────────────────────────────────────

class FakeAssetRepository : AssetRepository {
    var shouldReturnError = false
    private val assets = mutableListOf<Asset>()
    private val projectAssetsMap = mutableMapOf<String, MutableList<Asset>>()

    override fun getAllAssets(): Flow<AppResult<List<Asset>>> = flow {
        if (shouldReturnError) emit(AppResult.Error(Exception("Fake error")))
        else emit(AppResult.Success(assets.toList()))
    }

    override fun getAssetsByProject(projectId: String): Flow<AppResult<List<Asset>>> = flow {
        if (shouldReturnError) emit(AppResult.Error(Exception("Fake error")))
        else emit(AppResult.Success(projectAssetsMap[projectId]?.toList() ?: emptyList()))
    }

    override suspend fun getAssetById(id: String): AppResult<Asset> {
        val asset = assets.find { it.id == id }
        return if (asset != null) AppResult.Success(asset)
        else AppResult.Error(NoSuchElementException("Asset not found"))
    }

    override suspend fun importAssets(assets: List<Asset>, projectId: String): AppResult<List<Asset>> {
        if (shouldReturnError) return AppResult.Error(Exception("Fake error"))
        projectAssetsMap.getOrPut(projectId) { mutableListOf() }.addAll(assets)
        this.assets.addAll(assets)
        return AppResult.Success(assets)
    }

    override suspend fun updateAsset(asset: Asset): AppResult<Asset> {
        val index = assets.indexOfFirst { it.id == asset.id }
        return if (index >= 0) {
            assets[index] = asset
            AppResult.Success(asset)
        } else AppResult.Error(NoSuchElementException("Asset not found"))
    }

    override suspend fun deleteAsset(id: String): AppResult<Unit> {
        assets.removeIf { it.id == id }
        for (list in projectAssetsMap.values) {
            list.removeIf { it.id == id }
        }
        return AppResult.Success(Unit)
    }
}
