package com.example.glucoseguard.report

import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import java.text.SimpleDateFormat
import java.util.*

class ReportDataBuilder {

    fun buildReportData(
        periodDays: Int,
        glucoseRecords: List<GlucoseRecord>,
        insulinRecords: List<InsulinRecord>,
        mealRecords: List<com.example.glucoseguard.data.model.MealRecord> = emptyList(),
        target: com.example.glucoseguard.util.GlucosePolicy.Target = com.example.glucoseguard.util.GlucosePolicy.Target()
    ): ReportData {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val startDate = if (periodDays > 0) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -(periodDays - 1))
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        } else {
            // All time
            listOfNotNull(glucoseRecords.minOfOrNull { it.timestamp }, insulinRecords.minOfOrNull { it.timestamp }, mealRecords.minOfOrNull { it.timestamp }).minOrNull() ?: now
        }

        val filteredGlucose = glucoseRecords.filter { it.timestamp in startDate..now }
        val filteredInsulin = insulinRecords.filter { it.timestamp in startDate..now }

        val stats = calculateStats(filteredGlucose, filteredInsulin, target)
        val analysisGenerator = ReportAnalysisGenerator()
        val analysisItems = analysisGenerator.generateAnalysisText(stats)

        val dailyEntries = buildDailyEntries(filteredGlucose)
        val weeklyEntries = buildWeeklyEntries(filteredGlucose)
        val monthlyEntries = buildMonthlyEntries(filteredGlucose)
        val hourlyEntries = buildHourlyEntries(filteredGlucose)

        val periodTitle = if (periodDays > 0) "최근 ${periodDays}일" else "전체 기간"

        return ReportData(
            periodTitle = periodTitle,
            startDate = startDate,
            endDate = now,
            generationDate = now,
            glucoseRecords = filteredGlucose,
            insulinRecords = filteredInsulin,
            stats = stats,
            analysisItems = analysisItems,
            dailyGlucoseEntries = dailyEntries,
            weeklyGlucoseEntries = weeklyEntries,
            monthlyGlucoseEntries = monthlyEntries,
            hourlyGlucoseEntries = hourlyEntries,
            mealRecords = mealRecords.filter { it.timestamp in startDate..now }
        )
    }

    private fun buildDailyEntries(records: List<GlucoseRecord>): List<ChartEntry> {
        val grouped = records.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        return grouped.keys.sorted().mapIndexed { index, time ->
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            ChartEntry(index.toFloat(), avg, sdf.format(Date(time)))
        }
    }

    private fun buildWeeklyEntries(records: List<GlucoseRecord>): List<ChartEntry> {
        val grouped = records.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.add(Calendar.DAY_OF_YEAR, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        return grouped.keys.sorted().mapIndexed { index, time ->
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            val c = Calendar.getInstance().apply { timeInMillis = time }
            ChartEntry(index.toFloat(), avg, "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.WEEK_OF_MONTH)}주")
        }
    }

    private fun buildMonthlyEntries(records: List<GlucoseRecord>): List<ChartEntry> {
        val grouped = records.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val sdf = SimpleDateFormat("yy/MM", Locale.getDefault())
        return grouped.keys.sorted().mapIndexed { index, time ->
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            ChartEntry(index.toFloat(), avg, sdf.format(Date(time)))
        }
    }

    private fun buildHourlyEntries(records: List<GlucoseRecord>): List<ChartEntry> {
        if (records.isEmpty()) return emptyList()
        val latestTimestamp = records.maxOf { it.timestamp }
        val cal = Calendar.getInstance().apply { timeInMillis = latestTimestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        
        val dailyRecords = records.filter { it.timestamp in startOfDay until (startOfDay + 86400000L) }.sortedBy { it.timestamp }
        return dailyRecords.map { 
            ChartEntry((it.timestamp - startOfDay) / 3600000f, it.value.toFloat(), String.format("%02d:00", ((it.timestamp - startOfDay) / 3600000f).toInt()))
        }
    }

    private fun calculateStats(glucose: List<GlucoseRecord>, insulin: List<InsulinRecord>, target: com.example.glucoseguard.util.GlucosePolicy.Target): ReportStats {
        val avg = if (glucose.isNotEmpty()) glucose.map { it.value }.average().toInt() else 0
        val max = if (glucose.isNotEmpty()) glucose.maxOf { it.value } else 0
        val min = if (glucose.isNotEmpty()) glucose.minOf { it.value } else 0
        
        val normalCount = glucose.count { com.example.glucoseguard.util.GlucosePolicy.classify(it.value, it.category, target) == com.example.glucoseguard.util.GlucosePolicy.Status.IN_RANGE }
        val highCount = glucose.count { com.example.glucoseguard.util.GlucosePolicy.classify(it.value, it.category, target) == com.example.glucoseguard.util.GlucosePolicy.Status.ABOVE_TARGET }
        val lowCount = glucose.count { it.value < 70 }
        val percentage = if (glucose.isNotEmpty()) (normalCount * 100) / glucose.size else 0

        val totalInsulin = insulin.sumOf { it.dosage.toDouble() }.toFloat()
        val avgInsulin = if (insulin.isNotEmpty()) totalInsulin / insulin.size else 0f
        
        val statusSummary = when {
            lowCount > 0 -> "저혈당 기록이 있어 주의가 필요합니다."
            highCount > (glucose.size * 0.2) -> "최근 기록에서 고혈당 구간이 일부 확인되었습니다."
            glucose.isEmpty() -> "기록이 없습니다."
            else -> "입력한 기록 기준 요약입니다. 측정하지 않은 시간의 상태는 알 수 없습니다."
        }

        return ReportStats(
            avgGlucose = avg,
            maxGlucose = max,
            minGlucose = min,
            normalRangePercentage = percentage,
            highCount = highCount,
            lowCount = lowCount,
            totalInsulin = totalInsulin,
            avgInsulin = avgInsulin,
            insulinCount = insulin.size,
            statusSummary = statusSummary
        )
    }
}
