package com.example.glucoseguard.report

import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord

data class ReportData(
    val periodTitle: String,
    val startDate: Long,
    val endDate: Long,
    val generationDate: Long,
    val glucoseRecords: List<GlucoseRecord>,
    val insulinRecords: List<InsulinRecord>,
    val stats: ReportStats,
    val analysisItems: List<String>,
    val dailyGlucoseEntries: List<ChartEntry> = emptyList(),
    val weeklyGlucoseEntries: List<ChartEntry> = emptyList(),
    val monthlyGlucoseEntries: List<ChartEntry> = emptyList(),
    val hourlyGlucoseEntries: List<ChartEntry> = emptyList()
)

data class ChartEntry(
    val x: Float,
    val y: Float,
    val label: String
)

data class ReportStats(
    val avgGlucose: Int,
    val maxGlucose: Int,
    val minGlucose: Int,
    val normalRangePercentage: Int,
    val highCount: Int,
    val lowCount: Int,
    val totalInsulin: Float,
    val avgInsulin: Float,
    val insulinCount: Int,
    val statusSummary: String
)
