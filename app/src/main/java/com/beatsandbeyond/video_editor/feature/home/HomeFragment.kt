package com.beatsandbeyond.video_editor.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.databinding.FragmentHomeBinding
import com.beatsandbeyond.video_editor.feature.home.adapter.ProjectAdapter
import com.beatsandbeyond.video_editor.feature.home.model.HomeUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

/**
 * The app's entry point — displays the project grid or empty state.
 *
 * Phase 2: Shows a 2-column grid of saved projects. Tapping opens the project.
 * Long-pressing shows a delete confirmation dialog.
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var projectAdapter: ProjectAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupProjectGrid()
        setupClickListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        // Force the ViewModel to reload projects when Fragment resumes
        // This ensures the Flow collection is active after navigation
        viewModel.loadProjects()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_asset_library -> {
                    findNavController().navigate(R.id.action_homeFragment_to_assetLibraryFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupProjectGrid() {
        projectAdapter = ProjectAdapter(
            onProjectClicked = { project ->
                val action = HomeFragmentDirections
                    .actionHomeFragmentToProjectDetailFragment(project.id)
                findNavController().navigate(action)
            },
            onProjectLongClicked = { project ->
                showDeleteConfirmation(project.id, project.name)
            },
        )
        binding.projectGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), PROJECT_GRID_SPAN_COUNT)
            adapter = projectAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClickListeners() {
        binding.btnNewProject.setOnClickListener {
            showCreateProjectDialog()
        }
        binding.fabNewProject.setOnClickListener {
            showCreateProjectDialog()
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

    private fun renderState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
                binding.projectGrid.visibility = View.GONE
                binding.fabNewProject.hide()
            }
            is HomeUiState.Empty -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.projectGrid.visibility = View.GONE
                binding.fabNewProject.hide()
            }
            is HomeUiState.Content -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.projectGrid.visibility = View.VISIBLE
                binding.fabNewProject.show()
                projectAdapter.submitList(state.projects)
            }
            is HomeUiState.Error -> {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.projectGrid.visibility = View.GONE
                binding.fabNewProject.hide()
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────

    private fun showDeleteConfirmation(projectId: String, projectName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_delete_project_title))
            .setMessage(getString(R.string.dialog_delete_project_message, projectName))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                viewModel.deleteProject(projectId)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showCreateProjectDialog() {
        val defaultName = generateDefaultProjectName()
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_project, null)
        val tilProjectName = dialogView.findViewById<TextInputLayout>(R.id.tilProjectName)
        val etProjectName = dialogView.findViewById<TextInputEditText>(R.id.etProjectName)
        
        etProjectName.setText(defaultName)
        etProjectName.selectAll()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_new_project_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_next), null)
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val enteredName = etProjectName.text?.toString()?.trim()
            if (enteredName.isNullOrEmpty()) {
                tilProjectName.error = getString(R.string.dialog_new_project_error_empty)
            } else {
                dialog.dismiss()
                val action = HomeFragmentDirections
                    .actionHomeFragmentToMediaImportFragment(enteredName)
                findNavController().navigate(action)
            }
        }
    }

    private fun generateDefaultProjectName(): String {
        val formatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return "Project — ${formatter.format(java.util.Date())}"
    }

    companion object {
        private const val PROJECT_GRID_SPAN_COUNT = 2
    }
}
