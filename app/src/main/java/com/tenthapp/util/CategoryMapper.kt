package com.example.glucoseguard.util
import android.content.Context
import com.example.glucoseguard.R
object CategoryMapper {
    fun getTranslatedCategory(context: Context, category: String): String { return context.getString(when(GlucosePolicy.categoryCode(category)) {
        "fasting" -> R.string.chip_fasting
        "breakfast_before" -> R.string.chip_breakfast_before
        "breakfast_after" -> R.string.chip_breakfast_after
        "lunch_before" -> R.string.chip_lunch_before
        "lunch_after" -> R.string.chip_lunch_after
        "dinner_before" -> R.string.chip_dinner_before
        "dinner_after" -> R.string.chip_dinner_after
        "bedtime" -> R.string.chip_sleep_before
        else -> return category
    }) }
}
