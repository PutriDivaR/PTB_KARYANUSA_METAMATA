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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

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

    fun loadMyKarya(token: String, userId: Int) {
        if (_currentUserId.value == userId && !_isLoading.value) {
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

    fun deleteKarya(token: String, galeriId: Int, userId: Int) {
        viewModelScope.launch {
            _isDeleting.value = true
            _deleteSuccess.value = false
            _errorMessage.value = null

            try {
                karyaRepository.deleteKarya(token, galeriId)

                _deleteSuccess.value = true
                Log.d("GaleriVM", "Karya deleted: $galeriId")

                refreshInBackground(token, userId)
            } catch (e: Exception) {
                Log.e("GaleriVM", "Error deleting: ${e.message}")
                _errorMessage.value = "Gagal menghapus karya: ${e.message}"
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}