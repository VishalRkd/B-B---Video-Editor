package com.beatsandbeyond.video_editor.feature.assetlibrary

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.usecase.asset.GetAllAssetsUseCase
import com.beatsandbeyond.video_editor.feature.assetlibrary.model.AssetLibraryUiState
import com.beatsandbeyond.video_editor.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Asset Library screen.
 * Exposes all imported assets as a reactive [StateFlow].
 */
@HiltViewModel
class AssetLibraryViewModel @Inject constructor(
    private val getAllAssetsUseCase: GetAllAssetsUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<AssetLibraryUiState>(AssetLibraryUiState.Loading)
    val uiState: StateFlow<AssetLibraryUiState> = _uiState.asStateFlow()

    init {
        loadAssets()
    }

    private fun loadAssets() {
        launchSafely {
            getAllAssetsUseCase().collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Loading -> AssetLibraryUiState.Loading
                    is AppResult.Success -> {
                        if (result.data.isEmpty()) AssetLibraryUiState.Empty
                        else AssetLibraryUiState.Content(result.data)
                    }
                    is AppResult.Error -> AssetLibraryUiState.Error(
                        result.message ?: "Failed to load assets"
                    )
                }
            }
        }
    }
}
