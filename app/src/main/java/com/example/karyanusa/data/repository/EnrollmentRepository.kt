package com.example.karyanusa.data.repository

import android.util.Log
import com.example.karyanusa.data.local.dao.EnrollmentDao
import com.example.karyanusa.data.local.entity.EnrollmentEntity
import com.example.karyanusa.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EnrollmentRepository(
    private val api: ApiService,
    private val dao: EnrollmentDao
) {

    fun getEnrollments(userId: Int): Flow<List<EnrollmentEntity>> {
        return dao.getEnrollmentsByUser(userId)
    }

    suspend fun getEnrollmentByKursus(userId: Int, kursusId: Int): EnrollmentEntity? {
        return dao.getEnrollmentByKursus(userId, kursusId)
    }

    suspend fun syncEnrollments(token: String, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getEnrollments("Bearer $token").execute()
                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        val entities = list.map {
                            EnrollmentEntity(
                                enrollment_id = it.enrollment_id,
                                user_id = it.user_id,
                                kursus_id = it.kursus_id,
                                progress = it.progress,
                                status = it.status,
                                last_updated = System.currentTimeMillis()
                            )
                        }
                        dao.insertAll(entities)
                        Log.d("EnrollmentRepo", "Synced ${entities.size} enrollments")
                    }
                } else {
                    Log.e("EnrollmentRepo", "Sync failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EnrollmentRepo", "Sync error: ${e.message}")
            }
        }
    }

    suspend fun updateProgress(token: String, kursusId: Int, progress: Int) {
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf("kursus_id" to kursusId, "progress" to progress)
                val response = api.updateProgress("Bearer $token", body).execute()

                if (response.isSuccessful) {
                    response.body()?.data?.let { enrollmentData ->
                        val entity = EnrollmentEntity(
                            enrollment_id = enrollmentData.enrollment_id,
                            user_id = enrollmentData.user_id,
                            kursus_id = enrollmentData.kursus_id,
                            progress = enrollmentData.progress,
                            status = enrollmentData.status,
                            last_updated = System.currentTimeMillis()
                        )
                        dao.insert(entity)
                    }
                }
            } catch (e: Exception) {
                Log.e("EnrollmentRepo", "Update progress error: ${e.message}")
            }
        }
    }

    suspend fun clearUserData(userId: Int) {
        dao.deleteByUser(userId)
    }
}
