package com.example.glucoseguard.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.glucoseguard.databinding.BottomSheetAddRecordBinding

class AddRecordBottomSheet(
    private val onGlucoseClick: () -> Unit,
    private val onInsulinClick: () -> Unit,
    private val onMealClick: () -> Unit
) : BottomSheetDialogFragment() {

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
            onGlucoseClick()
            dismiss()
        }

        binding.btnAddInsulin.setOnClickListener {
            onInsulinClick()
            dismiss()
        }

        binding.btnAddMeal.setOnClickListener {
            onMealClick()
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
