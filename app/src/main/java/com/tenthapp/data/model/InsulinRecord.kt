package com.example.glucoseguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insulin_records")
data class InsulinRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val dosage: Float,
    val timestamp: Long,
    val injectionSite: String = "",
    val memo: String = ""
)
