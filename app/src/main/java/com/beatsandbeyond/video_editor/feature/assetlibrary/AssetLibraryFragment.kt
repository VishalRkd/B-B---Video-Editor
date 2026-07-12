package com.beatsandbeyond.video_editor.feature.assetlibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.beatsandbeyond.video_editor.databinding.FragmentAssetLibraryBinding
import com.beatsandbeyond.video_editor.feature.assetlibrary.adapter.AssetAdapter
import com.beatsandbeyond.video_editor.feature.assetlibrary.model.AssetLibraryUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Displays all media assets that have been imported into any project. */
@AndroidEntryPoint
class AssetLibraryFragment : BaseFragment<FragmentAssetLibraryBinding>() {

    private val viewModel: AssetLibraryViewModel by viewModels()
    private lateinit var assetAdapter: AssetAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentAssetLibraryBinding =
        FragmentAssetLibraryBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        assetAdapter = AssetAdapter()
        binding.assetGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = assetAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: AssetLibraryUiState) {
        when (state) {
            is AssetLibraryUiState.Loading -> {
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.assetGrid.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
                binding.errorMessage.visibility = View.GONE
            }
            is AssetLibraryUiState.Empty -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.assetGrid.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                binding.errorMessage.visibility = View.GONE
            }
            is AssetLibraryUiState.Content -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.assetGrid.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                binding.errorMessage.visibility = View.GONE
                assetAdapter.submitList(state.assets)
            }
            is AssetLibraryUiState.Error -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.assetGrid.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = state.message
            }
        }
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }
}
