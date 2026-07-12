package com.beatsandbeyond.video_editor.feature.home.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.beatsandbeyond.video_editor.core.domain.model.Project
import com.beatsandbeyond.video_editor.databinding.ItemProjectCardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for the project grid on the Home screen.
 *
 * @param onProjectClicked Called when the user taps a project card to open it.
 * @param onProjectLongClicked Called when the user long-presses to show a delete dialog.
 */
class ProjectAdapter(
    private val onProjectClicked: (Project) -> Unit,
    private val onProjectLongClicked: (Project) -> Unit,
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(project: Project) {
            binding.projectName.text = project.name
            binding.projectDate.text = dateFormatter.format(Date(project.updatedAt))

            val durationSecs = project.timeline.totalDurationMs / 1000L
            binding.projectDuration.text = String.format(
                Locale.getDefault(),
                "%d:%02d",
                durationSecs / 60,
                durationSecs % 60,
            )

            // Thumbnail — Phase 2: load from project.thumbnailUri if set.
            // Phase 3 will generate and cache a thumbnail from the first frame.
            val thumbnailUri = project.thumbnailUri
            if (thumbnailUri != null) {
                binding.projectThumbnail.load(Uri.parse(thumbnailUri)) {
                    crossfade(true)
                    placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                }
            } else {
                binding.projectThumbnail.setImageResource(0)
                binding.projectThumbnail.setBackgroundColor(
                    binding.root.context.getColor(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                )
            }

            binding.root.setOnClickListener { onProjectClicked(project) }
            binding.root.setOnLongClickListener {
                onProjectLongClicked(project)
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Project>() {
            override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean =
                oldItem == newItem
        }
    }
}
