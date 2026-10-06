package com.example.glucoseguard.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Locale

class HealthCalculatorViewModel : ViewModel() {

    private val _bmiValue = MutableLiveData<Double>()
    val bmiValue: LiveData<Double> = _bmiValue

    private val _bmiStatus = MutableLiveData<String>()
    val bmiStatus: LiveData<String> = _bmiStatus

    private val _recommendedWeight = MutableLiveData<Double>()
    val recommendedWeight: LiveData<Double> = _recommendedWeight

    private val _waterIntake = MutableLiveData<Double>()
    val waterIntake: LiveData<Double> = _waterIntake

    private val _bmr = MutableLiveData<Double>()
    val bmr: LiveData<Double> = _bmr

    private val _comment = MutableLiveData<String>()
    val comment: LiveData<String> = _comment

    fun calculate(height: Double, weight: Double, age: Int, isMale: Boolean) {
        val heightMeters = height / 100.0
        
        // 1. BMI
        val bmi = weight / (heightMeters * heightMeters)
        _bmiValue.value = bmi
        
        // BMI Status (Korean Standard)
        val status = when {
            bmi < 18.5 -> "저체중"
            bmi < 23.0 -> "정상"
            bmi < 25.0 -> "과체중"
            else -> "비만"
        }
        _bmiStatus.value = status

        // 2. Recommended Weight (BMI 22)
        _recommendedWeight.value = 22.0 * (heightMeters * heightMeters)

        // 3. Water Intake (Weight * 30ml = Weight * 0.03L)
        _waterIntake.value = weight * 0.03

        // 4. BMR (Mifflin-St Jeor)
        val bmrValue = if (isMale) {
            10.0 * weight + 6.25 * height - 5.0 * age + 5.0
        } else {
            10.0 * weight + 6.25 * height - 5.0 * age - 161.0
        }
        _bmr.value = bmrValue

        // 5. Comment
        val commentText = when (status) {
            "저체중" -> "저체중 단계입니다. 균형 잡힌 식단으로 적정 체중을 유지하는 것이 중요합니다."
            "정상" -> "정상 체중입니다. 현재의 건강한 생활 습관을 꾸준히 유지해 주세요."
            "과체중" -> "과체중 단계입니다. 규칙적인 운동과 식단 관리를 통해 체중을 조절하는 것이 도움이 됩니다."
            "비만" -> "비만 단계입니다. 건강을 위해 전문가의 도움을 받아 체계적인 관리를 시작하는 것을 권장합니다."
            else -> ""
        }
        _comment.value = commentText
    }
}
