package com.beatsandbeyond.video_editor.feature.assetlibrary.model

import com.beatsandbeyond.video_editor.core.domain.model.Asset

/**
 * All possible UI states for the Asset Library screen.
 */
sealed class AssetLibraryUiState {

    /** Assets are being loaded. */
    data object Loading : AssetLibraryUiState()

    /** No assets have been imported yet. */
    data object Empty : AssetLibraryUiState()

    /**
     * Assets loaded and ready to display.
     * @param assets All imported assets, ordered by import date descending.
     */
    data class Content(val assets: List<Asset>) : AssetLibraryUiState()

    /**
     * An error occurred while loading assets.
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : AssetLibraryUiState()
}
