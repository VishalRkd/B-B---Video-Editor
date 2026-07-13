package com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.databinding.BottomSheetSpeedBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpeedBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSpeedBinding? = null
    private val binding get() = _binding!!

    var onSpeedSelected: ((Float) -> Unit)? = null

    private var currentSpeed: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSpeed = requireArguments().getFloat(ARG_SPEED, 1.0f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetSpeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.sliderSpeed.value = currentSpeed
        binding.txtSpeedValue.text = String.format("%.1fx", currentSpeed)

        binding.sliderSpeed.addOnChangeListener { _, value, _ ->
            binding.txtSpeedValue.text = String.format("%.1fx", value)
        }

        binding.btnApply.setOnClickListener {
            onSpeedSelected?.invoke(binding.sliderSpeed.value)
            dismiss()
        }

        binding.btnReset.setOnClickListener {
            binding.sliderSpeed.value = 1.0f
        }
        
        binding.tabStandard.setOnClickListener {
            binding.containerStandard.visibility = View.VISIBLE
            binding.containerCurve.visibility = View.GONE
        }
        
        binding.tabCurve.setOnClickListener {
            binding.containerStandard.visibility = View.GONE
            binding.containerCurve.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SPEED = "arg_speed"

        fun newInstance(currentSpeed: Float): SpeedBottomSheet {
            return SpeedBottomSheet().apply {
                arguments = Bundle().apply {
                    putFloat(ARG_SPEED, currentSpeed)
                }
            }
        }
    }
}
