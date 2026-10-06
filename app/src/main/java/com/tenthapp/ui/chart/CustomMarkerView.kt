package com.example.glucoseguard.ui.chart

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.glucoseguard.R
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    context: Context, 
    layoutResource: Int, 
    private var labels: List<String> = emptyList(),
    private val unit: String,
    private val mode: Int = 0, // 0: Daily(Time), 1: Weekly/Monthly(Index), 2: Hourly(Detail)
    private val startTime: Long = 0L
) : MarkerView(context, layoutResource) {

    private val tvTime: TextView = findViewById(R.id.tv_marker_time)
    private val tvValue: TextView = findViewById(R.id.tv_marker_value)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return
        
        when (mode) {
            0 -> { // 일별 (시간 기반)
                val timestamp = (e.x * 86400000L).toLong() + startTime
                tvTime.text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
            }
            1 -> { // 주별, 월별 (인덱스 기반)
                val index = e.x.toInt()
                tvTime.text = if (index >= 0 && index < labels.size) labels[index] else ""
            }
            2 -> { // 하루 상세 (시간 기반)
                val timestamp = (e.x * 3600000L).toLong() + startTime
                tvTime.text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
        }

        val valueText = if (e.y % 1 == 0f) "${e.y.toInt()} $unit" else String.format(Locale.getDefault(), "%.1f %s", e.y, unit)
        tvValue.text = valueText

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
