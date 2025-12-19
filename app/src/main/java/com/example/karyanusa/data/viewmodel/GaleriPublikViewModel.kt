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

    // StateFlows untuk UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Data karya dari Room Database
    private val _allKaryaList = MutableStateFlow<List<KaryaEntity>>(emptyList())

    // Filtered karya berdasarkan search query
    val filteredKaryaList: StateFlow<List<KaryaEntity>> = combine(
        _allKaryaList,
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

    // Load semua karya publik dari Room + Sync dari API
    fun loadAllKarya() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Sync dari API
                karyaRepository.syncAllKarya()

                // Observe dari Room Database
                karyaRepository.getAllKarya()
                    .catch { e ->
                        Log.e("GaleriPublikVM", "Error observing karya: ${e.message}")
                        _errorMessage.value = "Gagal memuat data: ${e.message}"
                    }
                    .collect { karya ->
                        _allKaryaList.value = karya
                        Log.d("GaleriPublikVM", "All karya loaded: ${karya.size}")
                    }
            } catch (e: Exception) {
                Log.e("GaleriPublikVM", "Error loading karya: ${e.message}")
                _errorMessage.value = "Gagal memuat galeri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Refresh data (pull to refresh)
    fun refreshKarya() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                karyaRepository.syncAllKarya()
                Log.d("GaleriPublikVM", "Karya refreshed")
            } catch (e: Exception) {
                Log.e("GaleriPublikVM", "Error refreshing: ${e.message}")
                _errorMessage.value = "Gagal refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Get karya by ID (untuk detail)
    suspend fun getKaryaById(galeriId: Int): KaryaEntity? {
        return database.karyaDao().getKaryaById(galeriId)
    }
}