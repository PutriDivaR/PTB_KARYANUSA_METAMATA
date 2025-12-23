package com.example.karyanusa.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karyanusa.data.local.AppDatabase
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.data.repository.KaryaRepository
import com.example.karyanusa.network.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GaleriPublikViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val api = RetrofitClient.instance
    private val karyaRepository = KaryaRepository(api, database.karyaDao())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val allKaryaFlow = karyaRepository.getAllKarya()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val filteredKaryaList: StateFlow<List<KaryaEntity>> = combine(
        allKaryaFlow,
        _searchQuery
    ) { karya, query ->
        if (query.isBlank()) {
            karya
        } else {
            karya.filter {
                it.judul.contains(query, ignoreCase = true) ||
                        it.caption.contains(query, ignoreCase = true) ||
                        it.uploader_name?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var isFirstLoad = true

    fun loadAllKarya() {
        if (!isFirstLoad) {
            refreshInBackground()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                karyaRepository.syncAllKarya()
                Log.d("GaleriPublikVM", "All karya synced successfully")
                isFirstLoad = false
            } catch (e: Exception) {
                Log.e("GaleriPublikVM", "Error loading karya: ${e.message}")
                _errorMessage.value = "Gagal memuat galeri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshInBackground() {
        viewModelScope.launch {
            try {
                karyaRepository.syncAllKarya()
                Log.d("GaleriPublikVM", "Background refresh completed")
            } catch (e: Exception) {
                Log.e("GaleriPublikVM", "Background refresh failed: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun getKaryaById(galeriId: Int): KaryaEntity? {
        return database.karyaDao().getKaryaById(galeriId)
    }
}