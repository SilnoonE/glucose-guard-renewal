package com.example.glucoseguard

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import com.example.glucoseguard.data.database.AppDatabase
import com.example.glucoseguard.data.model.*
import com.example.glucoseguard.data.repository.DiabetesRepository
import com.example.glucoseguard.util.BackupCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RenewalInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val app get() = context.applicationContext as DiabetesApplication
    private fun reset() = runBlocking(Dispatchers.IO) { app.database.clearAllTables() }
    private fun screenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun waitForText(id: Int,text: String) {
        var error: Throwable?=null
        repeat(40) {
            try { onView(withId(id)).check(matches(withText(text)));return }
            catch(e: Throwable) { error=e;Thread.sleep(100) }
        }
        throw error ?: AssertionError(text)
    }
    @Test fun firstRecordCanBeSavedAndVisibleOnHome() {
        reset()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText(R.id.tv_last_glucose,"—")
            screenshot("01_home_empty")
            onView(withId(R.id.btn_record_glucose)).perform(scrollTo(),click())
            onView(withId(R.id.et_glucose_value)).perform(replaceText("150"))
            closeSoftKeyboard()
            onView(withId(R.id.chip_lunch_after)).perform(scrollTo(),click())
            screenshot("02_glucose_input")
            onView(withId(R.id.btn_save)).perform(scrollTo(),click())
            waitForText(R.id.tv_last_glucose,"150")
            onView(withId(R.id.tv_status_title)).check(matches(withText(R.string.within_target)))
            Thread.sleep(2300)
            screenshot("03_home_saved")
            runBlocking(Dispatchers.IO) { assertEquals("lunch_after",app.repository.allGlucoseRecords.first().single().category) }
        }
    }
    @Test fun latestAndExtremeReadingsRemainVisibleWithHealthMemo() {
        reset()
        val now=System.currentTimeMillis()
        runBlocking(Dispatchers.IO) {
            app.repository.insertGlucose(GlucoseRecord(value=50,timestamp=now-60000,category="fasting"))
            app.repository.insertGlucose(GlucoseRecord(value=250,timestamp=now-30000,category="lunch_after"))
            app.repository.insertMeal(MealRecord(timestamp=now-10000,memo="산책 20분, 식후 기록"))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var lowText=""
            scenario.onActivity { lowText=it.getString(R.string.low_record_note,1) }
            waitForText(R.id.tv_last_glucose,"250")
            onView(withId(R.id.tv_status_title)).check(matches(withText(R.string.above_target)))
            onView(withId(R.id.tv_insight_text)).check(matches(withText(lowText)))
            screenshot("04_home_extremes")
            onView(withId(R.id.navigation_settings)).perform(click())
            onView(withId(R.id.btn_create_report)).check(matches(isDisplayed()))
            screenshot("05_management")
            onView(withId(R.id.navigation_chart)).perform(click())
            screenshot("06_summary")
        }
    }
    @Test fun pdfIncludesAllRecordTypesAndCanBeRendered() {
        val now=System.currentTimeMillis()
        val data=com.example.glucoseguard.report.ReportDataBuilder().buildReportData(7,
            listOf(GlucoseRecord(value=150,timestamp=now,category="lunch_after",memo="밥, 산책")),
            listOf(InsulinRecord(type="기록한 제품",dosage=1.5f,timestamp=now)),
            listOf(MealRecord(timestamp=now,memo="건강 메모\n".repeat(250))))
        val options=com.example.glucoseguard.report.ReportOptions(true,true,true,true,true,true,true,true)
        val file=com.example.glucoseguard.report.PdfReportGenerator(com.example.glucoseguard.util.LocaleHelper.onAttach(context)).generateReport(data,options)!!
        assertTrue(file.length()>1000)
        android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(file,android.os.ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount>0)
            renderer.openPage(0).use { page ->
                val bitmap=Bitmap.createBitmap(page.width*2,page.height*2,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val dir=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
                File(dir,"07_report.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                file.copyTo(File(dir,"sample_report.pdf"),overwrite=true)
                bitmap.recycle()
            }
        }
    }
    @Test fun dialogsSurviveActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.btn_more_record)).perform(scrollTo(),click())
            onView(withId(R.id.btn_add_meal)).inRoot(isDialog()).perform(click())
            scenario.recreate()
            onView(withId(R.id.et_memo)).inRoot(isDialog()).perform(replaceText("재생성 후 메모"))
            closeSoftKeyboard()
            onView(withId(R.id.btn_save)).inRoot(isDialog()).perform(click())
            repeat(40) {
                if(runBlocking(Dispatchers.IO) { app.repository.allMealRecords.first().any { it.memo == "재생성 후 메모" } }) return@repeat
                Thread.sleep(100)
            }
            Thread.sleep(600)
            onView(withId(R.id.navigation_settings)).perform(click())
            onView(withId(R.id.btn_create_report)).perform(click())
            scenario.recreate()
            onView(withId(R.id.btn_generate_pdf)).inRoot(isDialog()).check(matches(isDisplayed()))
        }
    }
    @Test fun notificationOpensInputDirectly() {
        ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("open_glucose",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).use {
            onView(withId(R.id.et_glucose_value)).check(matches(isDisplayed()))
        }
    }
    @Test fun migrationRetainsVersionOneReadings() {
        val name="renewal_migration_fixture.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.execSQL("CREATE TABLE glucose_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,value INTEGER NOT NULL,timestamp INTEGER NOT NULL,category TEXT NOT NULL,memo TEXT NOT NULL)")
            db.execSQL("CREATE TABLE insulin_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,type TEXT NOT NULL,dosage REAL NOT NULL,timestamp INTEGER NOT NULL,injectionSite TEXT NOT NULL,memo TEXT NOT NULL)")
            db.execSQL("INSERT INTO glucose_records VALUES (1,123,1000,'공복','이전 기록')")
            db.execSQL("INSERT INTO insulin_records VALUES (1,'제품명',1.5,1001,'복부','보존할 메모')")
            db.version=1
        }
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            runBlocking(Dispatchers.IO) {
                assertEquals(123,db.glucoseDao().getAllRecords().first().single().value)
                assertEquals("보존할 메모",db.insulinDao().getAllRecords().first().single().memo)
                assertTrue(db.mealDao().getAllRecords().first().isEmpty())
            }
        } finally { db.close();context.deleteDatabase(name) }
    }
    @Test fun importIsIdempotentAndRollsBackOnStorageFailure() {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val repo=DiabetesRepository(db.glucoseDao(),db.insulinDao(),db.mealDao(),db)
        try {
            runBlocking(Dispatchers.IO) {
                val data=BackupCodec.Records(listOf(GlucoseRecord(value=110,timestamp=1000,category="fasting",memo="밥,산책\n")),emptyList(),listOf(MealRecord(timestamp=1001,memo="건강 메모\n".repeat(250))))
                assertEquals(2,repo.importBackup(data));assertEquals(0,repo.importBackup(data))
                assertEquals(1,repo.exportBackup().meals.size)
                db.openHelper.writableDatabase.execSQL("CREATE TRIGGER test_fail BEFORE INSERT ON insulin_records BEGIN SELECT RAISE(ABORT,'test storage failure'); END")
                try {
                    repo.importBackup(BackupCodec.Records(listOf(GlucoseRecord(value=120,timestamp=2000,category="fasting")),listOf(InsulinRecord(type="일반",dosage=1f,timestamp=2001)),emptyList()))
                    fail("Expected a rollback")
                } catch(e: android.database.SQLException) { /* transaction must roll back the glucose insert */ }
                assertEquals(1,repo.allGlucoseRecords.first().size)
            }
        } finally { db.close() }
    }
}
