package com.example.glucoseguard.ui.record

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.databinding.FragmentInsulinInputBinding
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class InsulinInputFragment : Fragment() {
    private var _binding: FragmentInsulinInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by activityViewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREAN)

    private var editRecordId: Long = -1L
    private var isEditModeInitialized = false
    private var saving = false
    private var originalType = "일반"
    private var originalMemo = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsulinInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editRecordId = arguments?.getLong("recordId") ?: -1L

        if (editRecordId != -1L) {
            binding.btnSaveInsulin.text = "수정하기"
            observeRecordForEdit()
        } else {
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

        binding.btnSaveInsulin.setOnClickListener {
            if (saving) return@setOnClickListener
            val dosageStr = binding.etInsulinDosage.text.toString()
            if (dosageStr.isEmpty()) {
                Toast.makeText(requireContext(), "투여량을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dosage = dosageStr.toFloatOrNull()
            if (!com.example.glucoseguard.util.GlucosePolicy.validDosage(dosage)) {
                binding.etInsulinDosage.error = getString(com.example.glucoseguard.R.string.invalid_dosage)
                return@setOnClickListener
            }
            if (calendar.timeInMillis > System.currentTimeMillis() + 60000) {
                Toast.makeText(requireContext(), com.example.glucoseguard.R.string.future_time_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val siteId = binding.chipGroupSite.checkedChipId
            if (siteId == View.NO_ID) {
                Toast.makeText(requireContext(), "투여 부위를 선택하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val site = binding.chipGroupSite.findViewById<Chip>(siteId).text.toString()

            val type = binding.etInsulinType.text.toString().trim().ifBlank { originalType }
            val memo = binding.etInsulinMemo.text.toString().trim()
            saving = true; binding.btnSaveInsulin.isEnabled = false
            val result: (Boolean) -> Unit = { success ->
                saving = false
                _binding?.let {
                    it.btnSaveInsulin.isEnabled = true
                    if (isAdded) {
                        Toast.makeText(requireContext(), if (success) com.example.glucoseguard.R.string.saving_success else com.example.glucoseguard.R.string.save_failed, Toast.LENGTH_SHORT).show()
                        if (success) findNavController().navigateUp()
                    }
                }
            }
            if (editRecordId != -1L) viewModel.updateInsulin(editRecordId, type, dosage!!, site, memo, calendar.timeInMillis, result)
            else viewModel.insertInsulin(type, dosage!!, site, memo, calendar.timeInMillis, result)
        }
    }

    private fun observeRecordForEdit() {
        viewModel.allInsulinRecords.observe(viewLifecycleOwner) { records ->
            if (isEditModeInitialized) return@observe
            records.find { it.id == editRecordId }?.let { record ->
                binding.etInsulinDosage.setText(record.dosage.toString())
                originalType = record.type; originalMemo = record.memo
                binding.etInsulinType.setText(record.type); binding.etInsulinMemo.setText(record.memo)
                calendar.timeInMillis = record.timestamp
                updateDateTimeButtons()
                for (i in 0 until binding.chipGroupSite.childCount) {
                    val chip = binding.chipGroupSite.getChildAt(i) as Chip
                    if (chip.text == record.injectionSite) {
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
        isEditModeInitialized = false
    }
}
