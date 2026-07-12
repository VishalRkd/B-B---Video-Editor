package com.beatsandbeyond.video_editor.feature.projectdetail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.databinding.ItemTimelineClipBinding

/**
 * Adapter for the horizontal timeline clip strip in [ProjectDetailFragment].
 * Each item represents one [Asset] (a clip on the primary video track).
 * Phase 2: display-only. Editing gestures are Phase 3.
 */
class TimelineClipAdapter : ListAdapter<Asset, TimelineClipAdapter.TimelineClipViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineClipViewHolder {
        val binding = ItemTimelineClipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TimelineClipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineClipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimelineClipViewHolder(
        private val binding: ItemTimelineClipBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asset: Asset) {
            // Load a frame from the video at the 500ms mark as the clip thumbnail
            if (asset.mediaType == MediaType.VIDEO) {
                binding.clipThumbnail.load(android.net.Uri.parse(asset.mediaUri)) {
                    videoFrameMillis(500L)
                    crossfade(true)
                    placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                }
            } else {
                binding.clipThumbnail.load(android.net.Uri.parse(asset.mediaUri)) {
                    crossfade(true)
                    placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                }
            }

            binding.clipName.text = asset.displayName.substringBeforeLast(".")
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Asset>() {
            override fun areItemsTheSame(oldItem: Asset, newItem: Asset): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Asset, newItem: Asset): Boolean =
                oldItem == newItem
        }
    }
}
