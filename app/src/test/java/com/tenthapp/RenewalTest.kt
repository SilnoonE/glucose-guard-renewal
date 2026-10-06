package com.example.glucoseguard

import com.example.glucoseguard.data.model.*
import com.example.glucoseguard.util.*
import com.example.glucoseguard.report.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RenewalTest {
    @Test fun measurementContextAndCustomTargetsAgree() {
        assertEquals(GlucosePolicy.Status.ABOVE_TARGET,GlucosePolicy.classify(150,"공복"))
        assertEquals(GlucosePolicy.Status.IN_RANGE,GlucosePolicy.classify(150,"After Lunch"))
        assertEquals(GlucosePolicy.Status.IN_RANGE,GlucosePolicy.classify(150,"lunch_after"))
        assertEquals(GlucosePolicy.Status.ABOVE_TARGET,GlucosePolicy.classify(150,"lunch_after",GlucosePolicy.Target(80,120,140)))
        assertEquals(GlucosePolicy.Status.LOW,GlucosePolicy.classify(50,"lunch_after",GlucosePolicy.Target(80,120,140)))
        assertEquals(GlucosePolicy.Status.BELOW_TARGET,GlucosePolicy.classify(75,"fasting",GlucosePolicy.Target(80,120,140)))
    }
    @Test fun backupPreservesAllFieldsAcrossLanguagesAndDelimiters() {
        val data=BackupCodec.Records(
            listOf(GlucoseRecord(value=150,timestamp=123,category="lunch_after",memo="밥, 산책\n日本語 😀 % +")),
            listOf(InsulinRecord(type="속효성,제품",dosage=1.5f,timestamp=124,injectionSite="복부\n좌측",memo="원래 메모\t문자")),
            listOf(MealRecord(timestamp=125,memo="건강 메모\n쉼표,줄바꿈")))
        assertEquals(data,BackupCodec.read(BackupCodec.write(data)))
    }
    @Test fun legacyBackupRetainsCommaInsideMemoAndNormalizesCategory() {
        val data=BackupCodec.read("--- BACKUP DATA START ---\nG,123,150,점심후,,밥, 산책\n--- BACKUP DATA END ---")
        assertEquals("밥, 산책",data.glucose.single().memo)
        assertEquals("lunch_after",data.glucose.single().category)
    }
    @Test(expected=IllegalArgumentException::class) fun malformedBackupFailsBeforeImport() {
        BackupCodec.read(BackupCodec.HEADER+"\nG\t123\tnot-a-number\tfasting\tmemo")
    }
    @Test fun invalidInputsDoNotReachStorage() {
        assertFalse(GlucosePolicy.validGlucose("999999999999".toIntOrNull()))
        assertFalse(GlucosePolicy.validGlucose(0))
        assertFalse(GlucosePolicy.validDosage(Float.NaN))
        assertFalse(GlucosePolicy.validDosage(Float.POSITIVE_INFINITY))
        assertFalse(GlucosePolicy.validDosage(0f))
    }
    @Test fun reportPreservesExtremeReadingsRatherThanCallingTheirMeanStable() {
        val now=System.currentTimeMillis()
        val report=ReportDataBuilder().buildReportData(7,listOf(
            GlucoseRecord(value=50,timestamp=now-1000,category="fasting"),
            GlucoseRecord(value=250,timestamp=now-500,category="lunch_after")),emptyList())
        assertEquals(150,report.stats.avgGlucose)
        assertEquals(1,report.stats.lowCount);assertEquals(1,report.stats.highCount)
        assertEquals(0,report.stats.normalRangePercentage)
        assertFalse(report.analysisItems.any { it.contains("매우 안정") || it.contains("용량 증가") })
    }
    @Test fun sevenCalendarDayReportExcludesEighthDayAndFutureRecords() {
        val start=Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR,-6);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis
        val data=ReportDataBuilder().buildReportData(7,listOf(
            GlucoseRecord(value=100,timestamp=start-1,category="fasting"),
            GlucoseRecord(value=110,timestamp=start,category="fasting"),
            GlucoseRecord(value=200,timestamp=System.currentTimeMillis()+86400000L,category="fasting")),emptyList())
        assertEquals(1,data.glucoseRecords.size);assertEquals(110,data.stats.avgGlucose)
    }
    @Test fun allTimeReportIncludesMealOnlyHistory() {
        val meal=MealRecord(timestamp=123,memo="식사 메모")
        assertEquals(listOf(meal),ReportDataBuilder().buildReportData(-1,emptyList(),emptyList(),listOf(meal)).mealRecords)
    }
}
