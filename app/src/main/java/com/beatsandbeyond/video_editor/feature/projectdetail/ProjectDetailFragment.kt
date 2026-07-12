package com.beatsandbeyond.video_editor.feature.projectdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.databinding.FragmentProjectDetailBinding
import com.beatsandbeyond.video_editor.feature.projectdetail.adapter.TimelineClipAdapter
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Project editor screen — displays a video preview and a read-only timeline strip.
 *
 * Phase 2 scope:
 * - Video preview via ExoPlayer (through [PreviewEngine] — no ExoPlayer imports here)
 * - Toolbar with project name and back navigation
 * - Clip strip at the bottom (horizontal RecyclerView, read-only)
 * - Play/Pause FAB control
 *
 * The Fragment manages the SurfaceView lifecycle carefully:
 * - [SurfaceHolder.Callback.surfaceCreated] → [ProjectDetailViewModel.onSurfaceReady]
 * - [SurfaceHolder.Callback.surfaceDestroyed] → [ProjectDetailViewModel.onSurfaceDestroyed]
 */
@AndroidEntryPoint
class ProjectDetailFragment : BaseFragment<FragmentProjectDetailBinding>() {

    private val viewModel: ProjectDetailViewModel by viewModels()
    private val args: ProjectDetailFragmentArgs by navArgs()
    private lateinit var clipAdapter: TimelineClipAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentProjectDetailBinding =
        FragmentProjectDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSurface()
        setupClipStrip()
        setupPlayPauseButton()
        observeUiState()
        viewModel.loadProject(args.projectId)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSurface() {
        binding.previewSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                viewModel.onSurfaceReady(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // ExoPlayer handles resolution changes automatically
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                viewModel.onSurfaceDestroyed()
            }
        })
    }

    private fun setupClipStrip() {
        clipAdapter = TimelineClipAdapter()
        binding.timelineClipStrip.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false,
            )
            adapter = clipAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupPlayPauseButton() {
        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ProjectDetailUiState) {
        when (state) {
            is ProjectDetailUiState.Loading -> showLoading()
            is ProjectDetailUiState.Ready -> showReady(state)
            is ProjectDetailUiState.Error -> showError(state.message)
        }
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.previewSurface.visibility = View.INVISIBLE
        binding.bottomBar.visibility = View.GONE
    }

    private fun showReady(state: ProjectDetailUiState.Ready) {
        binding.loadingIndicator.visibility = View.GONE
        binding.previewSurface.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE

        // Toolbar
        binding.toolbar.title = state.project.name

        // Play/Pause button icon
        val playPauseIcon = if (state.isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        binding.btnPlayPause.setIconResource(playPauseIcon)

        // Playback state overlays
        binding.bufferingIndicator.visibility =
            if (state.playbackState is PlaybackState.Buffering) View.VISIBLE else View.GONE

        // Timeline clip strip
        clipAdapter.submitList(state.assets)
    }

    private fun showError(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.previewSurface.visibility = View.INVISIBLE
        binding.bottomBar.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }
}
