package com.beatsandbeyond.video_editor.core.domain.usecase.asset

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns all imported [Asset]s across all projects as a reactive [Flow]. */
class GetAllAssetsUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
) {
    operator fun invoke(): Flow<AppResult<List<Asset>>> =
        assetRepository.getAllAssets()
}
