package com.example.glucoseguard.ui.home
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.*
import com.example.glucoseguard.databinding.FragmentHomeBinding
import com.example.glucoseguard.ui.adapter.RecordAdapter
import com.example.glucoseguard.ui.viewmodel.*
import com.example.glucoseguard.util.*
import java.text.SimpleDateFormat
import java.util.*
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DiabetesViewModel by activityViewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }
    private lateinit var adapter: RecordAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false); return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecordAdapter(onEditClick = { edit(it) }, onDeleteClick = { item ->
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message).setPositiveButton(R.string.delete_action) { _, _ -> viewModel.deleteRecord(item) }
                .setNegativeButton(android.R.string.cancel, null).show()
        }, onItemClick = { if (it is MealRecord) edit(it) })
        binding.rvRecentRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentRecords.adapter = adapter
        binding.btnRecordGlucose.setOnClickListener { findNavController().navigate(R.id.action_home_to_glucoseInput) }
        binding.btnMoreRecord.setOnClickListener {
            AddRecordBottomSheet().show(childFragmentManager, AddRecordBottomSheet.TAG)
        }
        binding.btnAllRecords.setOnClickListener { findNavController().navigate(R.id.navigation_record) }
        binding.btnReport.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings, Bundle().apply { putBoolean("openReport", true) })
        }
        binding.btnTargets.setOnClickListener { findNavController().navigate(R.id.guideTargetFragment) }
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { refresh() }
        viewModel.allInsulinRecords.observe(viewLifecycleOwner) { refresh() }
        viewModel.allMealRecords.observe(viewLifecycleOwner) { refresh() }
        refresh()
    }
    override fun onResume() { super.onResume(); if (_binding != null) refresh() }
    private fun refresh() {
        val now = System.currentTimeMillis()
        val glucose = viewModel.allGlucoseRecords.value.orEmpty().filter { it.timestamp <= now }.sortedByDescending { it.timestamp }
        val insulin = viewModel.allInsulinRecords.value.orEmpty().filter { it.timestamp <= now }
        val meals = viewModel.allMealRecords.value.orEmpty().filter { it.timestamp <= now }
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0);set(Calendar.MINUTE, 0);set(Calendar.SECOND, 0);set(Calendar.MILLISECOND, 0) }.timeInMillis
        val week = Calendar.getInstance().apply { timeInMillis = today;add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
        val target = TargetPreferences.read(requireContext())
        binding.tvTodayDate.text = SimpleDateFormat("M월 d일 EEEE", Locale.getDefault()).format(Date(now))
        binding.tvTargetRange.text = getString(R.string.target_summary_format, target.min, target.beforeMax, target.afterMax)
        val latest = glucose.firstOrNull()
        binding.tvLastGlucose.text = latest?.value?.toString() ?: "—"
        binding.tvUnit.visibility = if (latest == null) View.GONE else View.VISIBLE
        binding.tvLatestMeta.text = if (latest == null) getString(R.string.first_record_hint) else
            "${CategoryMapper.getTranslatedCategory(requireContext(), latest.category)} · ${SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(Date(latest.timestamp))}"
        if (latest == null) {
            binding.tvStatusTitle.text = getString(R.string.no_records_yet)
            binding.tvStatusTitle.setTextColor(requireContext().getColor(R.color.text_sub))
            binding.tvStatusMessage.text = getString(R.string.first_record_desc)
        } else {
            val status = GlucosePolicy.classify(latest.value, latest.category, target)
            val (label, color) = when(status) {
                GlucosePolicy.Status.LOW -> R.string.status_low to R.color.status_low
                GlucosePolicy.Status.BELOW_TARGET -> R.string.below_target to R.color.status_warning
                GlucosePolicy.Status.ABOVE_TARGET -> R.string.above_target to R.color.status_high
                else -> R.string.within_target to R.color.primary
            }
            binding.tvStatusTitle.setText(label); binding.tvStatusTitle.setTextColor(requireContext().getColor(color))
            binding.tvStatusMessage.text = if (now - latest.timestamp > 86400000L) getString(R.string.old_reading_note) else getString(R.string.record_status_note)
        }
        val todayG = glucose.filter { it.timestamp >= today }
        val lows = todayG.count { it.value < 70 }
        val highs = todayG.count { GlucosePolicy.classify(it.value, it.category, target) == GlucosePolicy.Status.ABOVE_TARGET }
        binding.tvInsightText.text = when {
            lows > 0 -> getString(R.string.low_record_note, lows)
            highs > 0 -> getString(R.string.high_record_note, highs)
            todayG.isEmpty() -> getString(R.string.today_empty_note)
            else -> getString(R.string.today_record_note, todayG.size)
        }
        binding.tvInsightText.setTextColor(requireContext().getColor(if(lows > 0) R.color.status_low else R.color.text_sub))
        binding.tvTodayCount.text = getString(R.string.record_count_format, todayG.size)
        val days = glucose.filter { it.timestamp >= week }.map { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(it.timestamp)) }.distinct().size
        binding.tvWeekDays.text = getString(R.string.week_days_format, days)
        binding.insulinSummary.visibility = if (insulin.isEmpty()) View.GONE else View.VISIBLE
        binding.tvTotalInsulin.text = getString(R.string.insulin_total_format, insulin.filter { it.timestamp >= today }.sumOf { it.dosage.toDouble() })
        val all = (glucose + insulin + meals).sortedByDescending { time(it) }
        adapter.submitList(all.take(8))
        binding.tvEmptyRecords.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun time(item: Any): Long = when(item) { is GlucoseRecord -> item.timestamp; is InsulinRecord -> item.timestamp; is MealRecord -> item.timestamp; else -> 0 }
    private fun edit(item: Any) {
        val bundle = Bundle().apply { putLong("recordId", when(item) { is GlucoseRecord -> item.id; is InsulinRecord -> item.id; else -> -1 }) }
        when(item) {
            is GlucoseRecord -> findNavController().navigate(R.id.action_home_to_glucoseInput, bundle)
            is InsulinRecord -> findNavController().navigate(R.id.action_home_to_insulinInput, bundle)
            is MealRecord -> MealInputBottomSheet.edit(item).show(childFragmentManager, MealInputBottomSheet.TAG)
        }
    }
    override fun onDestroyView() { binding.rvRecentRecords.adapter = null; _binding = null; super.onDestroyView() }
}
