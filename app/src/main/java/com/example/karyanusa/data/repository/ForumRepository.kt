package com.example.karyanusa.data.repository

import android.util.Log
import com.example.karyanusa.data.local.dao.ForumDao
import com.example.karyanusa.data.local.entity.ForumEntity
import com.example.karyanusa.network.ApiService
import com.example.karyanusa.network.ForumPertanyaanResponse
import com.example.karyanusa.network.SimpleResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ForumRepository(
    private val apiService: ApiService,
    private val forumDao: ForumDao
) {
    private val gson = Gson()
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 menit

    fun getPertanyaan(token: String): Flow<Result<List<ForumPertanyaanResponse>>> = flow {
        try {
            val cachedData = forumDao.getAllPertanyaan()
            val currentTime = System.currentTimeMillis()

            if (cachedData.isNotEmpty() &&
                (currentTime - cachedData.first().cached_at) < CACHE_DURATION) {
                Log.d("ForumRepository", "Using cached data")
                emit(Result.success(cachedData.map { it.toResponse() }))
            } else {
                Log.d("ForumRepository", "Fetching from API")
                val apiResponse = fetchFromApi(token)

                saveToCache(apiResponse)

                emit(Result.success(apiResponse))
            }
        } catch (e: Exception) {
            Log.e("ForumRepository", "Error: ${e.message}")

            try {
                val staleCache = forumDao.getAllPertanyaan()
                if (staleCache.isNotEmpty()) {
                    Log.d("ForumRepository", "Using stale cache as fallback")
                    emit(Result.success(staleCache.map { it.toResponse() }))
                } else {
                    emit(Result.failure(e))
                }
            } catch (cacheError: Exception) {
                emit(Result.failure(e))
            }
        }
    }.flowOn(Dispatchers.IO)


    private suspend fun fetchFromApi(token: String): List<ForumPertanyaanResponse> {
        return suspendCoroutine { continuation ->
            apiService.getPertanyaan("Bearer $token").enqueue(
                object : Callback<List<ForumPertanyaanResponse>> {
                    override fun onResponse(
                        call: Call<List<ForumPertanyaanResponse>>,
                        response: Response<List<ForumPertanyaanResponse>>
                    ) {
                        if (response.isSuccessful) {
                            val body: List<ForumPertanyaanResponse> = response.body() ?: emptyList()
                            continuation.resume(body)
                        } else {
                            continuation.resumeWithException(
                                Exception("API Error: ${response.code()}")
                            )
                        }
                    }

                    override fun onFailure(call: Call<List<ForumPertanyaanResponse>>, t: Throwable) {
                        continuation.resumeWithException(t)
                    }
                }
            )
        }
    }

    private suspend fun saveToCache(data: List<ForumPertanyaanResponse>) {
        withContext(Dispatchers.IO) {
            val entities = data.map { it.toEntity() }
            forumDao.deleteAll() // Clear old cache
            forumDao.insertAll(entities)
            Log.d("ForumRepository", "Saved ${entities.size} items to cache")
        }
    }


    suspend fun refreshPertanyaan(token: String): Result<List<ForumPertanyaanResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val apiResponse = fetchFromApi(token)
                saveToCache(apiResponse)
                Result.success(apiResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun deletePertanyaan(token: String, id: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = suspendCoroutine<SimpleResponse> { continuation ->
                    apiService.deletePertanyaan("Bearer $token", id).enqueue(
                        object : Callback<SimpleResponse> {
                            override fun onResponse(
                                call: Call<SimpleResponse>,
                                response: Response<SimpleResponse>
                            ) {
                                if (response.isSuccessful && response.body() != null) {
                                    continuation.resume(response.body()!!)
                                } else {
                                    continuation.resumeWithException(
                                        Exception("Delete failed: ${response.code()}")
                                    )
                                }
                            }

                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                continuation.resumeWithException(t)
                            }
                        }
                    )
                }

                forumDao.deleteById(id)

                Result.success(response.message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            forumDao.deleteAll()
        }
    }


    private fun ForumPertanyaanResponse.toEntity(): ForumEntity {
        val imageListJson: String = gson.toJson(this.image_forum ?: emptyList<String>())

        return ForumEntity(
            pertanyaan_id = this.pertanyaan_id,
            user_id = this.user_id,
            isi = this.isi,
            tanggal = this.tanggal,
            updated_at = this.updated_at,
            image_forum = imageListJson,
            jawaban_count = this.jawaban?.size ?: 0,
            user_nama = this.user?.nama,
            user_username = this.user?.username,
            user_foto_profile = this.user?.foto_profile,
            cached_at = System.currentTimeMillis()
        )
    }


    private fun ForumEntity.toResponse(): ForumPertanyaanResponse {
        val imageList: List<String>? = try {
            val array = gson.fromJson(this.image_forum, Array<String>::class.java)
            array?.toList()
        } catch (e: Exception) {
            emptyList()
        }

        return ForumPertanyaanResponse(
            pertanyaan_id = this.pertanyaan_id,
            user_id = this.user_id,
            isi = this.isi,
            tanggal = this.tanggal,
            updated_at = this.updated_at,
            image_forum = imageList,
            jawaban = null,
            user = if (this.user_nama != null) {
                com.example.karyanusa.network.UserData(
                    user_id = this.user_id,
                    nama = this.user_nama,
                    username = this.user_username ?: "",
                    foto_profile = this.user_foto_profile
                )
            } else null
        )
    }
}