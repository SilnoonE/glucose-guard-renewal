package com.example.glucoseguard.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.glucoseguard.data.dao.GlucoseDao
import com.example.glucoseguard.data.dao.InsulinDao
import com.example.glucoseguard.data.dao.MealDao
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord

@Database(entities = [GlucoseRecord::class, InsulinRecord::class, MealRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
    abstract fun insulinDao(): InsulinDao
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diabetes_database"
                )
                // .fallbackToDestructiveMigration() // 업데이트 시 데이터 삭제 방지를 위해 주석 처리하거나 제거
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
