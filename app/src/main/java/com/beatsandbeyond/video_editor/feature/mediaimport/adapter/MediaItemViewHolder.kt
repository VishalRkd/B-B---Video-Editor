package com.beatsandbeyond.video_editor.feature.mediaimport.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.utils.FileUtils
import com.beatsandbeyond.video_editor.databinding.ItemMediaGridBinding

/**
 * ViewHolder for a single media item in the import grid.
 *
 * Responsibilities:
 * - Load thumbnail via Coil (handles both images and video frames)
 * - Show duration badge for video items
 * - Render selected/deselected visual state
 */
class MediaItemViewHolder(
    private val binding: ItemMediaGridBinding,
    private val onItemClick: (MediaItem) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MediaItem, isSelected: Boolean) {
        loadThumbnail(item)
        bindDurationBadge(item)
        bindSelectionState(isSelected)
        binding.root.setOnClickListener { onItemClick(item) }
    }

    private fun loadThumbnail(item: MediaItem) {
        binding.thumbnailImage.load(item.uri) {
            crossfade(true)
            if (item.mediaType == MediaType.VIDEO) {
                videoFrameMillis(0L)
            }
            error(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun bindDurationBadge(item: MediaItem) {
        if (item.mediaType == MediaType.VIDEO && item.durationMs > 0) {
            binding.durationBadge.visibility = View.VISIBLE
            binding.durationBadge.text = FileUtils.formatDuration(item.durationMs)
        } else {
            binding.durationBadge.visibility = View.GONE
        }
    }

    private fun bindSelectionState(isSelected: Boolean) {
        binding.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        binding.selectionCheckbox.isChecked = isSelected
        binding.selectionCheckbox.visibility = if (isSelected) View.VISIBLE else View.GONE
    }
}
