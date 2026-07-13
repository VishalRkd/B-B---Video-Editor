package com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.databinding.BottomSheetFilterPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bottom sheet for applying a [VideoFilter] to the selected clip.
 *
 * Two tabs:
 * - [Tab.FILTER]:  preset filters (None, Vintage, Vignette).
 * - [Tab.ADJUST]:  continuous adjustments (Brightness, Contrast, Saturation sliders).
 *
 * The chosen filter is delivered via [onFilterSelected].
 */
@AndroidEntryPoint
class FilterPickerBottomSheet : BottomSheetDialogFragment() {

    enum class Tab { FILTER, ADJUST }

    private var _binding: BottomSheetFilterPickerBinding? = null
    private val binding get() = _binding!!

    /** Callback invoked with the chosen [VideoFilter]. */
    var onFilterSelected: ((VideoFilter) -> Unit)? = null

    private lateinit var clipId: String
    private var currentFilter: VideoFilter = VideoFilter.None
    private var activeTab: Tab = Tab.FILTER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipId = requireArguments().getString(ARG_CLIP_ID) ?: ""
        currentFilter = requireArguments().getSerializable(ARG_FILTER) as? VideoFilter
            ?: VideoFilter.None
        activeTab = requireArguments().getString(ARG_TAB)?.let { Tab.valueOf(it) } ?: Tab.FILTER
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetFilterPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        renderTab()
    }

    private fun setupTabs() {
        binding.tabFilter.setOnClickListener {
            activeTab = Tab.FILTER
            renderTab()
        }
        binding.tabAdjust.setOnClickListener {
            activeTab = Tab.ADJUST
            renderTab()
        }
    }

    private fun renderTab() {
        val isFilter = activeTab == Tab.FILTER
        binding.filterPresets.visibility = if (isFilter) View.VISIBLE else View.GONE
        binding.adjustControls.visibility = if (isFilter) View.GONE else View.VISIBLE
        binding.tabFilter.isSelected = isFilter
        binding.tabAdjust.isSelected = !isFilter

        if (isFilter) {
            renderFilterPresets()
        } else {
            renderAdjustControls()
        }
    }

    private fun renderFilterPresets() {
        // Simple preset buttons. Selected preset is highlighted.
        val presets = listOf(
            VideoFilter.None,
            VideoFilter.Vintage(0.6f),
            VideoFilter.Vignette(0.5f),
        )
        binding.filterPresets.removeAllViews()
        presets.forEach { filter ->
            val button = android.widget.Button(requireContext()).apply {
                text = filter.displayName
                isSelected = filter::class == currentFilter::class
                setOnClickListener {
                    currentFilter = filter
                    onFilterSelected?.invoke(filter)
                    dismiss()
                }
            }
            binding.filterPresets.addView(button)
        }
    }

    private fun renderAdjustControls() {
        // Brightness / Contrast / Saturation sliders, each committing on change.
        binding.brightnessSlider.addOnChangeListener { _, value, _ ->
            currentFilter = VideoFilter.Brightness(value)
            onFilterSelected?.invoke(currentFilter)
        }
        binding.contrastSlider.addOnChangeListener { _, value, _ ->
            currentFilter = VideoFilter.Contrast(value)
            onFilterSelected?.invoke(currentFilter)
        }
        binding.saturationSlider.addOnChangeListener { _, value, _ ->
            currentFilter = VideoFilter.Saturation(value)
            onFilterSelected?.invoke(currentFilter)
        }

        // Seed sliders from the current filter if it matches the type.
        when (val f = currentFilter) {
            is VideoFilter.Brightness -> binding.brightnessSlider.value = f.value
            is VideoFilter.Contrast -> binding.contrastSlider.value = f.value
            is VideoFilter.Saturation -> binding.saturationSlider.value = f.value
            else -> {
                binding.brightnessSlider.value = 0f
                binding.contrastSlider.value = 1f
                binding.saturationSlider.value = 1f
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CLIP_ID = "arg_clip_id"
        private const val ARG_FILTER = "arg_filter"
        private const val ARG_TAB = "arg_tab"

        fun newInstance(clipId: String, filter: VideoFilter, tab: Tab = Tab.FILTER): FilterPickerBottomSheet {
            return FilterPickerBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CLIP_ID, clipId)
                    putSerializable(ARG_FILTER, filter)
                    putString(ARG_TAB, tab.name)
                }
            }
        }
    }
}
