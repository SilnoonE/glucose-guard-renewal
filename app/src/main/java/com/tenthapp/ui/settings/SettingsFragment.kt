package com.example.glucoseguard.ui.settings
import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.glucoseguard.*
import com.example.glucoseguard.databinding.FragmentSettingsBinding
import com.example.glucoseguard.report.*
import com.example.glucoseguard.ui.viewmodel.*
import com.example.glucoseguard.util.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DiabetesViewModel by activityViewModels { DiabetesViewModelFactory((requireActivity().application as DiabetesApplication).repository) }
    private val repository get() = (requireActivity().application as DiabetesApplication).repository
    private var reportOpened = false
    private var busy = false
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            requireContext().getSharedPreferences("diabetes_prefs",Context.MODE_PRIVATE).edit().putBoolean("notif_master",false).apply()
            _binding?.switchMasterNotification?.isChecked = false
            toast(R.string.notif_permission_denied)
        }
        ReminderScheduler.schedule(requireContext())
    }
    private val saveBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if(uri != null) {
            val context = requireContext().applicationContext
            val repo = repository
            runOperation {
                withContext(Dispatchers.IO) {
                    val text = BackupCodec.write(repo.exportBackup())
                    context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                        ?: error("파일을 열 수 없습니다.")
                }
                toast("모든 기록을 백업했습니다.")
            }
        }
    }
    private val importBackup = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) {
            val context = requireContext().applicationContext
            runOperation {
                val records = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        val data=StringBuilder();val buffer=CharArray(8192)
                        while(true) { val count=it.read(buffer);if(count<0) break;data.append(buffer,0,count);require(data.length <= 10_000_000) { "백업 파일이 너무 큽니다." } }
                        data.toString()
                    } ?: error("파일을 읽을 수 없습니다.")
                    BackupCodec.read(text)
                }
                if(isAdded && _binding != null) MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.restore_action)
                    .setMessage("혈당 ${records.glucose.size}건 · 인슐린 ${records.insulin.size}건 · 건강 메모 ${records.meals.size}건\n기존 기록은 유지하고 동일한 기록은 중복 저장하지 않습니다.")
                    .setPositiveButton("불러오기") { _, _ ->
                        setBusy(true)
                        viewModel.importBackup(records) { result ->
                            if(_binding != null && isAdded) {
                                setBusy(false)
                                toast(result.fold({ "${it}건을 불러왔습니다." },{ "불러오기에 실패했습니다. 기존 기록은 유지됩니다." }))
                            }
                        }
                    }.setNegativeButton(android.R.string.cancel,null).show()
            }
        }
    }
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) {
            val context=requireContext().applicationContext
            runOperation {
                withContext(Dispatchers.IO) {
                    val file=File(context.filesDir,"profile_image.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: error("이미지를 읽을 수 없습니다.")
                    context.getSharedPreferences("diabetes_prefs",Context.MODE_PRIVATE).edit().putString("profile_image_path",file.absolutePath).apply()
                }
                toast("이미지를 저장했습니다.")
            }
        }
    }
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding=FragmentSettingsBinding.inflate(inflater,container,false);return binding.root
    }
    override fun onViewCreated(view: View,savedInstanceState: Bundle?) {
        childFragmentManager.setFragmentResultListener(ReportOptionsBottomSheet.TAG,viewLifecycleOwner) { _,result ->
            generateReport(result.getInt("days",14),ReportOptions(true,true,true,true,true,true,true,true))
        }
        reportOpened=savedInstanceState?.getBoolean("reportOpened") ?: false
        setupNotifications()
        binding.btnCreateReport.setOnClickListener { showReportOptions() }
        binding.btnExportData.setOnClickListener { saveBackup.launch("혈당지킴이_백업_${SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(Date())}.txt") }
        binding.btnImportData.setOnClickListener { importBackup.launch("*/*") }
        binding.btnChangeProfileImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnClearData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("모든 기록 삭제").setMessage("백업하지 않은 기록은 복원할 수 없습니다. 모든 기록을 삭제할까요?")
                .setPositiveButton(R.string.delete_action) { _, _ -> viewModel.clearAllData() }.setNegativeButton(android.R.string.cancel,null).show()
        }
        val destinations=listOf(binding.btnGuideManage to R.id.guideManageFragment,binding.btnGuideTarget to R.id.guideTargetFragment,binding.btnGuideHigh to R.id.guideHighFragment,binding.btnGuideLow to R.id.guideLowFragment,binding.btnGuideFood to R.id.guideFoodFragment,binding.btnHealthCalculatorTop to R.id.healthCalculatorFragment)
        destinations.forEach { (button,id) -> button.setOnClickListener { findNavController().navigate(id) } }
        binding.btnToggleGuideDetails.setOnClickListener {
            val show=binding.layoutGuideItems.visibility != View.VISIBLE
            binding.layoutGuideItems.visibility=if(show) View.VISIBLE else View.GONE
            binding.dividerGuide.visibility=if(show) View.VISIBLE else View.GONE
            binding.ivGuideExpandArrow.rotation=if(show) 180f else 0f
        }
        binding.btnToggleNotificationDetails.setOnClickListener {
            val show=binding.layoutDetailNotifications.visibility != View.VISIBLE
            binding.layoutDetailNotifications.visibility=if(show) View.VISIBLE else View.GONE
            binding.dividerNotification.visibility=if(show) View.VISIBLE else View.GONE
            binding.ivExpandArrow.rotation=if(show) 180f else 0f
        }
        binding.btnChangeLanguage.setOnClickListener {
            val codes=arrayOf("ko","en","ja","zh","es")
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.language_setting)
                .setSingleChoiceItems(arrayOf("한국어","English","日本語","简体中文","Español"),codes.indexOf(LocaleHelper.getLanguage(requireContext())).coerceAtLeast(0)) { dialog,index ->
                    LocaleHelper.setLocale(requireContext(),codes[index]);dialog.dismiss();requireActivity().recreate()
                }.setNegativeButton(android.R.string.cancel,null).show()
        }
        viewModel.allGlucoseRecords.observe(viewLifecycleOwner) {
            if(arguments?.getBoolean("openReport") == true && !reportOpened) {
                reportOpened=true; arguments?.putBoolean("openReport",false);showReportOptions()
            }
        }
    }
    private fun showReportOptions() {
        if(busy || childFragmentManager.findFragmentByTag(ReportOptionsBottomSheet.TAG)!=null) return
        ReportOptionsBottomSheet().show(childFragmentManager,ReportOptionsBottomSheet.TAG)
    }
    private fun generateReport(days: Int,options: ReportOptions) {
        val context=requireContext().applicationContext
        val repo=repository
        val target=TargetPreferences.read(context)
        runOperation {
            toast(R.string.report_generating)
            val file=withContext(Dispatchers.IO) {
                val snapshot=repo.exportBackup()
                val data=ReportDataBuilder().buildReportData(days,snapshot.glucose,snapshot.insulin,snapshot.meals,target)
                require(data.glucoseRecords.isNotEmpty() || data.insulinRecords.isNotEmpty() || data.mealRecords.isNotEmpty()) { context.getString(R.string.report_empty_period) }
                PdfReportGenerator(context).generateReport(data,options) ?: error("PDF를 저장하지 못했습니다.")
            }
            if(isAdded) MaterialAlertDialogBuilder(requireContext()).setTitle("진료용 리포트 준비 완료").setMessage("선택한 기간의 기록을 PDF로 정리했습니다.")
                .setPositiveButton("공유하기") { _, _ -> sharePdf(file) }.setNeutralButton("열기") { _, _ -> openPdf(file) }.setNegativeButton("닫기",null).show()
        }
    }
    private fun pdfUri(file: File): Uri = FileProvider.getUriForFile(requireContext(),"${requireContext().packageName}.fileprovider",file)
    private fun sharePdf(file: File) {
        try { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM,pdfUri(file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"리포트 공유")) }
        catch(e: Exception) { toast("PDF를 공유할 수 있는 앱을 확인해주세요.") }
    }
    private fun openPdf(file: File) {
        (requireActivity() as MainActivity).showPdfInterstitial {
            if(isAdded) openPdfDirect(file)
        }
    }
    private fun openPdfDirect(file: File) {
        try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(pdfUri(file),"application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
        catch(e: Exception) { toast("PDF를 열 수 있는 앱을 확인해주세요.") }
    }
    private fun setupNotifications() {
        val prefs=requireContext().getSharedPreferences("diabetes_prefs",Context.MODE_PRIVATE)
        binding.switchMasterNotification.isChecked=prefs.getBoolean("notif_master",false)
        val controls=listOf(Triple("morning",binding.switchMorning,binding.tvMorningTime),Triple("lunch",binding.switchLunch,binding.tvLunchTime),Triple("dinner",binding.switchDinner,binding.tvDinnerTime))
        controls.forEachIndexed { index,(key,switch,time) ->
            switch.isChecked=prefs.getBoolean("notif_$key",false)
            time.text=prefs.getString("notif_${key}_time",if(index==0) "08:00" else if(index==1) "12:00" else "18:00")
            switch.setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("notif_$key",checked).apply();ReminderScheduler.schedule(requireContext()) }
        }
        binding.switchMasterNotification.setOnCheckedChangeListener { _,checked ->
            prefs.edit().putBoolean("notif_master",checked).apply()
            if(checked) {
                if(!controls.any { it.second.isChecked }) binding.switchMorning.isChecked=true
                if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else if(!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) { binding.switchMasterNotification.isChecked=false;toast(R.string.notif_permission_denied) }
            }
            ReminderScheduler.schedule(requireContext())
        }
        val buttons=listOf(binding.btnMorningTime,binding.btnLunchTime,binding.btnDinnerTime)
        controls.forEachIndexed { index,(key,_,label) -> buttons[index].setOnClickListener {
            val parts=label.text.toString().split(":")
            TimePickerDialog(requireContext(),{ _,hour,minute ->
                val time=String.format(Locale.ROOT,"%02d:%02d",hour,minute)
                label.text=time;prefs.edit().putString("notif_${key}_time",time).apply();ReminderScheduler.schedule(requireContext())
            },parts[0].toIntOrNull() ?: 8,parts.getOrNull(1)?.toIntOrNull() ?: 0,true).show()
        } }
    }
    private fun runOperation(action: suspend () -> Unit) {
        if(busy) return
        viewLifecycleOwner.lifecycleScope.launch {
            setBusy(true)
            try { action() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { toast(e.message ?: "작업을 완료하지 못했습니다.") }
            finally { if(_binding != null) setBusy(false) }
        }
    }
    private fun setBusy(value: Boolean) {
        busy=value
        _binding?.let { b -> listOf(b.btnCreateReport,b.btnExportData,b.btnImportData,b.btnClearData).forEach { it.isEnabled=!value } }
    }
    private fun toast(message: String) { if(isAdded) Toast.makeText(requireContext(),message,Toast.LENGTH_LONG).show() }
    private fun toast(id: Int) { if(isAdded) toast(getString(id)) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("reportOpened",reportOpened);super.onSaveInstanceState(outState) }
    override fun onDestroyView() { _binding=null;busy=false;super.onDestroyView() }
}
