package com.example.glucoseguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_records")
data class GlucoseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: Int,
    val timestamp: Long,
    val category: String,
    val memo: String = ""
)
