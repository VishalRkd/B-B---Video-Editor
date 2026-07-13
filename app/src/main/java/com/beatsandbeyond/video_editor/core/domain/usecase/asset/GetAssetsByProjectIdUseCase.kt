package com.beatsandbeyond.video_editor.core.domain.usecase.asset

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case responsible for retrieving all [Asset]s associated with a specific project ID.
 */
class GetAssetsByProjectIdUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
) {
    operator fun invoke(projectId: String): Flow<AppResult<List<Asset>>> =
        assetRepository.getAssetsByProject(projectId)
}
