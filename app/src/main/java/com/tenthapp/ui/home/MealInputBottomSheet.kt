package com.example.glucoseguard.ui.home

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.glucoseguard.databinding.BottomSheetMealInputBinding

class MealInputBottomSheet : BottomSheetDialogFragment() {
    private val viewModel: com.example.glucoseguard.ui.viewmodel.DiabetesViewModel by activityViewModels {
        com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory((requireActivity().application as com.example.glucoseguard.DiabetesApplication).repository)
    }
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

        val initialMemo=arguments?.getString("memo").orEmpty()
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
            binding.btnSave.isEnabled=false
            val result: (Boolean) -> Unit = { success ->
                _binding?.let { b ->
                    b.btnSave.isEnabled=true
                    if(success) dismiss() else b.layoutMemo.error=getString(com.example.glucoseguard.R.string.save_failed)
                }
            }
            val id=arguments?.getLong("id",-1) ?: -1
            if(id>=0) viewModel.updateMeal(id,memo,requireArguments().getLong("timestamp"),result)
            else viewModel.insertMeal(memo,onResult=result)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MealInputBottomSheet"
        fun edit(record: com.example.glucoseguard.data.model.MealRecord) = MealInputBottomSheet().apply {
            arguments=Bundle().apply { putLong("id",record.id);putLong("timestamp",record.timestamp);putString("memo",record.memo) }
        }
    }
}
