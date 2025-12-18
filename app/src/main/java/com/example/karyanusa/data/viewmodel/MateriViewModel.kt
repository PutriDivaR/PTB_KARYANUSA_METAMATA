package com.example.karyanusa.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karyanusa.data.local.AppDatabase
import com.example.karyanusa.data.local.entity.MateriEntity
import com.example.karyanusa.data.repository.MateriRepository
import com.example.karyanusa.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MateriViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = MateriRepository(
        RetrofitClient.instance,
        db.materiDao()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun getMateri(kursusId: Int): Flow<List<MateriEntity>> {
        return repo.getMateriByKursus(kursusId)
    }

    fun refreshMateri(kursusId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repo.syncMateri(kursusId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
