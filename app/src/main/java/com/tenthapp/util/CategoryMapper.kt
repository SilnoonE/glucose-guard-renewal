package com.example.glucoseguard.util

import android.content.Context
import com.example.glucoseguard.R

object CategoryMapper {
    fun getTranslatedCategory(context: Context, category: String): String {
        return when (category) {
            "공복", "Fasting", "空腹時", "空腹", "Ayunas" -> context.getString(R.string.chip_fasting)
            "아침전", "Before Breakfast", "朝食前", "早餐前", "Antes del Desayuno" -> context.getString(R.string.chip_breakfast_before)
            "아침후", "After Breakfast", "朝食後", "早餐后", "Después del Desayuno" -> context.getString(R.string.chip_breakfast_after)
            "점심전", "Before Lunch", "昼食前", "午餐前", "Antes del Almuerzo" -> context.getString(R.string.chip_lunch_before)
            "점심후", "After Lunch", "昼食後", "午餐后", "Después del Almuerzo" -> context.getString(R.string.chip_lunch_after)
            "저녁전", "Before Dinner", "夕食前", "晚餐前", "Antes de la Cena" -> context.getString(R.string.chip_dinner_before)
            "저녁후", "After Dinner", "夕食後", "晚餐后", "Después de la Cena" -> context.getString(R.string.chip_dinner_after)
            "취침전", "Before Bed", "就寝前", "睡前", "Antes de Dormir" -> context.getString(R.string.chip_sleep_before)
            else -> category
        }
    }
}
