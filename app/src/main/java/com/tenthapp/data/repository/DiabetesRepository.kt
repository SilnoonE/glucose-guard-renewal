package com.example.glucoseguard.data.repository

import com.example.glucoseguard.data.dao.GlucoseDao
import com.example.glucoseguard.data.dao.InsulinDao
import com.example.glucoseguard.data.dao.MealDao
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import kotlinx.coroutines.flow.Flow

class DiabetesRepository(
    private val glucoseDao: GlucoseDao,
    private val insulinDao: InsulinDao,
    private val mealDao: MealDao
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

    suspend fun deleteAllData() {
        glucoseDao.deleteAll()
        insulinDao.deleteAll()
        mealDao.deleteAll()
    }

    fun getGlucoseInRange(startTime: Long, endTime: Long): Flow<List<GlucoseRecord>> {
        return glucoseDao.getRecordsInRange(startTime, endTime)
    }

    fun getInsulinInRange(startTime: Long, endTime: Long): Flow<List<InsulinRecord>> {
        return insulinDao.getRecordsInRange(startTime, endTime)
    }
}
