package com.example.glucoseguard.ui.record

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.databinding.FragmentGlucoseInputBinding
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class GlucoseInputFragment : Fragment() {
    private var _binding: FragmentGlucoseInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by viewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREAN)
    
    private var editRecordId: Long = -1L
    private var isEditModeInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlucoseInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editRecordId = arguments?.getLong("recordId") ?: -1L
        
        if (editRecordId != -1L) {
            binding.btnSave.text = "수정하기"
            observeRecordForEdit()
        } else {
            // 입력 모드일 때 현재 시간으로 자동 초기화
            calendar.timeInMillis = System.currentTimeMillis()
            updateDateTimeButtons()
        }

        binding.btnDatePicker.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeButtons()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnTimePicker.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                updateDateTimeButtons()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        binding.btnSave.setOnClickListener {
            val valueStr = binding.etGlucoseValue.text.toString()
            if (valueStr.isEmpty()) {
                Toast.makeText(requireContext(), "혈당 수치를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val value = valueStr.toInt()
            val categoryId = binding.chipGroupCategory.checkedChipId
            if (categoryId == View.NO_ID) {
                Toast.makeText(requireContext(), "구분을 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = binding.chipGroupCategory.findViewById<Chip>(categoryId).text.toString()
            val memo = binding.etMemo.text.toString()

            if (editRecordId != -1L) {
                viewModel.updateGlucose(editRecordId, value, category, memo, calendar.timeInMillis)
                Toast.makeText(requireContext(), "수정되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insertGlucose(value, category, memo, calendar.timeInMillis)
                Toast.makeText(requireContext(), "저장되었습니다", Toast.LENGTH_SHORT).show()
            }
            findNavController().navigateUp()
        }
    }

    private fun observeRecordForEdit() {
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { records ->
            if (isEditModeInitialized) return@observe
            
            records.find { it.id == editRecordId }?.let { record ->
                binding.etGlucoseValue.setText(record.value.toString())
                binding.etMemo.setText(record.memo)
                calendar.timeInMillis = record.timestamp
                updateDateTimeButtons()
                
                for (i in 0 until binding.chipGroupCategory.childCount) {
                    val chip = binding.chipGroupCategory.getChildAt(i) as Chip
                    if (chip.text == record.category) {
                        chip.isChecked = true
                        break
                    }
                }
                isEditModeInitialized = true
            }
        }
    }

    private fun updateDateTimeButtons() {
        binding.btnDatePicker.text = dateFormat.format(calendar.time)
        binding.btnTimePicker.text = timeFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
