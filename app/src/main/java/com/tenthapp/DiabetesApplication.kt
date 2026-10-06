package com.example.glucoseguard

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.example.glucoseguard.data.database.AppDatabase
import com.example.glucoseguard.data.repository.DiabetesRepository
import com.example.glucoseguard.util.LocaleHelper

class DiabetesApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        DiabetesRepository(
            database.glucoseDao(), 
            database.insulinDao(),
            database.mealDao(), database
        ) 
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()

    }
}
