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
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    // Data karya pribadi dari Room (auto-update)
    private val _currentUserId = MutableStateFlow<Int?>(null)

    val myKaryaList: StateFlow<List<KaryaEntity>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                karyaRepository.getMyKarya(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load & Sync karya pribadi (hanya 1 kali)
    fun loadMyKarya(token: String, userId: Int) {
        if (_currentUserId.value == userId && !_isLoading.value) {
            // Sudah loaded, langsung refresh di background
            refreshInBackground(token, userId)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _currentUserId.value = userId
            _errorMessage.value = null

            try {
                karyaRepository.syncMyKarya(token, userId)
                Log.d("GaleriVM", "Karya synced successfully")
            } catch (e: Exception) {
                Log.e("GaleriVM", "Error loading karya: ${e.message}")
                _errorMessage.value = "Gagal memuat galeri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Refresh di background tanpa loading indicator
    private fun refreshInBackground(token: String, userId: Int) {
        viewModelScope.launch {
            try {
                karyaRepository.syncMyKarya(token, userId)
                Log.d("GaleriVM", "Background refresh completed")
            } catch (e: Exception) {
                Log.e("GaleriVM", "Background refresh failed: ${e.message}")
            }
        }
    }

    // Hapus karya (delete dari API + Room)
    fun deleteKarya(token: String, galeriId: Int, userId: Int) {
        viewModelScope.launch {
            _isDeleting.value = true
            _deleteSuccess.value = false
            _errorMessage.value = null

            try {
                // Delete dari API + Room
                karyaRepository.deleteKarya(token, galeriId)

                _deleteSuccess.value = true
                Log.d("GaleriVM", "Karya deleted: $galeriId")

                // Auto refresh setelah delete
                refreshInBackground(token, userId)
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