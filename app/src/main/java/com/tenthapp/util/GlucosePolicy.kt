package com.example.glucoseguard.util

/** Shared interpretation of manually entered readings; never a diagnosis. */
object GlucosePolicy {
    enum class Status { LOW, BELOW_TARGET, IN_RANGE, ABOVE_TARGET }
    data class Target(val min: Int = 70, val beforeMax: Int = 130, val afterMax: Int = 180) {
        fun upper(category: String): Int = if (isBeforeMeal(category)) beforeMax else afterMax
    }
    private val categories = mapOf(
        "fasting" to listOf("공복", "Fasting", "空腹時", "空腹", "Ayunas"),
        "breakfast_before" to listOf("아침전", "Before Breakfast", "朝食前", "早餐前", "Antes del Desayuno"),
        "breakfast_after" to listOf("아침후", "After Breakfast", "朝食後", "早餐后", "Después del Desayuno"),
        "lunch_before" to listOf("점심전", "Before Lunch", "昼食前", "午餐前", "Antes del Almuerzo"),
        "lunch_after" to listOf("점심후", "After Lunch", "昼食後", "午餐后", "Después del Almuerzo"),
        "dinner_before" to listOf("저녁전", "Before Dinner", "夕食前", "晚餐前", "Antes de la Cena"),
        "dinner_after" to listOf("저녁후", "After Dinner", "夕食後", "晚餐后", "Después de la Cena"),
        "bedtime" to listOf("취침전", "Before Bed", "就寝前", "睡前", "Antes de Dormir"),
        "other" to listOf("기타", "Other")
    )
    fun categoryCode(value: String): String = categories.entries.firstOrNull {
        it.key == value || value in it.value
    }?.key ?: value
    fun isBeforeMeal(category: String): Boolean = categoryCode(category).let { it == "fasting" || it.endsWith("_before") }
    fun classify(value: Int, category: String, target: Target = Target()): Status = when {
        value < 70 -> Status.LOW
        value < target.min -> Status.BELOW_TARGET
        value > target.upper(category) -> Status.ABOVE_TARGET
        else -> Status.IN_RANGE
    }
    fun validGlucose(value: Int?): Boolean = value != null && value in 1..1500
    fun validDosage(value: Float?): Boolean = value != null && value.isFinite() && value > 0f && value <= 1000f
}
