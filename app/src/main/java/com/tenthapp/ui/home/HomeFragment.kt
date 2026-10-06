package com.example.glucoseguard.ui.home

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import com.example.glucoseguard.databinding.FragmentHomeBinding
import com.example.glucoseguard.ui.adapter.RecordAdapter
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by viewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private lateinit var adapter: RecordAdapter
    
    // 페이징 관련 변수
    private var currentPage = 1
    private val itemsPerPage = 5
    private var allGlucoseInsulinRecords = listOf<Any>()

    // 고정된 목표 범위 (70~180 mg/dL)
    private val TARGET_MIN = 70
    private val TARGET_MAX = 180

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProfileImage()
        setupDate()
        setupRecyclerViews()
        setupPaginationButtons()
        setupButtons()
        observeData()
    }

    private fun setupProfileImage() {
        val sharedPref = requireActivity().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
        // SettingsFragment에서 저장한 "profile_image_path"를 사용합니다.
        val imagePath = sharedPref.getString("profile_image_path", null)
        
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                try {
                    binding.ivStatusIcon.setImageURI(Uri.fromFile(file))
                } catch (e: Exception) {
                    binding.ivStatusIcon.setImageResource(R.drawable.naturalimage)
                }
            } else {
                binding.ivStatusIcon.setImageResource(R.drawable.naturalimage)
            }
        } else {
            binding.ivStatusIcon.setImageResource(R.drawable.naturalimage)
        }
    }

    private fun setupDate() {
        val sdf = SimpleDateFormat("yyyy.MM.dd (E)", Locale.getDefault())
        binding.tvTodayDate.text = sdf.format(Date())
    }

    private fun setupRecyclerViews() {
        adapter = RecordAdapter(
            onEditClick = { item -> handleEdit(item) },
            onDeleteClick = { item -> showDeleteDialog(item) }
        )
        binding.rvRecentRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentRecords.adapter = adapter
    }

    private fun setupPaginationButtons() {
        binding.btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePagedList()
            }
        }
        binding.btnNext.setOnClickListener {
            val totalPages = Math.max(1, Math.ceil(allGlucoseInsulinRecords.size.toDouble() / itemsPerPage).toInt())
            if (currentPage < totalPages) {
                currentPage++
                updatePagedList()
            }
        }
    }

    private fun handleEdit(item: Any) {
        when (item) {
            is GlucoseRecord -> {
                val bundle = Bundle().apply { putLong("recordId", item.id) }
                findNavController().navigate(R.id.action_home_to_glucoseInput, bundle)
            }
            is InsulinRecord -> {
                val bundle = Bundle().apply { putLong("recordId", item.id) }
                findNavController().navigate(R.id.action_home_to_insulinInput, bundle)
            }
            is MealRecord -> {
                showEditMealDialog(item)
            }
        }
    }

    private fun showEditMealDialog(record: MealRecord) {
        val bottomSheet = MealInputBottomSheet(initialMemo = record.memo) { newMemo ->
            viewModel.updateMeal(record.id, newMemo, record.timestamp)
        }
        bottomSheet.show(childFragmentManager, MealInputBottomSheet.TAG)
    }

    private fun showDeleteDialog(item: Any) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.nav_record)
            .setMessage(R.string.medical_disclaimer) // Should be "Are you sure?"
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteRecord(item)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupButtons() {
        binding.fabAdd.setOnClickListener {
            showAddOptionsBottomSheet()
        }
    }

    private fun showAddOptionsBottomSheet() {
        val bottomSheet = AddRecordBottomSheet(
            onGlucoseClick = { findNavController().navigate(R.id.action_home_to_glucoseInput) },
            onInsulinClick = { findNavController().navigate(R.id.action_home_to_insulinInput) },
            onMealClick = { showMealMemoDialog() }
        )
        bottomSheet.show(childFragmentManager, AddRecordBottomSheet.TAG)
    }

    private fun showMealMemoDialog() {
        val bottomSheet = MealInputBottomSheet { memo ->
            viewModel.insertMeal(memo)
        }
        bottomSheet.show(childFragmentManager, MealInputBottomSheet.TAG)
    }

    private fun observeData() {
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { glucoseRecords ->
            viewModel.allInsulinRecords.observe(viewLifecycleOwner) { insulinRecords ->
                allGlucoseInsulinRecords = (glucoseRecords + insulinRecords)
                    .sortedByDescending { 
                        when (it) {
                            is GlucoseRecord -> it.timestamp
                            is InsulinRecord -> it.timestamp
                            else -> 0L
                        }
                    }
                updatePagedList()
                updateSummary(glucoseRecords, insulinRecords)
            }
        }
    }

    private fun updatePagedList() {
        val totalPages = Math.max(1, Math.ceil(allGlucoseInsulinRecords.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages

        val start = (currentPage - 1) * itemsPerPage
        val end = Math.min(start + itemsPerPage, allGlucoseInsulinRecords.size)
        
        val pagedList = if (allGlucoseInsulinRecords.isEmpty()) {
            emptyList<Any>()
        } else {
            allGlucoseInsulinRecords.subList(start, end)
        }

        adapter.submitList(pagedList)
        binding.tvPageInfo.text = "$currentPage / $totalPages"
        
        binding.btnPrev.isEnabled = currentPage > 1
        binding.btnNext.isEnabled = currentPage < totalPages
    }

    private fun updateSummary(glucoseRecords: List<GlucoseRecord>, insulinRecords: List<InsulinRecord>) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        binding.tvTargetRange.text = "${getString(R.string.guide_target)}: $TARGET_MIN~$TARGET_MAX"

        val todayGlucose = glucoseRecords.filter { it.timestamp >= today }
        val avg = if (todayGlucose.isNotEmpty()) todayGlucose.map { it.value }.average().toInt() else 0
        binding.tvAvgGlucose.text = avg.toString()

        val todayInsulin = insulinRecords.filter { it.timestamp >= today }
        val total = todayInsulin.sumOf { it.dosage.toDouble() }
        binding.tvTotalInsulin.text = String.format(Locale.getDefault(), "%.1f", total)

        generateInsights(avg, TARGET_MIN, TARGET_MAX, todayGlucose, todayInsulin)
    }

    private fun generateInsights(avg: Int, min: Int, max: Int, todayGlucose: List<GlucoseRecord>, todayInsulin: List<InsulinRecord>) {
        if (todayGlucose.isEmpty()) {
            binding.tvStatusTitle.text = "${getString(R.string.msg_today_status)}: ${getString(R.string.msg_no_data)}"
            binding.tvStatusMessage.text = getString(R.string.msg_no_data_desc)
            binding.tvInsightText.text = getString(R.string.msg_no_data_insight)
            return
        }

        when {
            avg < min -> {
                binding.tvStatusTitle.text = getString(R.string.status_low)
                binding.tvStatusTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_low))
                binding.tvStatusMessage.text = getString(R.string.msg_low_desc)
                binding.tvInsightText.text = getString(R.string.msg_low_insight)
            }
            avg > max -> {
                binding.tvStatusTitle.text = getString(R.string.status_high)
                binding.tvStatusTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_high))
                binding.tvStatusMessage.text = getString(R.string.msg_high_desc)
                
                val afterMealHigh = todayGlucose.filter { it.category.contains("후") && it.value > 180 }
                if (afterMealHigh.isNotEmpty()) {
                    binding.tvInsightText.text = getString(R.string.msg_high_after_meal_insight)
                } else {
                    binding.tvInsightText.text = getString(R.string.msg_high_insight)
                }
            }
            else -> {
                binding.tvStatusTitle.text = getString(R.string.status_normal)
                binding.tvStatusTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                binding.tvStatusMessage.text = getString(R.string.msg_normal_desc)
                binding.tvInsightText.text = getString(R.string.msg_normal_insight)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
