package com.beatsandbeyond.video_editor.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.databinding.FragmentHomeBinding
import com.beatsandbeyond.video_editor.feature.home.model.HomeUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The entry point of the app — displays recent projects or an empty state.
 *
 * Phase 1: Always shows the empty state (no projects exist yet).
 * Phase 2: Will display a grid of saved projects.
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnNewProject.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_mediaImportFragment)
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

    private fun renderState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
            }
            is HomeUiState.Empty -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
            }
            is HomeUiState.Content -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                // Phase 2: populate project grid
            }
            is HomeUiState.Error -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
            }
        }
    }
}
