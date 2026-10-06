package com.example.glucoseguard.ui.chart

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.MainActivity
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.databinding.FragmentChartBinding
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by activityViewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private var currentFilterPosition = 0 // 0: 일별, 1: 주별, 2: 월별
    private var selectedDetailDate = Calendar.getInstance()
    private val TARGET_MIN get() = com.example.glucoseguard.util.TargetPreferences.read(requireContext()).min.toFloat()
    private val TARGET_MAX get() = com.example.glucoseguard.util.TargetPreferences.read(requireContext()).afterMax.toFloat()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts(binding.lineChart)
        setupCharts(binding.fullLineChart)
        setupCharts(binding.dayDetailChart)
        setupInsulinChart()
        setupFilterTabs()
        setupZoomButtons()
        setupFullScreenControls()
        setupDayDetailControls()
        observeData()
    }

    private fun setupDayDetailControls() {
        updateSelectedDateText()
        binding.btnPrevDay.setOnClickListener {
            selectedDetailDate.add(Calendar.DAY_OF_YEAR, -1)
            updateSelectedDateText()
            refreshCharts()
        }
        binding.btnNextDay.setOnClickListener {
            selectedDetailDate.add(Calendar.DAY_OF_YEAR, 1)
            updateSelectedDateText()
            refreshCharts()
        }
        binding.tvSelectedDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDetailDate.set(year, month, dayOfMonth)
                updateSelectedDateText()
                refreshCharts()
            },
            selectedDetailDate.get(Calendar.YEAR),
            selectedDetailDate.get(Calendar.MONTH),
            selectedDetailDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateSelectedDateText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(selectedDetailDate.time)
    }

    private fun setupFullScreenControls() {
        binding.btnToggleHeight.setOnClickListener {
            (activity as? MainActivity)?.setMainUIVisible(false)
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.layoutFullChart.visibility = View.VISIBLE
            refreshCharts()
        }
        
        binding.btnCloseFullChart.setOnClickListener {
            (activity as? MainActivity)?.setMainUIVisible(true)
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            binding.layoutFullChart.visibility = View.GONE
        }
    }

    private fun setupFilterTabs() {
        binding.tabLayoutFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilterPosition = tab?.position ?: 0
                resetChartsZoom()
                refreshCharts()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun resetChartsZoom() {
        binding.lineChart.fitScreen()
        binding.fullLineChart.fitScreen()
        binding.insulinCombinedChart.fitScreen()
        binding.dayDetailChart.fitScreen()
    }

    private fun setupCharts(chart: BarLineChartBase<*>) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setScaleYEnabled(false) 
            setDrawGridBackground(false)
            minOffset = 15f
            setExtraOffsets(10f, 10f, 10f, 20f)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#424940")
                yOffset = 8f
                granularity = 1f
                isGranularityEnabled = true
                setAvoidFirstLastClipping(true)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#424940")
                setDrawAxisLine(false)
                axisMinimum = 40f
                axisMaximum = 250f
                xOffset = 10f
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupInsulinChart() {
        setupCharts(binding.insulinCombinedChart)
        binding.insulinCombinedChart.apply {
            drawOrder = arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE)
            axisLeft.axisMinimum = 0f
            axisLeft.resetAxisMaximum()
        }
    }

    private fun setupZoomButtons() {
        binding.btnLineZoomIn.setOnClickListener { 
            binding.lineChart.zoom(1.4f, 1f, binding.lineChart.viewPortHandler.contentWidth(), 0f) 
        }
        binding.btnLineZoomOut.setOnClickListener { 
            binding.lineChart.zoom(0.7f, 1f, binding.lineChart.viewPortHandler.contentWidth(), 0f) 
        }
        
        binding.btnBarZoomIn.setOnClickListener { 
            binding.insulinCombinedChart.zoom(1.4f, 1f, binding.insulinCombinedChart.viewPortHandler.contentWidth(), 0f) 
        }
        binding.btnBarZoomOut.setOnClickListener { 
            binding.insulinCombinedChart.zoom(0.7f, 1f, binding.insulinCombinedChart.viewPortHandler.contentWidth(), 0f) 
        }

        binding.btnFullZoomIn.setOnClickListener {
            binding.fullLineChart.zoom(1.4f, 1f, binding.fullLineChart.viewPortHandler.contentWidth(), 0f)
        }
        binding.btnFullZoomOut.setOnClickListener {
            binding.fullLineChart.zoom(0.7f, 1f, binding.fullLineChart.viewPortHandler.contentWidth(), 0f)
        }
    }

    private fun refreshCharts() {
        viewModel.allGlucoseRecords.value?.let { records ->
            when (currentFilterPosition) {
                0 -> updateDailyGlucoseChart(records)
                1 -> updateWeeklyGlucoseChart(records)
                2 -> updateMonthlyGlucoseChart(records)
            }
            updateDayDetailChart(records)
        }
        viewModel.allInsulinRecords.value?.let { updateInsulinChart(it) }
    }

    private fun observeData() {
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { refreshCharts() }
        viewModel.allInsulinRecords.observe(viewLifecycleOwner) { records -> updateInsulinChart(records) }
    }

    private fun updateDailyGlucoseChart(records: List<GlucoseRecord>) {
        if (records.isEmpty()) { 
            binding.lineChart.clear()
            binding.fullLineChart.clear()
            return 
        }

        val grouped = records.groupBy {
            val c = Calendar.getInstance()
            c.timeInMillis = it.timestamp
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        
        val sortedTimes = grouped.keys.sorted()
        if (sortedTimes.isEmpty()) return
        val firstTime = sortedTimes.first()
        
        val allEntries = mutableListOf<Entry>()
        val allColors = mutableListOf<Int>()

        sortedTimes.forEach { time ->
            val dayOffset = ((time - firstTime) / 86400000L).toFloat()
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            allEntries.add(Entry(dayOffset, avg))
            allColors.add(Color.parseColor("#087F78"))
        }

        setupGlucoseChartInternal(binding.fullLineChart, allEntries, emptyList(), allColors, 0, firstTime, true)

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis
        val sevenDaysAgoStart = todayStart - (6 * 86400000L)
        
        val mainColors = mutableListOf<Int>()
        val mainEntries = allEntries.filterIndexed { index, entry ->
            val ts = (entry.x * 86400000L).toLong() + firstTime
            val included = ts >= sevenDaysAgoStart && ts < (todayStart + 86400000L)
            if (included) mainColors.add(allColors[index])
            included
        }
        
        setupGlucoseChartInternal(binding.lineChart, mainEntries, emptyList(), mainColors, 0, firstTime, false)
    }

    private fun updateWeeklyGlucoseChart(records: List<GlucoseRecord>) {
        if (records.isEmpty()) { binding.lineChart.clear(); binding.fullLineChart.clear(); return }
        val grouped = records.groupBy {
            val c = Calendar.getInstance()
            c.timeInMillis = it.timestamp
            c.add(Calendar.DAY_OF_YEAR, -((c.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val sortedTimes = grouped.keys.sorted()
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()
        sortedTimes.forEachIndexed { index, time ->
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            entries.add(Entry(index.toFloat(), avg))
            val c = Calendar.getInstance(); c.timeInMillis = time
            labels.add("${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.WEEK_OF_MONTH)}주")
            colors.add(Color.parseColor("#087F78"))
        }
        setupGlucoseChartInternal(binding.lineChart, entries, labels, colors, 1, 0L, false)
        setupGlucoseChartInternal(binding.fullLineChart, entries, labels, colors, 1, 0L, true)
    }

    private fun updateMonthlyGlucoseChart(records: List<GlucoseRecord>) {
        if (records.isEmpty()) { binding.lineChart.clear(); binding.fullLineChart.clear(); return }
        val grouped = records.groupBy {
            val c = Calendar.getInstance()
            c.timeInMillis = it.timestamp
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val sortedTimes = grouped.keys.sorted()
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()
        sortedTimes.forEachIndexed { index, time ->
            val avg = grouped[time]!!.map { it.value }.average().toFloat()
            entries.add(Entry(index.toFloat(), avg))
            labels.add(SimpleDateFormat("yy/MM", Locale.getDefault()).format(Date(time)))
            colors.add(Color.parseColor("#087F78"))
        }
        setupGlucoseChartInternal(binding.lineChart, entries, labels, colors, 1, 0L, false)
        setupGlucoseChartInternal(binding.fullLineChart, entries, labels, colors, 1, 0L, true)
    }

    private fun setupGlucoseChartInternal(chart: LineChart, entries: List<Entry>, labels: List<String>, colors: List<Int>, mode: Int, startTime: Long, isFull: Boolean) {
        chart.apply {
            xAxis.resetAxisMinimum(); xAxis.resetAxisMaximum()
            axisLeft.resetAxisMinimum(); axisLeft.resetAxisMaximum()
            
            if (entries.isEmpty() && !(mode == 0 && !isFull) && !isFull) { clear(); return }

            val dataSet = LineDataSet(entries, "평균 혈당").apply {
                lineWidth = 4f; circleRadius = 6f; color = Color.parseColor("#087F78")
                if (colors.size == entries.size) setCircleColors(colors) else setCircleColor(Color.parseColor("#087F78"))
                setDrawFilled(true); fillAlpha = 40; fillColor = Color.parseColor("#E3F3EF")
                setDrawValues(false); highLightColor = Color.parseColor("#087F78")
                setDrawHorizontalHighlightIndicator(false); setDrawVerticalHighlightIndicator(true)
            }
            
            data = LineData(dataSet)
            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (mode == 0) SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date((value * 86400000L).toLong() + startTime))
                        else if (value.toInt() in labels.indices) labels[value.toInt()] else ""
                    }
                }
                granularity = 1f
            }
            
            marker = CustomMarkerView(requireContext(), R.layout.layout_chart_marker, labels, "mg/dL", mode, startTime)
            applyLimitLines(this)
            
            axisLeft.apply {
                val maxVal = if (entries.isNotEmpty()) entries.maxOf { it.y } else 250f
                val minVal = if (entries.isNotEmpty()) entries.minOf { it.y } else 40f
                axisMaximum = Math.max(250f, maxVal + 30f)
                axisMinimum = Math.min(40f, minVal - 20f)
            }
            
            fitScreen()
            if (mode == 0 && !isFull) {
                val todayCal = Calendar.getInstance()
                todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
                val todayTs = todayCal.timeInMillis
                xAxis.axisMinimum = (todayTs - (6 * 86400000L) - startTime) / 86400000f
                xAxis.axisMaximum = (todayTs - startTime) / 86400000f
                axisLeft.axisMinimum = 40f
            } else if (!isFull && entries.size > 7) {
                setVisibleXRangeMaximum(7f); moveViewToX(entries.size - 1f)
            }
            notifyDataSetChanged()
            invalidate()
        }
    }

    private fun updateDayDetailChart(records: List<GlucoseRecord>) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDetailDate.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        
        val dailyRecords = records.filter { it.timestamp in startOfDay until (startOfDay + 86400000L) }.sortedBy { it.timestamp }
        
        binding.dayDetailChart.apply {
            xAxis.resetAxisMinimum(); xAxis.resetAxisMaximum()
            axisLeft.resetAxisMinimum(); axisLeft.resetAxisMaximum()
            
            if (dailyRecords.isEmpty()) { clear(); return }
            
            val entries = dailyRecords.map { Entry((it.timestamp - startOfDay) / 3600000f, it.value.toFloat()) }
            val colors = dailyRecords.map {
                val status = com.example.glucoseguard.util.GlucosePolicy.classify(it.value, it.category, com.example.glucoseguard.util.TargetPreferences.read(requireContext()))
                when(status) { com.example.glucoseguard.util.GlucosePolicy.Status.LOW -> Color.parseColor("#185FA0"); com.example.glucoseguard.util.GlucosePolicy.Status.BELOW_TARGET -> Color.parseColor("#9A640C"); com.example.glucoseguard.util.GlucosePolicy.Status.ABOVE_TARGET -> Color.parseColor("#B53D3B"); else -> Color.parseColor("#087F78") }
            }
            val dataSet = LineDataSet(entries, "혈당 흐름").apply { 
                lineWidth = 3f; circleRadius = 5f; mode = LineDataSet.Mode.LINEAR; color = Color.parseColor("#087F78")
                setCircleColors(colors); setDrawFilled(true); fillAlpha = 30; fillColor = Color.parseColor("#E3F3EF")
                setDrawValues(false); highLightColor = Color.parseColor("#087F78")
            }
            
            data = LineData(dataSet)
            marker = CustomMarkerView(requireContext(), R.layout.layout_chart_marker, emptyList(), "mg/dL", 2, startOfDay)
            xAxis.apply { 
                valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float) = String.format(Locale.getDefault(), "%02d:00", value.toInt()) }
                axisMinimum = 0f; axisMaximum = 24f 
            }
            applyLimitLines(this)
            
            axisLeft.apply {
                val maxVal = entries.maxOf { it.y }
                val minVal = entries.minOf { it.y }
                axisMaximum = Math.max(250f, maxVal + 30f)
                axisMinimum = Math.min(40f, minVal - 20f)
            }
            
            fitScreen(); notifyDataSetChanged(); invalidate()
        }
    }

    private fun applyLimitLines(chart: LineChart) {
        chart.axisLeft.apply {
            removeAllLimitLines()
            addLimitLine(LimitLine(TARGET_MAX, "참고 상한 (${TARGET_MAX.toInt()})").apply { lineWidth = 1.5f; lineColor = Color.parseColor("#E53935"); enableDashedLine(10f, 10f, 0f); labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP; textSize = 9f; textColor = Color.parseColor("#E53935") })
            addLimitLine(LimitLine(TARGET_MIN, "목표 하한 (${TARGET_MIN.toInt()})").apply { lineWidth = 1.5f; lineColor = Color.parseColor("#1E88E5"); enableDashedLine(10f, 10f, 0f); labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM; textSize = 9f; textColor = Color.parseColor("#1E88E5") })
            setDrawLimitLinesBehindData(true)
        }
    }

    private fun getGlucoseColor(value: Int, min: Float, max: Float): Int {
        return when { value < min -> Color.parseColor("#1E88E5"); value > max -> Color.parseColor("#E53935"); else -> Color.parseColor("#087F78") }
    }

    private fun updateInsulinChart(records: List<InsulinRecord>) {
        if (records.isEmpty()) { binding.insulinCombinedChart.clear(); return }
        val grouped = records.groupBy {
            val c = Calendar.getInstance(); c.timeInMillis = it.timestamp
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }
        val sortedTimes = grouped.keys.sorted()
        val barEntries = mutableListOf<BarEntry>()
        val lineEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        sortedTimes.forEachIndexed { index, time ->
            val total = grouped[time]!!.sumOf { it.dosage.toDouble() }.toFloat()
            barEntries.add(BarEntry(index.toFloat(), total)); lineEntries.add(Entry(index.toFloat(), total))
            labels.add(SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(time)))
        }
        binding.insulinCombinedChart.apply {
            data = CombinedData().apply {
                setData(BarData(BarDataSet(barEntries, "투여량").apply { color = Color.parseColor("#66BB6A"); setDrawValues(true) }).apply { barWidth = 0.4f })
                setData(LineData(LineDataSet(lineEntries, "추세").apply { color = Color.parseColor("#087F78"); lineWidth = 2f; setDrawValues(false) }))
            }
            marker = CustomMarkerView(requireContext(), R.layout.layout_chart_marker, labels, "U", 1, 0L)
            xAxis.apply { valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float) = if (value.toInt() in labels.indices) labels[value.toInt()] else "" }; axisMinimum = -0.5f; axisMaximum = (labels.size - 1) + 0.5f }
            fitScreen(); if (labels.size > 7) { setVisibleXRangeMaximum(7f); moveViewToX(labels.size - 1f) }
            notifyDataSetChanged(); invalidate()
        }
    }

    override fun onResume() { super.onResume(); refreshCharts() }
    override fun onDestroyView() { (activity as? MainActivity)?.setMainUIVisible(true); requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; super.onDestroyView(); _binding = null }
}
