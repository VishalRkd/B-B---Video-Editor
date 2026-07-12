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
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.utils.extensions.arePermissionsGranted
import com.beatsandbeyond.video_editor.databinding.FragmentMediaImportBinding
import com.beatsandbeyond.video_editor.feature.mediaimport.adapter.MediaItemAdapter
import com.beatsandbeyond.video_editor.feature.mediaimport.model.MediaImportUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen media picker that queries device storage via MediaStore.
 *
 * Handles:
 * - Runtime permission request (API-version aware: API 33+ uses granular permissions)
 * - Loading and displaying device media in a 3-column grid
 * - Multi-selection with visual feedback
 * - Filter bar (All / Videos / Photos)
 * - Confirm FAB enabled only when ≥1 item is selected
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
            val selectedItems = viewModel.getSelectedItems()
            // Phase 2: Pass selected items to project creation flow
            // For now, navigate back to home (items are ready to consume)
            findNavController().navigateUp()
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

    private fun renderState(state: MediaImportUiState) {
        when (state) {
            is MediaImportUiState.Checking -> {
                showLoading()
            }
            is MediaImportUiState.PermissionRequired -> {
                showPermissionRequired(state.shouldShowRationale)
            }
            is MediaImportUiState.PermissionPermanentlyDenied -> {
                showPermissionPermanentlyDenied()
            }
            is MediaImportUiState.Loading -> {
                showLoading()
            }
            is MediaImportUiState.Content -> {
                showContent(state)
            }
            is MediaImportUiState.Error -> {
                showError(state.message)
            }
        }
    }

    // ── State renderers ───────────────────────────────────────────────────────

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

        // FAB visibility and label
        if (state.hasSelection) {
            binding.fabAddMedia.show()
            binding.fabAddMedia.text = "Add ${state.selectedCount}"
        } else {
            binding.fabAddMedia.hide()
        }
    }

    private fun showPermissionRequired(shouldShowRationale: Boolean) {
        binding.loadingIndicator.visibility = View.GONE
        binding.mediaGrid.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
        binding.permissionContainer.visibility = View.VISIBLE
        binding.fabAddMedia.hide()

        binding.permissionTitle.text = "Storage Permission Required"
        binding.permissionMessage.text = if (shouldShowRationale) {
            "B&B Video Editor needs access to your photos and videos to import media into your projects."
        } else {
            "Grant permission to access your media library."
        }
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

        binding.permissionTitle.text = "Permission Denied"
        binding.permissionMessage.text =
            "Please enable storage access for B&B Video Editor in your device settings."
        binding.btnGrantPermission.text = "Open Settings"
        binding.btnGrantPermission.setOnClickListener {
            openAppSettings()
        }
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
