package com.beatsandbeyond.video_editor.feature.mediaimport.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.databinding.ItemMediaGridBinding

/**
 * RecyclerView adapter for the media import grid.
 *
 * Uses [ListAdapter] with [DiffUtil] for efficient, animated updates when
 * the filter changes or new media is loaded.
 *
 * Selection state is passed in on each update (via [submitList] + [selectedIds])
 * rather than being stored in the adapter, keeping the adapter stateless.
 */
class MediaItemAdapter(
    private val onItemClick: (MediaItem) -> Unit,
) : ListAdapter<MediaItem, MediaItemViewHolder>(MediaItemDiffCallback()) {

    private var selectedIds: Set<Long> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val binding = ItemMediaGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return MediaItemViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, isSelected = item.id in selectedIds)
    }

    /**
     * Updates the selection state and refreshes only the items whose selection status changed.
     * This avoids a full rebind of the entire list on every tap.
     */
    fun updateSelection(newSelectedIds: Set<Long>) {
        val changedPositions = (0 until itemCount).filter { position ->
            val itemId = getItem(position).id
            val wasSelected = itemId in selectedIds
            val isNowSelected = itemId in newSelectedIds
            wasSelected != isNowSelected
        }
        selectedIds = newSelectedIds
        changedPositions.forEach { notifyItemChanged(it) }
    }

    private class MediaItemDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
            oldItem == newItem
    }
}
