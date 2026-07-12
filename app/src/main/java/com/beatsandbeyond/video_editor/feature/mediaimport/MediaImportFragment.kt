package com.beatsandbeyond.video_editor.feature.mediaimport

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.utils.extensions.arePermissionsGranted
import com.beatsandbeyond.video_editor.databinding.FragmentMediaImportBinding
import com.beatsandbeyond.video_editor.feature.mediaimport.adapter.MediaItemAdapter
import com.beatsandbeyond.video_editor.feature.mediaimport.model.MediaImportUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen media picker that queries device storage via MediaStore.
 *
 * Phase 2: The FAB now creates a project, imports selected media as assets,
 * then navigates to the Project Detail screen. All navigation is driven by
 * [MediaImportViewModel.navigationEvent] — the Fragment never decides where to go.
 */
@AndroidEntryPoint
class MediaImportFragment : BaseFragment<FragmentMediaImportBinding>() {

    private val viewModel: MediaImportViewModel by viewModels()
    private lateinit var mediaAdapter: MediaItemAdapter

    // ── Permission launcher ──────────────────────────────────────────────────

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onPermissionGranted()
        } else {
            val shouldShowRationale = getRequiredPermissions().any { permission ->
                shouldShowRequestPermissionRationale(permission)
            }
            if (shouldShowRationale) {
                viewModel.onPermissionDenied(shouldShowRationale = true)
            } else {
                viewModel.onPermissionPermanentlyDenied()
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentMediaImportBinding =
        FragmentMediaImportBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupFab()
        observeUiState()
        observeNavigationEvents()
        checkPermissions()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        mediaAdapter = MediaItemAdapter { item ->
            viewModel.toggleSelection(item)
        }
        binding.mediaGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = mediaAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { viewModel.applyFilter(MediaFilter.ALL) }
        binding.chipVideos.setOnClickListener { viewModel.applyFilter(MediaFilter.VIDEOS_ONLY) }
        binding.chipPhotos.setOnClickListener { viewModel.applyFilter(MediaFilter.IMAGES_ONLY) }
    }

    private fun setupFab() {
        binding.fabAddMedia.setOnClickListener {
            viewModel.createProjectFromSelection()
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

    private fun observeNavigationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is MediaImportViewModel.NavigationEvent.OpenProject -> {
                            val action = MediaImportFragmentDirections
                                .actionMediaImportFragmentToProjectDetailFragment(event.projectId)
                            findNavController().navigate(action)
                        }
                    }
                }
            }
        }
    }

    // ── State renderers ───────────────────────────────────────────────────────

    private fun renderState(state: MediaImportUiState) {
        when (state) {
            is MediaImportUiState.Checking -> showLoading()
            is MediaImportUiState.PermissionRequired -> showPermissionRequired(state.shouldShowRationale)
            is MediaImportUiState.PermissionPermanentlyDenied -> showPermissionPermanentlyDenied()
            is MediaImportUiState.Loading -> showLoading()
            is MediaImportUiState.Content -> showContent(state)
            is MediaImportUiState.Error -> showError(state.message)
        }
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.mediaGrid.visibility = View.GONE
        binding.permissionContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showContent(state: MediaImportUiState.Content) {
        binding.loadingIndicator.visibility = View.GONE
        binding.permissionContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.mediaGrid.visibility = View.VISIBLE

        mediaAdapter.submitList(state.items)
        mediaAdapter.updateSelection(state.selectedIds)

        // Creating project overlay
        binding.creatingProjectOverlay.visibility =
            if (state.isCreatingProject) View.VISIBLE else View.GONE

        // FAB
        if (state.hasSelection && !state.isCreatingProject) {
            binding.fabAddMedia.show()
            binding.fabAddMedia.text = getString(R.string.fab_add_count, state.selectedCount)
        } else {
            binding.fabAddMedia.hide()
        }

        // One-shot error Snackbar
        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    private fun showPermissionRequired(shouldShowRationale: Boolean) {
        binding.loadingIndicator.visibility = View.GONE
        binding.mediaGrid.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.permissionContainer.visibility = View.VISIBLE
        binding.fabAddMedia.hide()

        binding.permissionTitle.text = getString(R.string.permission_title)
        binding.permissionMessage.text = if (shouldShowRationale) {
            getString(R.string.permission_message_rationale)
        } else {
            getString(R.string.permission_message)
        }
        binding.btnGrantPermission.text = getString(R.string.btn_grant_permission)
        binding.btnGrantPermission.setOnClickListener {
            permissionLauncher.launch(getRequiredPermissions())
        }
    }

    private fun showPermissionPermanentlyDenied() {
        binding.loadingIndicator.visibility = View.GONE
        binding.mediaGrid.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.permissionContainer.visibility = View.VISIBLE
        binding.fabAddMedia.hide()

        binding.permissionTitle.text = getString(R.string.permission_denied_title)
        binding.permissionMessage.text = getString(R.string.permission_denied_message)
        binding.btnGrantPermission.text = getString(R.string.btn_open_settings)
        binding.btnGrantPermission.setOnClickListener { openAppSettings() }
    }

    private fun showError(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.mediaGrid.visibility = View.GONE
        binding.permissionContainer.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.errorMessage.text = message
        binding.fabAddMedia.hide()
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    private fun checkPermissions() {
        val permissions = getRequiredPermissions()
        if (requireContext().arePermissionsGranted(*permissions)) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun getRequiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        })
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }
}
