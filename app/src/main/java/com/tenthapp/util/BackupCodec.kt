package com.example.glucoseguard.util

import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import java.net.URLEncoder
import java.net.URLDecoder

/** Versioned, UTF-8 encoded fields preserve commas, newlines and translated legacy data. */
object BackupCodec {
    const val HEADER = "GLUCOSE_GUARDIAN_BACKUP_V2"
    data class Records(val glucose: List<GlucoseRecord>, val insulin: List<InsulinRecord>, val meals: List<MealRecord>)
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun decode(value: String) = URLDecoder.decode(value, "UTF-8")
    fun write(data: Records): String = buildString {
        appendLine(HEADER)
        data.glucose.forEach { appendLine(listOf("G", it.timestamp, it.value, encode(GlucosePolicy.categoryCode(it.category)), encode(it.memo)).joinToString("\t")) }
        data.insulin.forEach { appendLine(listOf("I", it.timestamp, encode(it.type), it.dosage, encode(it.injectionSite), encode(it.memo)).joinToString("\t")) }
        data.meals.forEach { appendLine(listOf("M", it.timestamp, encode(it.memo)).joinToString("\t")) }
    }
    fun read(text: String): Records {
        val glucose = mutableListOf<GlucoseRecord>()
        val insulin = mutableListOf<InsulinRecord>()
        val meals = mutableListOf<MealRecord>()
        val current = text.trimStart().startsWith(HEADER)
        val lines = if (current) text.trimStart().lineSequence().drop(1).toList() else {
            val start = text.indexOf("--- BACKUP DATA START ---")
            val end = text.indexOf("--- BACKUP DATA END ---")
            require(start >= 0 && end > start) { "백업 형식을 확인해주세요." }
            text.substring(start + "--- BACKUP DATA START ---".length, end).trim().lines()
        }
        lines.filter { it.isNotBlank() }.forEachIndexed { index, line ->
            try {
                val p = if (current) line.split('\t') else line.split(',', limit = 6)
                val time = p[1].toLong().also { require(it >= 0) }
                when (p[0]) {
                    "G" -> {
                        val value = p[2].toInt().also { require(GlucosePolicy.validGlucose(it)) }
                        glucose += GlucoseRecord(value = value, timestamp = time,
                            category = GlucosePolicy.categoryCode(if (current) decode(p[3]) else p[3]),
                            memo = if (current) decode(p[4]) else p.getOrElse(5) { "" })
                    }
                    "I" -> {
                        val dosage = p[3].toFloat().also { require(GlucosePolicy.validDosage(it)) }
                        insulin += InsulinRecord(type = if (current) decode(p[2]) else p[2], dosage = dosage,
                            timestamp = time, injectionSite = if (current) decode(p[4]) else p[4],
                            memo = if (current) decode(p[5]) else p.getOrElse(5) { "" })
                    }
                    "M" -> { require(current); meals += MealRecord(timestamp = time, memo = decode(p[2])) }
                    else -> error("알 수 없는 기록 유형")
                }
            } catch (e: Exception) { throw IllegalArgumentException("${index + 1}번째 기록을 읽을 수 없습니다. 기존 기록은 변경되지 않았습니다.", e) }
        }
        return Records(glucose, insulin, meals)
    }
}
