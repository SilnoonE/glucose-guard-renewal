package com.example.glucoseguard.ui.record
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.R
import com.example.glucoseguard.databinding.FragmentGlucoseInputBinding
import com.example.glucoseguard.ui.viewmodel.*
import com.example.glucoseguard.util.GlucosePolicy
import java.text.SimpleDateFormat
import java.util.*
class GlucoseInputFragment : Fragment() {
    private var _binding: FragmentGlucoseInputBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DiabetesViewModel by activityViewModels { DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository) }
    private val calendar = Calendar.getInstance()
    private var editRecordId = -1L
    private var initialized = false
    private var saving = false
    private val codes = listOf("fasting", "breakfast_before", "breakfast_after", "lunch_before", "lunch_after", "dinner_before", "dinner_after", "bedtime")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGlucoseInputBinding.inflate(inflater, container, false); return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        editRecordId = arguments?.getLong("recordId", -1) ?: -1
        for (i in codes.indices) (binding.chipGroupCategory.getChildAt(i) as Chip).tag = codes[i]
        updateTime()
        val prefs = requireContext().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
        if (editRecordId < 0) selectCategory(prefs.getString("last_glucose_category", "") ?: "")
        else {
            binding.btnSave.setText(R.string.edit_action)
            viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { records ->
                if (!initialized) records.find { it.id == editRecordId }?.let {
                    binding.etGlucoseValue.setText(it.value.toString()); binding.etMemo.setText(it.memo)
                    calendar.timeInMillis = it.timestamp;selectCategory(it.category);updateTime();initialized = true
                }
            }
        }
        binding.btnDatePicker.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d -> calendar.set(y, m, d);updateTime() }, calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        }
        binding.btnTimePicker.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m -> calendar.set(Calendar.HOUR_OF_DAY,h);calendar.set(Calendar.MINUTE,m);calendar.set(Calendar.SECOND,0);updateTime() },calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),true).show()
        }
        binding.btnSave.setOnClickListener {
            if (saving) return@setOnClickListener
            val value = binding.etGlucoseValue.text.toString().trim().toIntOrNull()
            if (!GlucosePolicy.validGlucose(value)) { binding.etGlucoseValue.error = getString(R.string.invalid_glucose);return@setOnClickListener }
            val id = binding.chipGroupCategory.checkedChipId
            if (id == View.NO_ID) { Toast.makeText(requireContext(),R.string.context_hint,Toast.LENGTH_SHORT).show();return@setOnClickListener }
            if (calendar.timeInMillis > System.currentTimeMillis() + 60000) { Toast.makeText(requireContext(),R.string.future_time_error,Toast.LENGTH_SHORT).show();return@setOnClickListener }
            val category = binding.chipGroupCategory.findViewById<Chip>(id).tag.toString()
            val memo = binding.etMemo.text.toString().trim()
            saving = true;binding.btnSave.isEnabled = false;binding.btnSave.setText(R.string.saving)
            val result: (Boolean) -> Unit = { success ->
                saving = false
                if (success) prefs.edit().putString("last_glucose_category", category).apply()
                _binding?.let { b ->
                    b.btnSave.isEnabled = true;b.btnSave.setText(if(editRecordId < 0) R.string.btn_save else R.string.edit_action)
                    if (isAdded) {
                        Toast.makeText(requireContext(),if(success) R.string.saving_success else R.string.save_failed,Toast.LENGTH_SHORT).show()
                        if (success) findNavController().navigateUp()
                    }
                }
            }
            if (editRecordId < 0) viewModel.insertGlucose(value!!,category,memo,calendar.timeInMillis,result)
            else viewModel.updateGlucose(editRecordId,value!!,category,memo,calendar.timeInMillis,result)
        }
    }
    private fun selectCategory(category: String) {
        val code = GlucosePolicy.categoryCode(category)
        for(i in codes.indices) if(codes[i] == code) (binding.chipGroupCategory.getChildAt(i) as Chip).isChecked = true
    }
    private fun updateTime() {
        binding.btnDatePicker.text = SimpleDateFormat("yyyy.MM.dd",Locale.getDefault()).format(calendar.time)
        binding.btnTimePicker.text = SimpleDateFormat("HH:mm",Locale.getDefault()).format(calendar.time)
    }
    override fun onDestroyView() { _binding = null; initialized = false;super.onDestroyView() }
}
