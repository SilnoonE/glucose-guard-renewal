package com.example.glucoseguard.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.glucoseguard.R
import com.example.glucoseguard.databinding.FragmentHealthCalculatorBinding
import com.example.glucoseguard.ui.viewmodel.HealthCalculatorViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Locale

class HealthCalculatorFragment : Fragment() {
    private var _binding: FragmentHealthCalculatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthCalculatorViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveResultAsImage()
        } else {
            Toast.makeText(requireContext(), "파일 저장 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenderDropdown()
        setupObservers()

        binding.btnCalculate.setOnClickListener {
            calculate()
        }

        binding.btnSaveImage.setOnClickListener {
            saveResultAsImage()
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("남성", "여성")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        binding.tvGender.setAdapter(adapter)
        if (binding.tvGender.text.isEmpty()) {
            binding.tvGender.setText(genders[0], false)
        }
    }

    private fun setupObservers() {
        viewModel.bmiValue.observe(viewLifecycleOwner) { bmi ->
            binding.layoutResultCapture.visibility = View.VISIBLE
            binding.btnSaveImage.visibility = View.VISIBLE
            binding.tvBmiValue.text = String.format(Locale.getDefault(), "%.1f", bmi)
        }

        viewModel.bmiStatus.observe(viewLifecycleOwner) { status ->
            binding.tvBmiStatus.text = status
            val color = when (status) {
                "정상" -> ContextCompat.getColor(requireContext(), R.color.status_normal)
                "저체중" -> ContextCompat.getColor(requireContext(), R.color.status_low)
                "과체중" -> ContextCompat.getColor(requireContext(), R.color.status_warning)
                "비만" -> ContextCompat.getColor(requireContext(), R.color.status_high)
                else -> ContextCompat.getColor(requireContext(), R.color.text_sub)
            }
            // Update Chip Color
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_chip)?.mutate() as? GradientDrawable
            drawable?.setColor(color)
            binding.tvBmiStatus.background = drawable
        }

        viewModel.recommendedWeight.observe(viewLifecycleOwner) { weight ->
            binding.tvRecommendedWeight.text = String.format(Locale.getDefault(), "%.1f kg", weight)
        }

        viewModel.waterIntake.observe(viewLifecycleOwner) { water ->
            binding.tvWaterIntake.text = String.format(Locale.getDefault(), "%.1f L", water)
        }

        viewModel.bmr.observe(viewLifecycleOwner) { bmr ->
            binding.tvBmr.text = String.format(Locale.getDefault(), "%,.0f kcal", bmr)
        }

        viewModel.comment.observe(viewLifecycleOwner) { comment ->
            binding.tvComment.text = comment
        }
    }

    private fun calculate() {
        val heightStr = binding.etHeight.text.toString()
        val weightStr = binding.etWeight.text.toString()
        val ageStr = binding.etAge.text.toString()
        val genderStr = binding.tvGender.text.toString()

        if (heightStr.isEmpty() || weightStr.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(requireContext(), "모든 정보를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val height = heightStr.toDoubleOrNull() ?: 0.0
        val weight = weightStr.toDoubleOrNull() ?: 0.0
        val age = ageStr.toIntOrNull() ?: 0

        if (height <= 0 || weight <= 0 || age <= 0) {
            Toast.makeText(requireContext(), "올바른 숫자를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val isMale = genderStr == "남성"
        viewModel.calculate(height, weight, age, isMale)
    }

    private fun saveResultAsImage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }

        val view = binding.layoutResultCapture
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        view.draw(canvas)

        val fileName = "Health_Report_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DiabetesCare")
                }
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val folder = File(imagesDir, "DiabetesCare")
                if (!folder.exists()) folder.mkdirs()
                val file = File(folder, fileName)
                outputStream = FileOutputStream(file)
            }

            outputStream?.use { 
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(requireContext(), "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
