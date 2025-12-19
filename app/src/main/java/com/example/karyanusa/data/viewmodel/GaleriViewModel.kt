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

class GaleriViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val api = RetrofitClient.instance
    private val karyaRepository = KaryaRepository(api, database.karyaDao())

    // StateFlows untuk UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    // Data karya pribadi dari Room
    private val _myKaryaList = MutableStateFlow<List<KaryaEntity>>(emptyList())
    val myKaryaList: StateFlow<List<KaryaEntity>> = _myKaryaList.asStateFlow()

    // Load karya pribadi dari Room + Sync dari API
    fun loadMyKarya(token: String, userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Sync dari API
                karyaRepository.syncMyKarya(token, userId)

                // Observe dari Room Database
                karyaRepository.getMyKarya(userId)
                    .catch { e ->
                        Log.e("GaleriVM", "Error observing karya: ${e.message}")
                        _errorMessage.value = "Gagal memuat data: ${e.message}"
                    }
                    .collect { karya ->
                        _myKaryaList.value = karya
                        Log.d("GaleriVM", "My karya loaded: ${karya.size}")
                    }
            } catch (e: Exception) {
                Log.e("GaleriVM", "Error loading karya: ${e.message}")
                _errorMessage.value = "Gagal memuat galeri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Refresh data (pull to refresh)
    fun refreshKarya(token: String, userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                karyaRepository.syncMyKarya(token, userId)
                Log.d("GaleriVM", "Karya refreshed")
            } catch (e: Exception) {
                Log.e("GaleriVM", "Error refreshing: ${e.message}")
                _errorMessage.value = "Gagal refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Hapus karya (delete dari API + Room)
    fun deleteKarya(token: String, galeriId: Int) {
        viewModelScope.launch {
            _isDeleting.value = true
            _deleteSuccess.value = false
            _errorMessage.value = null

            try {
                // Delete dari API + Room
                karyaRepository.deleteKarya(token, galeriId)

                _deleteSuccess.value = true
                Log.d("GaleriVM", "Karya deleted: $galeriId")
            } catch (e: Exception) {
                Log.e("GaleriVM", "Error deleting: ${e.message}")
                _errorMessage.value = "Gagal menghapus karya: ${e.message}"
            } finally {
                _isDeleting.value = false
            }
        }
    }

    // Reset delete success state
    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }
}