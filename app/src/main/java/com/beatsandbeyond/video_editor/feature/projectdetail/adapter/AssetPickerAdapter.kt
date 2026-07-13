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
import com.beatsandbeyond.video_editor.databinding.ItemAssetPickerBinding

/**
 * RecyclerView adapter for the [com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.AssetPickerBottomSheet].
 *
 * Shows a thumbnail (video frame or image) plus the asset name. Stateless — the click
 * callback is supplied by the host sheet.
 */
class AssetPickerAdapter(
    private val onItemClick: (Asset) -> Unit,
) : ListAdapter<Asset, AssetPickerViewHolder>(AssetPickerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetPickerViewHolder {
        val binding = ItemAssetPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AssetPickerViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: AssetPickerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class AssetPickerDiffCallback : DiffUtil.ItemCallback<Asset>() {
        override fun areItemsTheSame(oldItem: Asset, newItem: Asset): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Asset, newItem: Asset): Boolean =
            oldItem == newItem
    }
}

class AssetPickerViewHolder(
    private val binding: ItemAssetPickerBinding,
    private val onItemClick: (Asset) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(asset: Asset) {
        binding.assetName.text = asset.displayName.substringBeforeLast(".")
        binding.assetThumbnail.load(android.net.Uri.parse(asset.mediaUri)) {
            if (asset.mediaType == MediaType.VIDEO) videoFrameMillis(0L)
            crossfade(true)
            placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
        }
        binding.root.setOnClickListener { onItemClick(asset) }
    }
}
