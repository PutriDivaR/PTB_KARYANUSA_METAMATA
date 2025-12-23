package com.example.karyanusa.data.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karyanusa.data.local.AppDatabase
import com.example.karyanusa.data.repository.ForumRepository
import com.example.karyanusa.network.ForumPertanyaanResponse
import com.example.karyanusa.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ForumViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ForumViewModel"

    private val db = AppDatabase.getInstance(application)
    private val repo = ForumRepository(
        RetrofitClient.instance,
        db.ForumDao()
    )

    private val _uiState = MutableStateFlow<ForumUiState>(ForumUiState.Loading)
    val uiState: StateFlow<ForumUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState

    private var cachedQuestions: List<ForumPertanyaanResponse> = emptyList()

    fun loadPertanyaan(token: String) {
        viewModelScope.launch {
            _uiState.value = ForumUiState.Loading

            repo.getPertanyaan(token).collect { result ->
                result.fold(
                    onSuccess = { questions ->
                        // ✅ DEBUG: Log jumlah pertanyaan dan status jawaban
                        Log.d(TAG, "Total pertanyaan: ${questions.size}")
                        questions.forEachIndexed { index, q ->
                            Log.d(TAG, "Pertanyaan #$index:")
                            Log.d(TAG, "  - ID: ${q.pertanyaan_id}")
                            Log.d(TAG, "  - Isi: ${q.isi.take(50)}...")
                            Log.d(TAG, "  - Jawaban: ${q.jawaban}")
                            Log.d(TAG, "  - Jawaban Size: ${q.jawaban?.size}")
                            Log.d(TAG, "  - Is Answered: ${isAnswered(q)}")
                        }

                        cachedQuestions = sortByNewest(questions)
                        _uiState.value = ForumUiState.Success(cachedQuestions)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error loading pertanyaan: ${error.message}")
                        _uiState.value = ForumUiState.Error(
                            error.message ?: "Terjadi kesalahan"
                        )
                    }
                )
            }
        }
    }

    fun refreshPertanyaan(token: String) {
        viewModelScope.launch {
            _isRefreshing.value = true

            val result = repo.refreshPertanyaan(token)

            result.fold(
                onSuccess = { questions ->
                    cachedQuestions = sortByNewest(questions)
                    _uiState.value = ForumUiState.Success(cachedQuestions)
                },
                onFailure = { error ->
                    if (cachedQuestions.isNotEmpty()) {
                        _uiState.value = ForumUiState.Success(cachedQuestions)
                    } else {
                        _uiState.value = ForumUiState.Error(
                            error.message ?: "Gagal memuat data"
                        )
                    }
                }
            )

            _isRefreshing.value = false
        }
    }

    fun deletePertanyaan(token: String, id: Int) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting

            val result = repo.deletePertanyaan(token, id)

            result.fold(
                onSuccess = { message ->
                    _deleteState.value = DeleteState.Success(message)

                    if (_uiState.value is ForumUiState.Success) {
                        val currentQuestions = (_uiState.value as ForumUiState.Success).pertanyaan
                        val updatedQuestions = currentQuestions.filter { it.pertanyaan_id != id }
                        cachedQuestions = updatedQuestions
                        _uiState.value = ForumUiState.Success(updatedQuestions)
                    }
                },
                onFailure = { error ->
                    _deleteState.value = DeleteState.Error(
                        error.message ?: "Gagal menghapus pertanyaan"
                    )
                }
            )
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    // ✅ PERBAIKAN: Fungsi untuk cek apakah pertanyaan sudah dijawab
    // Tambahkan berbagai cara pengecekan untuk menangani berbagai struktur data
    private fun isAnswered(question: ForumPertanyaanResponse): Boolean {
        // Cek 1: Apakah jawaban tidak null dan tidak kosong
        val hasAnswers = question.jawaban?.isNotEmpty() == true

        // Cek 2: Apakah ada field jumlah_jawaban (beberapa API menggunakan ini)
        // Uncomment jika API Anda menggunakan field ini
        // val hasJumlahJawaban = (question.jumlah_jawaban ?: 0) > 0

        Log.d(TAG, "isAnswered for pertanyaan ${question.pertanyaan_id}: $hasAnswers (jawaban=${question.jawaban})")

        return hasAnswers
    }

    // ✅ Fungsi untuk mengurutkan berdasarkan tanggal terbaru
    private fun sortByNewest(questions: List<ForumPertanyaanResponse>): List<ForumPertanyaanResponse> {
        return questions.sortedByDescending { question ->
            try {
                val dateString = if (!question.updated_at.isNullOrEmpty()) {
                    question.updated_at
                } else {
                    question.tanggal
                }

                val cleanedDate = dateString.replace(Regex("\\.\\d+Z"), "Z")
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val time = format.parse(cleanedDate)?.time ?: 0L

                Log.d(TAG, "Sort: pertanyaan ${question.pertanyaan_id} = $time")
                time
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date for pertanyaan ${question.pertanyaan_id}: ${e.message}")
                0L
            }
        }
    }

    fun getFilteredQuestions(
        tab: String,
        filter: String,
        currentUserId: Int?
    ): List<ForumPertanyaanResponse> {
        val questions = when (val state = _uiState.value) {
            is ForumUiState.Success -> state.pertanyaan
            else -> emptyList()
        }

        Log.d(TAG, "getFilteredQuestions - tab: $tab, filter: $filter, userId: $currentUserId")
        Log.d(TAG, "Total questions before filter: ${questions.size}")

        // Filter berdasarkan tab
        val tabFiltered = when (tab) {
            "my" -> questions.filter { it.user_id == currentUserId }
            else -> questions
        }

        Log.d(TAG, "After tab filter: ${tabFiltered.size}")

        // Filter berdasarkan status jawaban
        val result = when (filter) {
            "answered" -> {
                val filtered = tabFiltered.filter { isAnswered(it) }
                Log.d(TAG, "Answered filter result: ${filtered.size}")
                filtered
            }
            "unanswered" -> {
                val filtered = tabFiltered.filter { !isAnswered(it) }
                Log.d(TAG, "Unanswered filter result: ${filtered.size}")
                filtered
            }
            else -> {
                Log.d(TAG, "All filter result: ${tabFiltered.size}")
                tabFiltered
            }
        }

        return result
    }

    fun getFilterCounts(tab: String, currentUserId: Int?): FilterCounts {
        val questions = when (val state = _uiState.value) {
            is ForumUiState.Success -> state.pertanyaan
            else -> emptyList()
        }

        val tabFiltered = when (tab) {
            "my" -> questions.filter { it.user_id == currentUserId }
            else -> questions
        }

        val counts = FilterCounts(
            all = tabFiltered.size,
            answered = tabFiltered.count { isAnswered(it) },
            unanswered = tabFiltered.count { !isAnswered(it) }
        )

        Log.d(TAG, "Filter counts - all: ${counts.all}, answered: ${counts.answered}, unanswered: ${counts.unanswered}")

        return counts
    }

    fun clearCache() {
        viewModelScope.launch {
            repo.clearCache()
            cachedQuestions = emptyList()
        }
    }
}

sealed class ForumUiState {
    object Loading : ForumUiState()
    data class Success(val pertanyaan: List<ForumPertanyaanResponse>) : ForumUiState()
    data class Error(val message: String) : ForumUiState()
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Deleting : DeleteState()
    data class Success(val message: String) : DeleteState()
    data class Error(val message: String) : DeleteState()
}

data class FilterCounts(
    val all: Int = 0,
    val answered: Int = 0,
    val unanswered: Int = 0
)