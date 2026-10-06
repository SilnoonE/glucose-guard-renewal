package com.example.glucoseguard.util

import android.content.Context

object TargetPreferences {
    fun read(context: Context): GlucosePolicy.Target {
        val prefs = context.getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE)
        val min = prefs.getInt("target_min", 70).coerceIn(70, 200)
        val legacyMax = if (prefs.contains("target_max")) prefs.getInt("target_max", 180) else null
        return GlucosePolicy.Target(min,
            prefs.getInt("target_before_max", legacyMax ?: 130).coerceIn(min + 1, 600),
            prefs.getInt("target_after_max", legacyMax ?: 180).coerceIn(min + 1, 600))
    }
    fun save(context: Context, target: GlucosePolicy.Target) {
        context.getSharedPreferences("diabetes_prefs", Context.MODE_PRIVATE).edit()
            .putInt("target_min", target.min).putInt("target_before_max", target.beforeMax)
            .putInt("target_after_max", target.afterMax).apply()
    }
}
