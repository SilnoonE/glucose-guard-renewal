package com.example.glucoseguard.data.dao

import androidx.room.*
import com.example.glucoseguard.data.model.InsulinRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface InsulinDao {
    @Query("SELECT * FROM insulin_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<InsulinRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: InsulinRecord)

    @Update
    suspend fun update(record: InsulinRecord)

    @Delete
    suspend fun delete(record: InsulinRecord)

    @Query("DELETE FROM insulin_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM insulin_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getRecordsInRange(startTime: Long, endTime: Long): Flow<List<InsulinRecord>>
}
