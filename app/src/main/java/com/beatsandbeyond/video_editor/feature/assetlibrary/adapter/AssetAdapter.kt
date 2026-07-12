package com.beatsandbeyond.video_editor.feature.assetlibrary.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.databinding.ItemAssetCardBinding

/** Adapter for the asset library 3-column grid. */
class AssetAdapter : ListAdapter<Asset, AssetAdapter.AssetViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val binding = ItemAssetCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AssetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AssetViewHolder(
        private val binding: ItemAssetCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(asset: Asset) {
            val uri = Uri.parse(asset.mediaUri)

            if (asset.mediaType == MediaType.VIDEO) {
                binding.assetThumbnail.load(uri) {
                    videoFrameMillis(500L)
                    crossfade(true)
                    placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                }
                binding.videoBadge.visibility = android.view.View.VISIBLE
                val durationSecs = (asset.durationMs ?: 0L) / 1000L
                binding.videoBadge.text = String.format(
                    java.util.Locale.getDefault(), "%d:%02d", durationSecs / 60, durationSecs % 60
                )
            } else {
                binding.assetThumbnail.load(uri) {
                    crossfade(true)
                    placeholder(com.beatsandbeyond.video_editor.R.color.color_surface_variant)
                }
                binding.videoBadge.visibility = android.view.View.GONE
            }

            binding.assetName.text = asset.displayName.substringBeforeLast(".")
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
