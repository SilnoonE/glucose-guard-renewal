package com.example.glucoseguard.report

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {

    private val primaryColor = Color.parseColor("#087F78")
    private val secondaryColor = Color.parseColor("#487F99")
    private val textMainColor = Color.parseColor("#1A1C19")
    private val textSubColor = Color.parseColor("#424940")
    private val borderColor = Color.parseColor("#DDE5DB")

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    // PDF 상태 관리
    private var pdfDocument: PdfDocument? = null
    private var currentCanvas: Canvas? = null
    private var currentPage: PdfDocument.Page? = null
    private var currentY = 0f
    private var pageNum = 0
    private lateinit var data: ReportData

    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 45f
    private val headerHeight = 80f
    private val footerHeight = 50f
    private val contentWidth = pageWidth - 2 * margin
    private val bottomLimit = pageHeight - footerHeight - 20f

    fun generateReport(reportData: ReportData, options: ReportOptions): File? {
        this.data = reportData
        pdfDocument = PdfDocument()
        pageNum = 0

        // 첫 페이지 시작
        startNewPage()

        // 1. 요약 섹션
        drawSummarySection(options)

        // 2. 차트 섹션
        if (options.includeDailyChart || options.includeWeeklyChart || options.includeMonthlyChart) {
            drawChartsSection(options)
        }

        // 3. 일일 상세 차트
        if (options.includeDayDetailChart) {
            drawDetailChartSection()
        }

        // 4. 메모 섹션
        if (options.includeMemo) {
            drawMemoSummarySection()
        }

        // 5. AI 분석 섹션 (메모 섹션 바로 뒤에 붙여서 출력)
        if (options.includeAiAnalysis) {
            drawAiAnalysisSection()
        }

        // 6. 전체 기록 표 (무조건 새 페이지에서 시작)
        startNewPage()
        drawAllRecordsTable()

        // 마지막 페이지 종료
        finishCurrentPage()

        val fileName = "glucose_report_${SimpleDateFormat("yyyy_MM_dd_HHmmss_SSS", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            FileOutputStream(file).use { pdfDocument?.writeTo(it) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    private fun startNewPage() {
        finishCurrentPage()
        pageNum++
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNum).create()
        currentPage = pdfDocument?.startPage(pageInfo)
        currentCanvas = currentPage?.canvas
        currentCanvas?.drawColor(Color.WHITE)
        
        drawHeader()
        currentY = headerHeight + 45f // 헤더 바 밑에 공백 추가 (약 1.5cm~2cm)
    }

    private fun finishCurrentPage() {
        currentPage?.let { page ->
            currentCanvas?.let { canvas ->
                drawFooter(canvas, pageNum)
            }
            pdfDocument?.finishPage(page)
        }
        currentPage = null
        currentCanvas = null
    }

    private fun ensureSpaceOrNewPage(heightNeeded: Float) {
        if (currentY + heightNeeded > bottomLimit) {
            startNewPage()
        }
    }

    private fun drawHeader() {
        val canvas = currentCanvas ?: return
        val paint = Paint()
        
        // Header background
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth, headerHeight, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(com.example.glucoseguard.R.string.pdf_header_title), margin, 50f, paint)

        // Subtitle (Period)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        val periodStr = "${context.getString(com.example.glucoseguard.R.string.pdf_period)}: ${dateFormat.format(Date(data.startDate))} ~ ${dateFormat.format(Date(data.endDate))}"
        canvas.drawText(periodStr, margin, 70f, paint)
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val paint = Paint()
        paint.color = textSubColor
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        
        val footerText = context.getString(com.example.glucoseguard.R.string.pdf_footer_disclaimer)
        canvas.drawText(footerText, pageWidth / 2, pageHeight - 35f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("- $pageNumber -", pageWidth / 2, pageHeight - 20f, paint)
    }

    private fun drawSectionTitle(title: String) {
        ensureSpaceOrNewPage(40f)
        val canvas = currentCanvas ?: return
        val paint = Paint().apply {
            color = primaryColor
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, margin, currentY, paint)
        
        paint.strokeWidth = 2f
        canvas.drawLine(margin, currentY + 5f, margin + 80f, currentY + 5f, paint)
        currentY += 30f
    }

    private fun drawSummarySection(options: ReportOptions) {
        ensureSpaceOrNewPage(100f)
        val canvas = currentCanvas ?: return
        val paint = Paint()

        // Info info
        paint.textSize = 11f
        paint.color = textSubColor
        canvas.drawText("${context.getString(com.example.glucoseguard.R.string.pdf_gen_date)}: ${dateTimeFormat.format(Date(data.generationDate))}", margin, currentY, paint)
        currentY += 25f

        if (options.includeGlucoseSummary) {
            drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_glucose_summary))
            val stats = listOf(
                context.getString(com.example.glucoseguard.R.string.avg_glucose) to "${data.stats.avgGlucose} mg/dL",
                context.getString(com.example.glucoseguard.R.string.pdf_max_glucose) to "${data.stats.maxGlucose} mg/dL",
                context.getString(com.example.glucoseguard.R.string.pdf_min_glucose) to "${data.stats.minGlucose} mg/dL",
                context.getString(com.example.glucoseguard.R.string.pdf_normal_range) to "${data.stats.normalRangePercentage}%"
            )
            drawStatGrid(stats)
            currentY += 20f
        }

        if (options.includeInsulinSummary) {
            drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_insulin_summary))
            val stats = listOf(
                context.getString(com.example.glucoseguard.R.string.pdf_total_dosage) to "${String.format(Locale.getDefault(), "%.1f", data.stats.totalInsulin)} U",
                context.getString(com.example.glucoseguard.R.string.pdf_avg_dosage) to "${String.format(Locale.getDefault(), "%.1f", data.stats.avgInsulin)} U",
                context.getString(com.example.glucoseguard.R.string.pdf_dosage_count) to "${data.stats.insulinCount} ${context.getString(com.example.glucoseguard.R.string.unit_dosage)}"
            )
            drawStatGrid(stats)
            currentY += 40f // 섹션 간 간격 확대
        }

        drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_overall_status))
        ensureSpaceOrNewPage(40f)
        val textPaint = TextPaint().apply {
            textSize = 13f
            color = textMainColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawWrappedText(data.stats.statusSummary, textPaint)
        currentY += 60f // 다음 섹션(차트 등)과의 간격 확대 (약 2.5cm)
    }

    private fun drawStatGrid(stats: List<Pair<String, String>>) {
        val gridHeight = if (stats.size > 4) 130f else 65f
        ensureSpaceOrNewPage(gridHeight)
        val canvas = currentCanvas ?: return
        val paint = Paint()
        
        var x = margin
        val cardWidth = (contentWidth - 30f) / 4
        val cardHeight = 60f

        stats.forEachIndexed { index, pair ->
            if (index > 0 && index % 4 == 0) {
                x = margin
                currentY += cardHeight + 10f
            }

            // Card
            paint.color = Color.parseColor("#F1F8E9")
            canvas.drawRoundRect(x, currentY, x + cardWidth, currentY + cardHeight, 8f, 8f, paint)
            
            paint.color = textSubColor
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(pair.first, x + 8f, currentY + 20f, paint)
            
            paint.color = textMainColor
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(pair.second, x + 8f, currentY + 45f, paint)
            
            x += cardWidth + 10f
        }
        currentY += cardHeight + 10f
    }

    private fun drawChartsSection(options: ReportOptions) {
        val chartHeight = 200f // 최소 180dp 이상 확보
        val gap = 60f

        if (options.includeDailyChart) {
            drawSectionTitleWithChart(context.getString(com.example.glucoseguard.R.string.pdf_daily_trend), data.dailyGlucoseEntries, chartHeight, "daily")
            currentY += gap
        }
        
        if (options.includeWeeklyChart) {
            drawSectionTitleWithChart(context.getString(com.example.glucoseguard.R.string.pdf_weekly_trend), data.weeklyGlucoseEntries, chartHeight, "weekly")
            currentY += gap
        }

        if (options.includeMonthlyChart) {
            drawSectionTitleWithChart(context.getString(com.example.glucoseguard.R.string.pdf_monthly_trend), data.monthlyGlucoseEntries, chartHeight, "monthly")
            currentY += gap
        }
    }

    private fun drawDetailChartSection() {
        val chartHeight = 220f
        val latestDate = data.glucoseRecords.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
        val dateStr = dateFormat.format(Date(latestDate))
        
        ensureSpaceOrNewPage(chartHeight + 80f)
        
        drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_detail_analysis))
        
        val paint = Paint().apply { textSize = 11f; color = textSubColor }
        currentCanvas?.drawText("$dateStr 기준 상세 혈당 흐름", margin, currentY, paint)
        currentY += 20f
        
        drawChartSafely(data.hourlyGlucoseEntries, chartHeight, "hourly")
        currentY += 50f
    }

    private fun drawSectionTitleWithChart(title: String, entries: List<ChartEntry>, height: Float, type: String) {
        ensureSpaceOrNewPage(height + 70f)
        drawSectionTitle(title)
        currentY += 16f // 제목과 차트 사이 여백 16dp
        drawChartSafely(entries, height, type)
    }

    private fun drawChartSafely(entries: List<ChartEntry>, height: Float, type: String) {
        ensureSpaceOrNewPage(height)
        val canvas = currentCanvas ?: return
        
        if (entries.size < 2 && type != "hourly") {
            drawPlaceholder("데이터가 충분하지 않습니다 (2개 이상의 기록 필요)")
            currentY += height
            return
        }

        val bitmap = drawPdfSafeLineChart(entries, contentWidth, height, type)
        canvas.drawBitmap(bitmap, margin, currentY, null)
        bitmap.recycle()
        currentY += height
    }

    private fun drawAiAnalysisSection() {
        drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_ai_analysis))
        val paint = TextPaint().apply {
            textSize = 12f
            color = textMainColor
        }
        
        data.analysisItems.forEach { item ->
            val startX = margin + 15f
            val availableWidth = contentWidth - 15f
            
            val staticLayout = StaticLayout.Builder.obtain(item, 0, item.length, paint, availableWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            
            val itemHeight = staticLayout.height.toFloat()
            ensureSpaceOrNewPage(itemHeight + 10f)
            
            // Bullet point (aligned with first line)
            currentCanvas?.drawCircle(margin + 5f, currentY + 7f, 2f, Paint().apply { color = primaryColor })
            
            currentCanvas?.save()
            currentCanvas?.translate(startX, currentY)
            staticLayout.draw(currentCanvas)
            currentCanvas?.restore()
            
            currentY += itemHeight + 8f
        }
        currentY += 15f
    }

    private fun drawMemoSummarySection() {
        drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_memo_summary))
        val paint = TextPaint().apply { textSize = 10f; color = textMainColor }
        val timeFmt = SimpleDateFormat("MM.dd HH:mm", Locale.getDefault())
        
        val combinedMemos = (data.glucoseRecords.filter { it.memo.isNotEmpty() }.map { it.timestamp to "${context.getString(com.example.glucoseguard.R.string.glucose)}: ${it.memo}" } +
                           data.insulinRecords.filter { it.memo.isNotEmpty() }.map { it.timestamp to "${context.getString(com.example.glucoseguard.R.string.insulin)}: ${it.memo}" })
                           .plus(data.mealRecords.map { it.timestamp to "건강 메모: ${it.memo}" })
                           .sortedByDescending { it.first }
                           .take(20)

        if (combinedMemos.isEmpty()) {
            drawWrappedText(context.getString(com.example.glucoseguard.R.string.pdf_no_records), paint)
            currentY += 40f
            return
        }

        combinedMemos.forEach { (time, memo) ->
            val text = "[${timeFmt.format(Date(time))}] $memo"
            ensureSpaceOrNewPage(20f)
            drawWrappedText(text, paint)
            currentY += 5f
        }
        currentY += 40f // AI 분석 섹션과의 간격 확대
    }

    private fun drawAllRecordsTable() {
        drawSectionTitle(context.getString(com.example.glucoseguard.R.string.pdf_all_records))
        
        // 데이터 통합 및 정렬
        val allRecords = mutableListOf<UnifiedRow>()
        data.glucoseRecords.forEach { 
            allRecords.add(UnifiedRow(it.timestamp, it.value.toString(), com.example.glucoseguard.util.CategoryMapper.getTranslatedCategory(context, it.category), "-", it.memo))
        }
        data.insulinRecords.forEach {
            allRecords.add(UnifiedRow(it.timestamp, "-", "-", "${it.dosage}U (${it.type})", it.memo))
        }
        data.mealRecords.forEach { allRecords.add(UnifiedRow(it.timestamp, "-", "건강 메모", "-", it.memo)) }
        allRecords.sortBy { it.timestamp }

        if (allRecords.isEmpty()) {
            drawWrappedText(context.getString(com.example.glucoseguard.R.string.pdf_no_records), TextPaint().apply { textSize = 11f })
            return
        }

        // Table Header
        val colWidths = floatArrayOf(60f, 50f, 80f, 100f, contentWidth - 290f)
        val headers = arrayOf(
            context.getString(com.example.glucoseguard.R.string.pdf_col_time),
            context.getString(com.example.glucoseguard.R.string.pdf_col_glucose),
            context.getString(com.example.glucoseguard.R.string.pdf_col_status),
            context.getString(com.example.glucoseguard.R.string.pdf_col_insulin),
            context.getString(com.example.glucoseguard.R.string.pdf_col_memo)
        )
        
        drawTableRow(headers, colWidths, isHeader = true)
        
        var lastDate = ""
        val tableDateFormat = SimpleDateFormat("yyyy.MM.dd (E)", Locale.getDefault())
        val tableTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        allRecords.forEach { row ->
            val currentDate = tableDateFormat.format(Date(row.timestamp))
            if (currentDate != lastDate) {
                drawDateRow(currentDate)
                lastDate = currentDate
            }
            
            val timeStr = tableTimeFormat.format(Date(row.timestamp))
            val values = arrayOf(timeStr, row.glucose, row.category, row.insulin, row.memo)
            drawTableRow(values, colWidths, isHeader = false)
        }
    }

    private fun drawDateRow(date: String) {
        val rowHeight = 22f
        ensureSpaceOrNewPage(rowHeight)
        val canvas = currentCanvas ?: return
        
        val paint = Paint().apply {
            color = Color.parseColor("#F1F3F1")
            style = Paint.Style.FILL
        }
        canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, paint)
        
        paint.apply {
            color = textSubColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🗓️ $date", margin + 5f, currentY + 15f, paint)
        
        // Bottom border for date row
        paint.apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawLine(margin, currentY + rowHeight, margin + contentWidth, currentY + rowHeight, paint)
        
        currentY += rowHeight
    }

    private fun drawTableRow(values: Array<String>, widths: FloatArray, isHeader: Boolean, splitLongMemo: Boolean = true) {
        val paint = TextPaint().apply {
            textSize = if (isHeader) 10f else 9f
            typeface = if (isHeader) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            color = if (isHeader) Color.WHITE else textMainColor
        }

        val fullMemo = StaticLayout.Builder.obtain(values.last(),0,values.last().length,paint,(widths.last()-10f).toInt()).build()
        val maxLines = ((bottomLimit-headerHeight-50f)/(paint.fontSpacing+2f)).toInt().coerceAtLeast(1)
        if(splitLongMemo && fullMemo.lineCount>maxLines) {
            var offset=0
            var line=0
            while(line<fullMemo.lineCount) {
                val lastLine=minOf(line+maxLines,fullMemo.lineCount)-1
                val end=fullMemo.getLineEnd(lastLine)
                val chunk=values.copyOf()
                chunk[chunk.lastIndex]=values.last().substring(offset,end).trimEnd('\n','\r')
                if(line>0) for(i in 0 until chunk.lastIndex) chunk[i]=""
                drawTableRow(chunk,widths,isHeader,false)
                offset=end;line=lastLine+1
            }
            return
        }
        // Calculate row height based on longest text (memo)
        val memoWidth = (widths.last()-10f).toInt()
        val staticLayout = StaticLayout.Builder.obtain(values.last(), 0, values.last().length, paint, memoWidth).build()
        
        val rowHeight = Math.max(25f, staticLayout.height.toFloat() + 10f)
        ensureSpaceOrNewPage(rowHeight)
        
        val canvas = currentCanvas ?: return
        val bgPaint = Paint()
        
        // Row background
        if (isHeader) {
            bgPaint.color = primaryColor
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, bgPaint)
        } else {
            bgPaint.color = Color.WHITE
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, bgPaint)
            bgPaint.color = borderColor
            bgPaint.style = Paint.Style.STROKE
            bgPaint.strokeWidth = 0.5f
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, bgPaint)
        }

        var curX = margin
        values.forEachIndexed { i, text ->
            val cellWidth = widths[i]
            if (i == values.size - 1) { // Memo cell with wrapping
                canvas.save()
                canvas.translate(curX + 5f, currentY + 5f)
                staticLayout.draw(canvas)
                canvas.restore()
            } else {
                canvas.drawText(text, curX + 5f, currentY + 17f, paint)
            }
            
            // vertical line
            bgPaint.color = borderColor
            bgPaint.style = Paint.Style.STROKE
            canvas.drawLine(curX + cellWidth, currentY, curX + cellWidth, currentY + rowHeight, bgPaint)
            
            curX += cellWidth
        }
        
        currentY += rowHeight
    }

    private fun drawWrappedText(text: String, paint: TextPaint) {
        val layout=StaticLayout.Builder.obtain(text,0,text.length,paint,contentWidth.toInt()).build()
        for(line in 0 until layout.lineCount) {
            val top=layout.getLineTop(line)
            val height=(layout.getLineBottom(line)-top).toFloat()
            ensureSpaceOrNewPage(height)
            currentCanvas?.let { canvas ->
                canvas.save()
                canvas.clipRect(margin,currentY,margin+contentWidth,currentY+height)
                canvas.translate(margin,currentY-top)
                layout.draw(canvas)
                canvas.restore()
            }
            currentY+=height
        }
    }

    private fun drawPdfSafeLineChart(entries: List<ChartEntry>, width: Float, height: Float, type: String): Bitmap {
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // 1. Padding 설정 (DP 단위 기준)
        val leftPadding = 55f
        val rightPadding = 24f
        val topPadding = 35f
        val bottomPadding = 45f

        val plotRect = RectF(leftPadding, topPadding, width - rightPadding, height - bottomPadding)

        if (entries.isEmpty()) return bitmap

        // 2. 데이터 분석
        val rawMinY = entries.minOf { it.y }
        val rawMaxY = entries.maxOf { it.y }
        
        // Y축 범위 보정
        val minY = if (rawMinY > 60) 60f else Math.floor(rawMinY / 10.0).toFloat() * 10f
        val maxY = if (rawMaxY < 180) 200f else Math.ceil(rawMaxY / 10.0).toFloat() * 10f + 20f
        val rangeY = maxY - minY

        val minX = entries.minOf { it.x }
        val maxX = entries.maxOf { it.x }
        val rangeX = if (maxX == minX) 1f else maxX - minX

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = textSubColor
        }

        // 3. Y축 라벨 및 그리드 (최대 5개)
        textPaint.textAlign = Paint.Align.RIGHT
        val ySteps = 5
        for (i in 0 until ySteps) {
            val valY = minY + (rangeY / (ySteps - 1)) * i
            val py = plotRect.bottom - (valY - minY) / rangeY * plotRect.height()
            
            // 그리드 라인
            paint.color = Color.parseColor("#F0F0F0")
            paint.strokeWidth = 1f
            canvas.drawLine(plotRect.left, py, plotRect.right, py, paint)
            
            // Y축 숫자 (leftPadding 영역)
            canvas.drawText(valY.toInt().toString(), plotRect.left - 8f, py + 3f, textPaint)
        }

        // 4. 그래프 선 그리기 (Plot 영역으로 Clip)
        canvas.save()
        canvas.clipRect(plotRect)
        
        val linePath = Path()
        entries.forEachIndexed { i, entry ->
            val px = plotRect.left + (entry.x - minX) / rangeX * plotRect.width()
            val py = plotRect.bottom - (entry.y - minY) / rangeY * plotRect.height()
            if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
        }
        
        paint.style = Paint.Style.STROKE
        paint.color = primaryColor
        paint.strokeWidth = 2.5f
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(linePath, paint)
        
        // 채우기 효과 (Gradient)
        val fillPath = Path(linePath)
        fillPath.lineTo(plotRect.left + (entries.last().x - minX) / rangeX * plotRect.width(), plotRect.bottom)
        fillPath.lineTo(plotRect.left, plotRect.bottom)
        fillPath.close()
        
        val gradient = LinearGradient(0f, plotRect.top, 0f, plotRect.bottom, 
            Color.argb(80, 46, 125, 50), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL
        paint.shader = gradient
        canvas.drawPath(fillPath, paint)
        paint.shader = null

        canvas.restore()

        // 5. 점 및 데이터 값 라벨 (Plot 영역 안)
        val usedLabelRects = mutableListOf<Rect>()
        
        // 라벨 표시 규칙
        val showAllLabels = entries.size <= 10
        val importantIndices = mutableSetOf(0, entries.size - 1)
        if (!showAllLabels) {
            importantIndices.add(entries.indexOfFirst { it.y == rawMaxY })
            importantIndices.add(entries.indexOfFirst { it.y == rawMinY })
        }

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD
        
        entries.forEachIndexed { i, entry ->
            val px = plotRect.left + (entry.x - minX) / rangeX * plotRect.width()
            val py = plotRect.bottom - (entry.y - minY) / rangeY * plotRect.height()
            
            // 점 그리기
            paint.style = Paint.Style.FILL
            paint.color = primaryColor
            canvas.drawCircle(px, py, 3.5f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(px, py, 1.8f, paint)

            // 라벨 그리기
            if (showAllLabels || i in importantIndices) {
                val labelText = entry.y.toInt().toString()
                val bounds = Rect()
                textPaint.getTextBounds(labelText, 0, labelText.length, bounds)
                
                var labelY = py - 14f
                // 상단 영역 벗어남 체크
                if (labelY - bounds.height() < topPadding) {
                    labelY = py + 22f // 점 아래에 표시
                }
                
                val currentRect = Rect(
                    (px - bounds.width() / 2 - 5).toInt(),
                    (labelY - bounds.height() - 5).toInt(),
                    (px + bounds.width() / 2 + 5).toInt(),
                    (labelY + 5).toInt()
                )
                
                // 겹침 검사
                val isOverlapped = usedLabelRects.any { Rect.intersects(it, currentRect) }
                if (!isOverlapped) {
                    canvas.drawText(labelText, px, labelY, textPaint)
                    usedLabelRects.add(currentRect)
                }
            }
        }

        // 6. X축 날짜 라벨 (bottomPadding 영역)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textAlign = Paint.Align.CENTER
        
        val xInterval = when {
            entries.size <= 7 -> 1
            entries.size <= 20 -> 2
            entries.size <= 40 -> 4
            else -> 7
        }

        entries.forEachIndexed { i, entry ->
            val isFirst = i == 0
            val isLast = i == entries.size - 1
            val isInterval = i % xInterval == 0
            
            if (isFirst || isLast || isInterval) {
                val px = plotRect.left + (entry.x - minX) / rangeX * plotRect.width()
                val formattedLabel = formatXLabel(entry.label, type)
                
                // X축 라벨 겹침 방지 (간단히 x좌표 거리로 체크)
                val labelY = plotRect.bottom + 22f
                canvas.drawText(formattedLabel, px, labelY, textPaint)
            }
        }

        return bitmap
    }

    private fun formatXLabel(label: String, type: String): String {
        return try {
            when (type) {
                "daily" -> {
                    // input: 2024.03.24 or 03.24
                    if (label.contains(".")) {
                        val parts = label.split(".")
                        if (parts.size >= 2) "${parts[parts.size - 2]}/${parts.last()}" else label
                    } else label
                }
                "weekly" -> {
                    // input: 2024년 3월 4주
                    label.replace("2024년 ", "").replace("2025년 ", "")
                }
                "monthly" -> {
                    // input: 2024.03 or 24.03
                    if (label.contains(".")) {
                        val parts = label.split(".")
                        if (parts.size >= 2) "${parts[parts.size - 2].takeLast(2)}.${parts.last()}" else label
                    } else label
                }
                "hourly" -> label
                else -> label
            }
        } catch (e: Exception) {
            label
        }
    }

    private fun drawPlaceholder(text: String) {
        val canvas = currentCanvas ?: return
        val paint = Paint()
        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        canvas.drawRect(margin, currentY, margin + contentWidth, currentY + 100f, paint)
        
        paint.style = Paint.Style.FILL
        paint.pathEffect = null
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, pageWidth / 2, currentY + 55f, paint)
    }

    data class UnifiedRow(
        val timestamp: Long,
        val glucose: String,
        val category: String,
        val insulin: String,
        val memo: String
    )
}
