package com.example.glucoseguard.ui.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.example.glucoseguard.DiabetesApplication
import com.example.glucoseguard.NotificationReceiver
import com.example.glucoseguard.R
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.databinding.FragmentSettingsBinding
import com.example.glucoseguard.report.PdfReportGenerator
import com.example.glucoseguard.report.ReportDataBuilder
import com.example.glucoseguard.report.ReportOptions
import com.example.glucoseguard.report.ReportOptionsBottomSheet
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModel
import com.example.glucoseguard.ui.viewmodel.DiabetesViewModelFactory
import com.example.glucoseguard.util.LocaleHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiabetesViewModel by viewModels {
        DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository)
    }

    private var cachedGlucoseRecords: List<GlucoseRecord> = emptyList()
    private var cachedInsulinRecords: List<InsulinRecord> = emptyList()
    private var pendingReportData: Pair<Int, ReportOptions>? = null
    private var mInterstitialAd: InterstitialAd? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingReportData?.let { (period, options) ->
                showInterstitialAdAndContinue {
                    generateAndSaveReport(period, options)
                }
            }
        } else {
            Toast.makeText(requireContext(), "파일 저장 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
        pendingReportData = null
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveImageToInternalStorage(it)
        }
    }

    private val pickCsvLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importCsvData(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadInterstitialAd()
        setupNotificationSwitches()
        setupCollapsibleNotifications()

        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) { records ->
            cachedGlucoseRecords = records
        }

        viewModel.allInsulinRecords.observe(viewLifecycleOwner) { records ->
            cachedInsulinRecords = records
        }

        binding.btnCreateReport.setOnClickListener {
            if (cachedGlucoseRecords.isEmpty()) {
                Toast.makeText(requireContext(), "기록이 없어 리포트를 생성할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showReportOptions()
        }

        binding.btnChangeProfileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnExportData.setOnClickListener {
            if (cachedGlucoseRecords.isEmpty()) {
                Toast.makeText(requireContext(), "내보낼 기록이 없습니다. 먼저 혈당을 기록해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                showExportOptionsDialog()
            }
        }

        binding.btnImportData.setOnClickListener {
            pickCsvLauncher.launch("*/*")
        }

        binding.btnClearData.setOnClickListener {
            showClearDataDialog()
        }

        binding.btnGuideManage.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_guideManageFragment)
        }

        binding.btnGuideTarget.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_guideTargetFragment)
        }

        binding.btnGuideHigh.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_guideHighFragment)
        }

        binding.btnGuideLow.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_guideLowFragment)
        }

        binding.btnGuideFood.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_guideFoodFragment)
        }

        binding.btnHealthCalculatorTop.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_settings_to_healthCalculatorFragment)
        }

        binding.btnChangeLanguage.setOnClickListener {
            showLanguageDialog()
        }

        setupGuideCollapsible()
    }

    private fun setupGuideCollapsible() {
        binding.btnToggleGuideDetails.setOnClickListener {
            val isVisible = binding.layoutGuideItems.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutGuideItems.visibility = View.GONE
                binding.dividerGuide.visibility = View.GONE
                binding.ivGuideExpandArrow.rotation = 0f
            } else {
                binding.layoutGuideItems.visibility = View.VISIBLE
                binding.dividerGuide.visibility = View.VISIBLE
                binding.ivGuideExpandArrow.rotation = 180f
            }
        }
    }

    private fun showReportOptions() {
        val bottomSheet = ReportOptionsBottomSheet { periodDays, options ->
            checkPermissionAndGenerateReport(periodDays, options)
        }
        bottomSheet.show(childFragmentManager, ReportOptionsBottomSheet.TAG)
    }

    private fun checkPermissionAndGenerateReport(periodDays: Int, options: ReportOptions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingReportData = periodDays to options
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        showInterstitialAdAndContinue {
            generateAndSaveReport(periodDays, options)
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    private fun showInterstitialAdAndContinue(onDismiss: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    mInterstitialAd = null
                    onDismiss()
                }
            }
            mInterstitialAd?.show(requireActivity())
        } else {
            onDismiss()
        }
    }

    private fun generateAndSaveReport(periodDays: Int, options: ReportOptions) {
        val builder = ReportDataBuilder()
        val reportData = builder.buildReportData(periodDays, cachedGlucoseRecords, cachedInsulinRecords)
        
        val generator = PdfReportGenerator(requireContext())
        val tempFile = generator.generateReport(reportData, options)
        
        if (tempFile != null) {
            saveToPublicStorage(tempFile, "application/pdf")
        } else {
            Toast.makeText(requireContext(), "PDF 생성 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToPublicStorage(tempFile: File, mimeType: String) {
        val fileName = tempFile.name
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = requireContext().contentResolver
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val publicFile = File(downloadsDir, fileName)
                uri = Uri.fromFile(publicFile)
                outputStream = FileOutputStream(publicFile)
            }

            outputStream?.use { output ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(output)
                }
            }

            if (mimeType == "application/pdf") {
                showCompletionDialog(tempFile, uri)
            } else {
                showExportCompletionDialog(tempFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "파일 저장 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCompletionDialog(tempFile: File, publicUri: Uri?) {
        AlertDialog.Builder(requireContext())
            .setTitle("PDF 리포트 생성 완료")
            .setMessage("리포트가 다운로드 폴더에 저장되었습니다.")
            .setPositiveButton("공유하기") { _, _ ->
                sharePdfFile(tempFile)
            }
            .setNeutralButton("열기") { _, _ ->
                publicUri?.let { openPdfFile(it) }
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun showExportCompletionDialog(tempFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("데이터 내보내기 완료")
            .setMessage("데이터가 다운로드 폴더에 저장되었습니다.\n'불러오기' 시 해당 파일을 선택해주세요.")
            .setPositiveButton("공유하기") { _, _ ->
                shareFile(tempFile)
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun openPdfFile(uri: Uri) {
        try {
            val viewUri = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "PDF를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "건강 리포트 공유"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "공유 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            // 내부 저장소의 files 디렉토리에 profile_image.jpg로 저장
            val file = File(requireContext().filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // 저장된 파일의 절대 경로를 SharedPreferences에 저장
            val sharedPref = requireActivity().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("profile_image_path", file.absolutePath).apply()
            
            Toast.makeText(requireContext(), "프로필 이미지가 저장되었습니다", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "이미지 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("일별 리포트", "주별 리포트", "월별 리포트")
        val adDisclaimer = getString(R.string.report_ad_disclaimer)
        
        AlertDialog.Builder(requireContext())
            .setTitle("내보낼 기간 선택\n$adDisclaimer")
            .setItems(options) { _, which ->
                showInterstitialAdAndContinue {
                    exportData(which)
                }
            }
            .show()
    }

    private fun exportData(type: Int) {
        val glucoseRecords = cachedGlucoseRecords
        val insulinRecords = cachedInsulinRecords
        
        if (glucoseRecords.isEmpty() && insulinRecords.isEmpty()) {
            Toast.makeText(requireContext(), "내보낼 기록이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val reportTitle = when (type) {
            0 -> "일별 혈당/인슐린 통합 리포트"
            1 -> "주별 혈당/인슐린 통합 리포트"
            else -> "월별 혈당/인슐린 통합 리포트"
        }

        val sb = StringBuilder()
        sb.append("📋 $reportTitle\n")
        sb.append("발행일: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
        sb.append("--------------------------------\n\n")

        // 1. 기간별 요약 (혈당 위주)
        sb.append("📈 [기간별 평균 혈당 요약]\n")
        val grouped = when (type) {
            0 -> glucoseRecords.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }
            1 -> glucoseRecords.groupBy { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                "${cal.get(Calendar.YEAR)}년 ${cal.get(Calendar.MONTH) + 1}월 ${cal.get(Calendar.WEEK_OF_MONTH)}주"
            }
            else -> glucoseRecords.groupBy { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) }
        }

        if (grouped.isEmpty()) {
            sb.append("- 혈당 기록 없음\n")
        } else {
            grouped.keys.sortedDescending().forEach { key ->
                val periodRecords = grouped[key]!!
                val avg = periodRecords.map { it.value }.average().toInt()
                sb.append("• $key: 평균 ${avg}mg/dL (기록 ${periodRecords.size}건)\n")
            }
        }
        sb.append("\n")

        // 2. 전체 통계
        sb.append("📊 [전체 통계 요약]\n")
        if (glucoseRecords.isNotEmpty()) {
            val allValues = glucoseRecords.map { it.value }
            sb.append("- 평균 혈당: ${allValues.average().toInt()} mg/dL\n")
            sb.append("- 최고 혈당: ${allValues.maxOrNull()} mg/dL\n")
            sb.append("- 최저 혈당: ${allValues.minOrNull()} mg/dL\n")
            sb.append("- 혈당 기록: ${glucoseRecords.size} 건\n")
        }
        if (insulinRecords.isNotEmpty()) {
            val totalInsulin = insulinRecords.sumOf { it.dosage.toDouble() }
            sb.append("- 인슐린 총 투여량: ${String.format(Locale.getDefault(), "%.1f", totalInsulin)} U\n")
            sb.append("- 인슐린 기록: ${insulinRecords.size} 건\n")
        }
        sb.append("--------------------------------\n\n")

        // 3. 전체 상세 기록 (이 부분이 유저가 요청한 "그동안 기록한 거 다 보여줘")
        sb.append("📝 [전체 상세 내역]\n")
        
        val allRows = mutableListOf<PdfReportGenerator.UnifiedRow>()
        glucoseRecords.forEach { 
            allRows.add(PdfReportGenerator.UnifiedRow(it.timestamp, it.value.toString(), it.category, "-", it.memo))
        }
        insulinRecords.forEach {
            allRows.add(PdfReportGenerator.UnifiedRow(it.timestamp, "-", "-", "${it.dosage}U (${it.type})", it.memo))
        }
        
        if (allRows.isEmpty()) {
            sb.append("- 상세 기록이 없습니다.\n")
        } else {
            allRows.sortByDescending { it.timestamp }
            val timeDf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateDf = SimpleDateFormat("yyyy-MM-dd (E)", Locale.getDefault())
            var lastDay = ""
            
            allRows.forEach { row ->
                val currentDay = dateDf.format(Date(row.timestamp))
                if (currentDay != lastDay) {
                    if (lastDay.isNotEmpty()) sb.append("--------------------------------\n")
                    sb.append("🗓️ $currentDay\n")
                    lastDay = currentDay
                }
                
                val time = timeDf.format(Date(row.timestamp))
                sb.append("  [$time] ")
                if (row.glucose != "-") {
                    sb.append("혈당:${row.glucose}(${row.category}) ")
                }
                if (row.insulin != "-") {
                    sb.append("인슐린:${row.insulin} ")
                }
                if (row.memo.isNotEmpty()) {
                    sb.append("| 메모:${row.memo}")
                }
                sb.append("\n")
            }
        }
        sb.append("\n* 본 데이터는 혈당지킴이 앱에서 생성되었습니다.")
        
        // Hidden data for import
        sb.append("\n\n--- BACKUP DATA START ---\n")
        glucoseRecords.forEach {
            sb.append("G,${it.timestamp},${it.value},${it.category},,${it.memo}\n")
        }
        insulinRecords.forEach {
            sb.append("I,${it.timestamp},${it.type},${it.dosage},${it.injectionSite},${it.memo}\n")
        }
        sb.append("--- BACKUP DATA END ---")

        val fileName = "${reportTitle.replace(" ", "_").replace("/", "_")}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.txt"
        val tempFile = File(requireContext().cacheDir, fileName)
        tempFile.writeText(sb.toString())
        saveToPublicStorage(tempFile, "text/plain")
    }

    private fun importCsvData(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val reader = inputStream.bufferedReader()
            val content = reader.readText()
            
            val startIndex = content.indexOf("--- BACKUP DATA START ---")
            val endIndex = content.indexOf("--- BACKUP DATA END ---")
            
            if (startIndex == -1 || endIndex == -1) {
                Toast.makeText(requireContext(), "올바른 백업 파일이 아닙니다.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val dataSection = content.substring(startIndex + "--- BACKUP DATA START ---".length, endIndex).trim()
            val lines = dataSection.split("\n")
            
            val newGlucose = mutableListOf<GlucoseRecord>()
            val newInsulin = mutableListOf<InsulinRecord>()
            
            lines.forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 5) {
                    val type = parts[0]
                    val timestamp = parts[1].toLongOrNull() ?: return@forEach
                    
                    when (type) {
                        "G" -> {
                            val value = parts[2].toIntOrNull() ?: return@forEach
                            val category = parts[3]
                            val memo = if (parts.size > 5) parts[5] else ""
                            newGlucose.add(GlucoseRecord(value = value, timestamp = timestamp, category = category, memo = memo))
                        }
                        "I" -> {
                            val insulinType = parts[2]
                            val dosage = parts[3].toFloatOrNull() ?: return@forEach
                            val site = parts[4]
                            val memo = if (parts.size > 5) parts[5] else ""
                            newInsulin.add(InsulinRecord(type = insulinType, dosage = dosage, timestamp = timestamp, injectionSite = site, memo = memo))
                        }
                    }
                }
            }
            
            // Duplicate Detection
            val duplicateGlucose = newGlucose.filter { new -> 
                cachedGlucoseRecords.any { old -> old.timestamp == new.timestamp && old.value == new.value }
            }
            val duplicateInsulin = newInsulin.filter { new ->
                cachedInsulinRecords.any { old -> old.timestamp == new.timestamp && old.dosage == new.dosage }
            }
            
            val totalDuplicate = duplicateGlucose.size + duplicateInsulin.size
            
            if (totalDuplicate > 0) {
                AlertDialog.Builder(requireContext())
                    .setTitle("중복 기록 발견")
                    .setMessage("${totalDuplicate}건의 중복된 기록이 발견되었습니다. 어떻게 처리할까요?")
                    .setPositiveButton("중복 제외하고 가져오기") { _, _ ->
                        val finalG = newGlucose.filter { new -> !duplicateGlucose.contains(new) }
                        val finalI = newInsulin.filter { new -> !duplicateInsulin.contains(new) }
                        performImport(finalG, finalI)
                    }
                    .setNeutralButton("모두 가져오기") { _, _ ->
                        performImport(newGlucose, newInsulin)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            } else {
                performImport(newGlucose, newInsulin)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "불러오기 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performImport(glucoseList: List<GlucoseRecord>, insulinList: List<InsulinRecord>) {
        glucoseList.forEach { viewModel.insertGlucose(it.value, it.category, it.memo, it.timestamp) }
        insulinList.forEach { viewModel.insertInsulin(it.type, it.dosage, it.injectionSite, it.memo, it.timestamp) }
        
        Toast.makeText(requireContext(), "불러오기 완료: 혈당 ${glucoseList.size}건, 인슐린 ${insulinList.size}건", Toast.LENGTH_LONG).show()
    }

    private fun shareFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "건강 데이터 리포트 공유"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "파일 공유 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNotificationSwitches() {
        val sharedPref = requireActivity().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
        
        binding.switchMasterNotification.isChecked = sharedPref.getBoolean("notif_master", false)
        binding.switchMorning.isChecked = sharedPref.getBoolean("notif_morning", false)
        binding.switchLunch.isChecked = sharedPref.getBoolean("notif_lunch", false)
        binding.switchDinner.isChecked = sharedPref.getBoolean("notif_dinner", false)

        // 시간 텍스트 초기화
        binding.tvMorningTime.text = sharedPref.getString("notif_morning_time", "08:00")
        binding.tvLunchTime.text = sharedPref.getString("notif_lunch_time", "12:00")
        binding.tvDinnerTime.text = sharedPref.getString("notif_dinner_time", "18:00")

        binding.switchMasterNotification.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notif_master", isChecked).apply()
            if (isChecked) {
                scheduleAllAlarms()
            } else {
                cancelAlarms()
            }
        }

        binding.switchMorning.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notif_morning", isChecked).apply()
            if (binding.switchMasterNotification.isChecked) scheduleAllAlarms()
        }

        binding.switchLunch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notif_lunch", isChecked).apply()
            if (binding.switchMasterNotification.isChecked) scheduleAllAlarms()
        }

        binding.switchDinner.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notif_dinner", isChecked).apply()
            if (binding.switchMasterNotification.isChecked) scheduleAllAlarms()
        }

        // 시간 설정 클릭 리스너
        binding.btnMorningTime.setOnClickListener {
            showTimePicker("morning", binding.tvMorningTime.text.toString())
        }
        binding.btnLunchTime.setOnClickListener {
            showTimePicker("lunch", binding.tvLunchTime.text.toString())
        }
        binding.btnDinnerTime.setOnClickListener {
            showTimePicker("dinner", binding.tvDinnerTime.text.toString())
        }
    }

    private fun showTimePicker(key: String, currentTime: String) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            val sharedPref = requireActivity().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("notif_${key}_time", timeStr).apply()
            
            when(key) {
                "morning" -> binding.tvMorningTime.text = timeStr
                "lunch" -> binding.tvLunchTime.text = timeStr
                "dinner" -> binding.tvDinnerTime.text = timeStr
            }
            
            if (binding.switchMasterNotification.isChecked) {
                scheduleAllAlarms()
            }
        }, hour, minute, true).show()
    }

    private fun setupCollapsibleNotifications() {
        binding.btnToggleNotificationDetails.setOnClickListener {
            val isVisible = binding.layoutDetailNotifications.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutDetailNotifications.visibility = View.GONE
                binding.dividerNotification.visibility = View.GONE
                binding.ivExpandArrow.rotation = 0f
                binding.tvNotificationSubtitle.text = getString(R.string.notif_scheduler_desc_off)
            } else {
                binding.layoutDetailNotifications.visibility = View.VISIBLE
                binding.dividerNotification.visibility = View.VISIBLE
                binding.ivExpandArrow.rotation = 180f
                binding.tvNotificationSubtitle.text = getString(R.string.notif_scheduler_desc_on)
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("한국어 (Korean)", "English", "日本語 (Japanese)", "简体中文 (Chinese)", "Español (Spanish)")
        val languageCodes = arrayOf("ko", "en", "ja", "zh", "es")
        
        val currentLang = LocaleHelper.getLanguage(requireContext())
        val checkedItem = languageCodes.indexOf(currentLang).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.language_setting)
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLang = languageCodes[which]
                if (selectedLang != currentLang) {
                    LocaleHelper.setLocale(requireContext(), selectedLang)
                    // Restart Activity to apply changes
                    requireActivity().recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun scheduleAllAlarms() {
        val sharedPref = requireActivity().getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
        val master = sharedPref.getBoolean("notif_master", false)
        if (!master) {
            cancelAlarms()
            return
        }

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val keys = listOf("morning", "lunch", "dinner")

        keys.forEachIndexed { index, key ->
            val isEnabled = sharedPref.getBoolean("notif_$key", false)
            val timeStr = sharedPref.getString("notif_${key}_time", when(key) {
                "morning" -> "08:00"
                "lunch" -> "12:00"
                else -> "18:00"
            })!!
            
            val timeParts = timeStr.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            
            val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
                putExtra("reminder_type", key)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(), index, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (isEnabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun cancelAlarms() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0..2) {
            val intent = Intent(requireContext(), NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(), i, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("데이터 전체 초기화")
            .setMessage("정말로 모든 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("초기화") { _, _ ->
                viewModel.clearAllData()
                Toast.makeText(requireContext(), "모든 데이터가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
