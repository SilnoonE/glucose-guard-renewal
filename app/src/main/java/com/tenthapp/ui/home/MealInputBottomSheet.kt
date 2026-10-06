package com.example.glucoseguard.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.glucoseguard.databinding.BottomSheetMealInputBinding

class MealInputBottomSheet(
    private val initialMemo: String = "",
    private val onSave: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMealInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMealInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (initialMemo.isNotEmpty()) {
            binding.tvTitle.text = "기록 수정하기"
            binding.etMemo.setText(initialMemo)
            binding.btnSave.text = "수정 완료"
        }

        binding.btnSave.setOnClickListener {
            val memo = binding.etMemo.text.toString().trim()
            if (memo.isEmpty()) {
                binding.layoutMemo.error = "내용을 입력해 주세요."
                return@setOnClickListener
            }
            onSave(memo)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MealInputBottomSheet"
    }
}
