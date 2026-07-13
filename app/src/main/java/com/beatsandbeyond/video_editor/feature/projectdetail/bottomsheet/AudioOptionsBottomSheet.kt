package com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.beatsandbeyond.video_editor.databinding.BottomSheetAudioOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioOptionsBinding? = null
    private val binding get() = _binding!!

    var onVolumeSelected: ((Float) -> Unit)? = null
    // Fade in/out callbacks would go here for Phase 4

    private var currentVolume: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentVolume = requireArguments().getFloat(ARG_VOLUME, 1.0f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetAudioOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.sliderVolume.value = currentVolume
        binding.txtVolumeValue.text = "${(currentVolume * 100).toInt()}%"

        binding.sliderVolume.addOnChangeListener { _, value, _ ->
            binding.txtVolumeValue.text = "${(value * 100).toInt()}%"
        }

        binding.sliderFadeIn.addOnChangeListener { _, value, _ ->
            binding.txtFadeInValue.text = String.format("%.1fs", value)
        }

        binding.sliderFadeOut.addOnChangeListener { _, value, _ ->
            binding.txtFadeOutValue.text = String.format("%.1fs", value)
        }

        binding.btnApply.setOnClickListener {
            onVolumeSelected?.invoke(binding.sliderVolume.value)
            dismiss()
        }

        binding.btnExtractAudio.setOnClickListener {
            // For future implementation (Phase 4)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_VOLUME = "arg_volume"

        fun newInstance(currentVolume: Float): AudioOptionsBottomSheet {
            return AudioOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putFloat(ARG_VOLUME, currentVolume)
                }
            }
        }
    }
}
