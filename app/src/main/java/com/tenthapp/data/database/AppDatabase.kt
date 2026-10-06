package com.example.glucoseguard.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `meal_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `memo` TEXT NOT NULL)")
                // Add only missing optional columns; retain every existing reading.
                for ((table, column) in listOf("glucose_records" to "memo", "insulin_records" to "memo", "insulin_records" to "injectionSite")) {
                    val names = mutableSetOf<String>()
                    db.query("PRAGMA table_info(`$table`)").use { cursor ->
                        val index = cursor.getColumnIndexOrThrow("name")
                        while (cursor.moveToNext()) names += cursor.getString(index)
                    }
                    if (column !in names) db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diabetes_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
