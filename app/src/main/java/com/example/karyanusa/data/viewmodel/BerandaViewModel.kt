package com.example.karyanusa.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karyanusa.data.local.AppDatabase
import com.example.karyanusa.data.local.entity.EnrollmentEntity
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.data.local.entity.KursusEntity
import com.example.karyanusa.data.repository.EnrollmentRepository
import com.example.karyanusa.data.repository.KaryaRepository
import com.example.karyanusa.data.repository.KursusRepository
import com.example.karyanusa.network.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BerandaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val api = RetrofitClient.instance
    private val kursusRepository = KursusRepository(api, database.kursusDao())
    private val enrollmentRepository = EnrollmentRepository(api, database.enrollmentDao())
    private val karyaRepository = KaryaRepository(api, database.karyaDao())
    private val _isLoadingKursus = MutableStateFlow(false)
    val isLoadingKursus: StateFlow<Boolean> = _isLoadingKursus.asStateFlow()
    private val _isLoadingEnrollments = MutableStateFlow(false)
    val isLoadingEnrollments: StateFlow<Boolean> = _isLoadingEnrollments.asStateFlow()
    private val _isLoadingKarya = MutableStateFlow(false)
    val isLoadingKarya: StateFlow<Boolean> = _isLoadingKarya.asStateFlow()

    val kursusList: StateFlow<List<KursusEntity>> = kursusRepository.getKursus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _enrollmentsList = MutableStateFlow<List<EnrollmentEntity>>(emptyList())
    val enrollmentsList: StateFlow<List<EnrollmentEntity>> = _enrollmentsList.asStateFlow()

    private val _myKaryaList = MutableStateFlow<List<KaryaEntity>>(emptyList())
    val myKaryaList: StateFlow<List<KaryaEntity>> = _myKaryaList.asStateFlow()

    fun syncKursus() {
        viewModelScope.launch {
            _isLoadingKursus.value = true
            try {
                kursusRepository.syncKursus()
                Log.d("BerandaVM", "Kursus synced successfully")
            } catch (e: Exception) {
                Log.e("BerandaVM", "Error syncing kursus: ${e.message}")
            } finally {
                _isLoadingKursus.value = false
            }
        }
    }

    fun syncEnrollments(token: String, userId: Int) {
        viewModelScope.launch {
            _isLoadingEnrollments.value = true
            try {
                enrollmentRepository.syncEnrollments(token, userId)
                enrollmentRepository.getEnrollments(userId)
                    .take(1)
                    .collect { enrollments ->
                        _enrollmentsList.value = enrollments
                        Log.d("BerandaVM", "Enrollments updated: ${enrollments.size}")
                    }
            } catch (e: Exception) {
                Log.e("BerandaVM", "Error syncing enrollments: ${e.message}")
                _enrollmentsList.value = emptyList()
            } finally {
                _isLoadingEnrollments.value = false
            }
        }
    }

    fun syncMyKarya(token: String, userId: Int) {
        viewModelScope.launch {
            _isLoadingKarya.value = true
            try {
                karyaRepository.syncMyKarya(token, userId)
                karyaRepository.getMyKarya(userId)
                    .take(1)
                    .collect { karya ->
                        _myKaryaList.value = karya
                        Log.d("BerandaVM", "My karya updated: ${karya.size}")
                    }
            } catch (e: Exception) {
                Log.e("BerandaVM", "Error syncing karya: ${e.message}")
                _myKaryaList.value = emptyList()
            } finally {
                _isLoadingKarya.value = false
            }
        }
    }
    fun loadAllData(token: String?, userId: Int?) {
        syncKursus()

        if (token != null && userId != null) {
            syncEnrollments(token, userId)
            syncMyKarya(token, userId)
        }
    }
    fun refreshAllData(token: String?, userId: Int?) {
        viewModelScope.launch {
            _isLoadingKursus.value = true
            _isLoadingEnrollments.value = true
            _isLoadingKarya.value = true

            loadAllData(token, userId)
        }
    }
}