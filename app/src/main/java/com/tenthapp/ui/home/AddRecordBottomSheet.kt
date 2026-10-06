package com.example.glucoseguard.ui.home

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.glucoseguard.databinding.BottomSheetAddRecordBinding

class AddRecordBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddRecordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddGlucose.setOnClickListener {
            requireParentFragment().findNavController().navigate(com.example.glucoseguard.R.id.glucoseInputFragment)
            dismiss()
        }

        binding.btnAddInsulin.setOnClickListener {
            requireParentFragment().findNavController().navigate(com.example.glucoseguard.R.id.insulinInputFragment)
            dismiss()
        }

        binding.btnAddMeal.setOnClickListener {
            MealInputBottomSheet().show(parentFragmentManager,MealInputBottomSheet.TAG)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddRecordBottomSheet"
    }
}
