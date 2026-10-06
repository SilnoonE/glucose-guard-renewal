package com.example.glucoseguard.ui.viewmodel

import androidx.lifecycle.*
import com.example.glucoseguard.data.model.GlucoseRecord
import com.example.glucoseguard.data.model.InsulinRecord
import com.example.glucoseguard.data.model.MealRecord
import com.example.glucoseguard.data.repository.DiabetesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.example.glucoseguard.util.BackupCodec

class DiabetesViewModel(private val repository: DiabetesRepository) : ViewModel() {

    val allGlucoseRecords: LiveData<List<GlucoseRecord>> = repository.allGlucoseRecords.asLiveData()
    val allInsulinRecords: LiveData<List<InsulinRecord>> = repository.allInsulinRecords.asLiveData()
    val allMealRecords: LiveData<List<MealRecord>> = repository.allMealRecords.asLiveData()

    fun insertGlucose(value: Int, category: String, memo: String = "", timestamp: Long = System.currentTimeMillis(), onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
            val record = GlucoseRecord(
                value = value,
                timestamp = timestamp,
                category = category,
                memo = memo
            )
            repository.insertGlucose(record)
            onResult(true)
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { onResult(false) }
        }
    }

    fun updateGlucose(id: Long, value: Int, category: String, memo: String, timestamp: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
            val record = GlucoseRecord(
                id = id,
                value = value,
                timestamp = timestamp,
                category = category,
                memo = memo
            )
            repository.updateGlucose(record)
            onResult(true)
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { onResult(false) }
        }
    }

    fun insertInsulin(type: String, dosage: Float, injectionSite: String = "", memo: String = "", timestamp: Long = System.currentTimeMillis(), onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
            val record = InsulinRecord(
                type = type,
                dosage = dosage,
                timestamp = timestamp,
                injectionSite = injectionSite,
                memo = memo
            )
            repository.insertInsulin(record)
            onResult(true)
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { onResult(false) }
        }
    }

    fun updateInsulin(id: Long, type: String, dosage: Float, injectionSite: String, memo: String, timestamp: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
            val record = InsulinRecord(
                id = id,
                type = type,
                dosage = dosage,
                timestamp = timestamp,
                injectionSite = injectionSite,
                memo = memo
            )
            repository.updateInsulin(record)
            onResult(true)
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) { onResult(false) }
        }
    }

    fun insertMeal(memo: String, timestamp: Long = System.currentTimeMillis(), onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try { repository.insertMeal(MealRecord(timestamp=timestamp,memo=memo));onResult(true) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { onResult(false) }
        }
    }
    fun updateMeal(id: Long, memo: String, timestamp: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try { repository.updateMeal(MealRecord(id=id,timestamp=timestamp,memo=memo));onResult(true) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { onResult(false) }
        }
    }

    fun deleteRecord(record: Any) {
        viewModelScope.launch {
            when (record) {
                is GlucoseRecord -> repository.deleteGlucose(record)
                is InsulinRecord -> repository.deleteInsulin(record)
                is MealRecord -> repository.deleteMeal(record)
            }
        }
    }

    fun importBackup(records: BackupCodec.Records, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try { onResult(Result.success(repository.importBackup(records))) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onResult(Result.failure(e)) }
        }
    }
    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}

class DiabetesViewModelFactory(private val repository: DiabetesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiabetesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiabetesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
