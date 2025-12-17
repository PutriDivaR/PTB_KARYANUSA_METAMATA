package com.example.karyanusa.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karyanusa.data.local.AppDatabase
import com.example.karyanusa.data.repository.KursusRepository
import com.example.karyanusa.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KursusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = KursusRepository(
        RetrofitClient.instance,
        db.kursusDao()
    )

    val kursus = repo.getKursus()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val isLoading = MutableStateFlow(false)

    init {
        refreshIfOnline()
    }

    private fun refreshIfOnline() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repo.syncKursus()
            } catch (e: Exception) {

            } finally {
                isLoading.value = false
            }
        }
    }
}

