package com.example.glucoseguard.data.dao

import androidx.room.*
import com.example.glucoseguard.data.model.GlucoseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<GlucoseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GlucoseRecord)

    @Update
    suspend fun update(record: GlucoseRecord)

    @Delete
    suspend fun delete(record: GlucoseRecord)

    @Query("DELETE FROM glucose_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM glucose_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getRecordsInRange(startTime: Long, endTime: Long): Flow<List<GlucoseRecord>>
    
    @Query("SELECT * FROM glucose_records WHERE id = :id")
    suspend fun getRecordById(id: Long): GlucoseRecord?
}
