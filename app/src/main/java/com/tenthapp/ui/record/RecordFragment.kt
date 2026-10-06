package com.example.glucoseguard.ui.record

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import com.example.glucoseguard.databinding.FragmentRecordBinding
import com.example.glucoseguard.ui.adapter.RecordAdapter
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import com.google.android.material.tabs.TabLayout
import com.example.glucoseguard.ui.home.AddRecordBottomSheet
import com.example.glucoseguard.ui.home.MealInputBottomSheet

class RecordFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by activityViewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private lateinit var adapter: RecordAdapter
    private var allRecords = listOf<Any>()
    
    // 페이징 관련 변수
    private var currentPage = 1
    private val itemsPerPage = 10
    private var currentFilteredList = listOf<Any>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecordAdapter(
            onEditClick = { item ->
                handleEdit(item)
            },
            onDeleteClick = { item ->
                showDeleteDialog(item)
            },
            onItemClick = { item ->
                if (item is MealRecord) {
                    showMemoDialog(item)
                }
            }
        )
        binding.rvAllRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllRecords.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentPage = 1
                val position = tab?.position ?: 0
                
                // 메모 탭일 때 테마 색상 변경
                if (position == 3) {
                    binding.tabLayout.setSelectedTabIndicatorColor(android.graphics.Color.parseColor("#087F78"))
                    binding.tabLayout.setTabTextColors(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_sub),
                        android.graphics.Color.parseColor("#087F78")
                    )
                } else {
                    binding.tabLayout.setSelectedTabIndicatorColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
                    binding.tabLayout.setTabTextColors(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_sub),
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
                    )
                }
                
                filterRecords(position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        setupPaginationButtons()
        setupFab()
        observeData()
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddOptionsBottomSheet()
        }
    }

    private fun showAddOptionsBottomSheet() {
        val bottomSheet = AddRecordBottomSheet()
        bottomSheet.show(childFragmentManager, AddRecordBottomSheet.TAG)
    }

    private fun showMealMemoDialog() {
        val bottomSheet = MealInputBottomSheet()
        bottomSheet.show(childFragmentManager, MealInputBottomSheet.TAG)
    }

    private fun setupPaginationButtons() {
        binding.btnPrev.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updateRecyclerView()
            }
        }
        binding.btnNext.setOnClickListener {
            val totalPages = Math.ceil(currentFilteredList.size.toDouble() / itemsPerPage).toInt()
            if (currentPage < totalPages) {
                currentPage++
                updateRecyclerView()
            }
        }
    }

    private fun showMemoDialog(record: MealRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("건강 메모 상세")
            .setMessage(record.memo)
            .setPositiveButton("수정") { _, _ -> showEditMealDialog(record) }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun handleEdit(item: Any) {
        when (item) {
            is GlucoseRecord -> {
                val bundle = Bundle().apply { putLong("recordId", item.id) }
                findNavController().navigate(R.id.action_navigation_record_to_glucoseInputFragment, bundle)
            }
            is InsulinRecord -> {
                val bundle = Bundle().apply { putLong("recordId", item.id) }
                findNavController().navigate(R.id.action_navigation_record_to_insulinInputFragment, bundle)
            }
            is MealRecord -> {
                showEditMealDialog(item)
            }
        }
    }

    private fun showEditMealDialog(record: MealRecord) {
        val bottomSheet = MealInputBottomSheet.edit(record)
        bottomSheet.show(childFragmentManager, MealInputBottomSheet.TAG)
    }

    private fun observeData() {
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { refreshRecords() }
        viewModel.allInsulinRecords.observe(viewLifecycleOwner) { refreshRecords() }
        viewModel.allMealRecords.observe(viewLifecycleOwner) { refreshRecords() }
    }
    private fun refreshRecords() {
        allRecords = (viewModel.allGlucoseRecords.value.orEmpty() + viewModel.allInsulinRecords.value.orEmpty() + viewModel.allMealRecords.value.orEmpty()).sortedByDescending {
            when(it) { is GlucoseRecord -> it.timestamp; is InsulinRecord -> it.timestamp; is MealRecord -> it.timestamp; else -> 0L }
        }
        filterRecords(binding.tabLayout.selectedTabPosition)
    }
    private fun filterRecords(position: Int) {
        currentFilteredList = when (position) {
            0 -> allRecords
            1 -> allRecords.filterIsInstance<GlucoseRecord>()
            2 -> allRecords.filterIsInstance<InsulinRecord>()
            3 -> allRecords.filterIsInstance<MealRecord>()
            else -> allRecords
        }
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        val totalPages = Math.max(1, Math.ceil(currentFilteredList.size.toDouble() / itemsPerPage).toInt())
        if (currentPage > totalPages) currentPage = totalPages

        val start = (currentPage - 1) * itemsPerPage
        val end = Math.min(start + itemsPerPage, currentFilteredList.size)
        
        val pagedList = if (currentFilteredList.isEmpty()) {
            emptyList()
        } else {
            currentFilteredList.subList(start, end)
        }

        adapter.submitList(pagedList)
        binding.tvPageInfo.text = "$currentPage / $totalPages"
        
        binding.btnPrev.isEnabled = currentPage > 1
        binding.btnNext.isEnabled = currentPage < totalPages
    }

    private fun showDeleteDialog(item: Any) {
        AlertDialog.Builder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("이 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteRecord(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
