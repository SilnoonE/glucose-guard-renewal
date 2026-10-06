package com.example.glucoseguard.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.glucoseguard.R
import com.example.glucoseguard.databinding.BottomSheetReportOptionsBinding

class ReportOptionsBottomSheet(
    private val onGenerate: (periodDays: Int, options: ReportOptions) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReportOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetReportOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGeneratePdf.setOnClickListener {
            val periodDays = when (binding.chipGroupPeriod.checkedChipId) {
                R.id.chip_7days -> 7
                R.id.chip_14days -> 14
                R.id.chip_30days -> 30
                R.id.chip_all -> -1
                else -> 14
            }

            val options = ReportOptions(
                includeGlucoseSummary = true,
                includeInsulinSummary = true,
                includeDailyChart = true,
                includeWeeklyChart = true,
                includeMonthlyChart = true,
                includeDayDetailChart = true,
                includeAiAnalysis = true,
                includeMemo = true
            )

            onGenerate(periodDays, options)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReportOptionsBottomSheet"
    }
}

data class ReportOptions(
    val includeGlucoseSummary: Boolean,
    val includeInsulinSummary: Boolean,
    val includeDailyChart: Boolean,
    val includeWeeklyChart: Boolean,
    val includeMonthlyChart: Boolean,
    val includeDayDetailChart: Boolean,
    val includeAiAnalysis: Boolean,
    val includeMemo: Boolean
)
