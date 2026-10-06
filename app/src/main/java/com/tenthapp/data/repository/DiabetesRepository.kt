package com.example.glucoseguard.data.repository

import com.example.glucoseguard.data.dao.GlucoseDao
import com.example.glucoseguard.data.dao.InsulinDao
import com.example.glucoseguard.data.dao.MealDao
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import com.example.glucoseguard.data.database.AppDatabase
import com.example.glucoseguard.util.BackupCodec
import com.example.glucoseguard.util.GlucosePolicy

class DiabetesRepository(
    private val glucoseDao: GlucoseDao,
    private val insulinDao: InsulinDao,
    private val mealDao: MealDao,
    private val database: AppDatabase
) {
    val allGlucoseRecords: Flow<List<GlucoseRecord>> = glucoseDao.getAllRecords()
    val allInsulinRecords: Flow<List<InsulinRecord>> = insulinDao.getAllRecords()
    val allMealRecords: Flow<List<MealRecord>> = mealDao.getAllRecords()

    suspend fun insertGlucose(record: GlucoseRecord) {
        glucoseDao.insert(record)
    }

    suspend fun updateGlucose(record: GlucoseRecord) {
        glucoseDao.update(record)
    }

    suspend fun insertInsulin(record: InsulinRecord) {
        insulinDao.insert(record)
    }

    suspend fun updateInsulin(record: InsulinRecord) {
        insulinDao.update(record)
    }

    suspend fun insertMeal(record: MealRecord) {
        mealDao.insert(record)
    }

    suspend fun updateMeal(record: MealRecord) {
        mealDao.update(record)
    }

    suspend fun deleteGlucose(record: GlucoseRecord) {
        glucoseDao.delete(record)
    }

    suspend fun deleteInsulin(record: InsulinRecord) {
        insulinDao.delete(record)
    }

    suspend fun deleteMeal(record: MealRecord) {
        mealDao.delete(record)
    }

    suspend fun deleteAllData() = database.withTransaction {
        glucoseDao.deleteAll(); insulinDao.deleteAll(); mealDao.deleteAll()
    }
    suspend fun exportBackup(): BackupCodec.Records = database.withTransaction {
        BackupCodec.Records(glucoseDao.getAllRecords().first(), insulinDao.getAllRecords().first(), mealDao.getAllRecords().first())
    }
    suspend fun importBackup(data: BackupCodec.Records): Int = database.withTransaction {
        val glucose = glucoseDao.getAllRecords().first().map { it.copy(id = 0, category = GlucosePolicy.categoryCode(it.category)) }.toMutableSet()
        val insulin = insulinDao.getAllRecords().first().map { it.copy(id = 0) }.toMutableSet()
        val meals = mealDao.getAllRecords().first().map { it.copy(id = 0) }.toMutableSet()
        var count = 0
        data.glucose.forEach { if (glucose.add(it.copy(id = 0))) { glucoseDao.insert(it.copy(id = 0)); count++ } }
        data.insulin.forEach { if (insulin.add(it.copy(id = 0))) { insulinDao.insert(it.copy(id = 0)); count++ } }
        data.meals.forEach { if (meals.add(it.copy(id = 0))) { mealDao.insert(it.copy(id = 0)); count++ } }
        count
    }

    fun getGlucoseInRange(startTime: Long, endTime: Long): Flow<List<GlucoseRecord>> {
        return glucoseDao.getRecordsInRange(startTime, endTime)
    }

    fun getInsulinInRange(startTime: Long, endTime: Long): Flow<List<InsulinRecord>> {
        return insulinDao.getRecordsInRange(startTime, endTime)
    }
}
