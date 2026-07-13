package com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.databinding.BottomSheetAssetPickerBinding
import com.beatsandbeyond.video_editor.feature.projectdetail.adapter.AssetPickerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bottom sheet that lets the user pick an existing project asset to add to a track.
 *
 * @param Mode AUDIO  → only audio assets are shown (added to the AUDIO track).
 * @param Mode OVERLAY → image/video assets are shown (added to the OVERLAY track).
 *
 * Assets are resolved via [assetResolver] (set by the host) rather than passed through
 * the Bundle, keeping the domain [Asset] model free of Android SDK (Parcelable) deps.
 */
@AndroidEntryPoint
class AssetPickerBottomSheet : BottomSheetDialogFragment() {

    enum class Mode { AUDIO, OVERLAY }

    private var _binding: BottomSheetAssetPickerBinding? = null
    private val binding get() = _binding!!

    /** Resolves an asset ID to its [Asset]. Set by the host before showing. */
    var assetResolver: ((String) -> Asset?)? = null

    /** Callback invoked with the chosen asset ID. */
    var onAssetPicked: ((assetId: String) -> Unit)? = null

    private lateinit var mode: Mode
    private lateinit var assetIds: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getString(ARG_MODE)?.let { Mode.valueOf(it) } ?: Mode.AUDIO
        assetIds = requireArguments().getStringArrayList(ARG_ASSET_IDS).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAssetPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sheetTitle.text = when (mode) {
            Mode.AUDIO -> getString(com.beatsandbeyond.video_editor.R.string.sheet_add_audio_title)
            Mode.OVERLAY -> getString(com.beatsandbeyond.video_editor.R.string.sheet_add_overlay_title)
        }

        val assets = assetIds.mapNotNull { assetResolver?.invoke(it) }
        val filtered = when (mode) {
            Mode.AUDIO -> assets.filter { it.mediaType == MediaType.AUDIO }
            Mode.OVERLAY -> assets.filter { it.mediaType == MediaType.IMAGE || it.mediaType == MediaType.VIDEO }
        }

        if (filtered.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.assetGrid.visibility = View.GONE
            binding.emptyText.text = when (mode) {
                Mode.AUDIO -> getString(com.beatsandbeyond.video_editor.R.string.sheet_no_audio)
                Mode.OVERLAY -> getString(com.beatsandbeyond.video_editor.R.string.sheet_no_overlay)
            }
            return
        }

        val adapter = AssetPickerAdapter { asset ->
            onAssetPicked?.invoke(asset.id)
            dismiss()
        }
        adapter.submitList(filtered)
        binding.assetGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.assetGrid.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MODE = "arg_mode"
        private const val ARG_ASSET_IDS = "arg_asset_ids"

        fun newInstance(mode: Mode, assetIds: List<String>): AssetPickerBottomSheet {
            return AssetPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                    putStringArrayList(ARG_ASSET_IDS, ArrayList(assetIds))
                }
            }
        }
    }
}
