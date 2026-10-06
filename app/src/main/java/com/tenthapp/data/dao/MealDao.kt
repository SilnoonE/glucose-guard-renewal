package com.example.glucoseguard.data.dao

import androidx.room.*
import com.example.glucoseguard.data.model.MealRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<MealRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MealRecord)

    @Update
    suspend fun update(record: MealRecord)

    @Delete
    suspend fun delete(record: MealRecord)

    @Query("DELETE FROM meal_records")
    suspend fun deleteAll()
}
