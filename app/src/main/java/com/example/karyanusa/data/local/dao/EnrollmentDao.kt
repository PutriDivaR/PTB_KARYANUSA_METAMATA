package com.example.karyanusa.data.local.dao

import androidx.room.*
import com.example.karyanusa.data.local.entity.EnrollmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnrollmentDao {

    @Query("SELECT * FROM enrollment WHERE user_id = :userId ORDER BY enrollment_id DESC")
    fun getEnrollmentsByUser(userId: Int): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollment WHERE user_id = :userId AND kursus_id = :kursusId")
    suspend fun getEnrollmentByKursus(userId: Int, kursusId: Int): EnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(enrollments: List<EnrollmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(enrollment: EnrollmentEntity)

    @Update
    suspend fun update(enrollment: EnrollmentEntity)

    @Query("DELETE FROM enrollment WHERE user_id = :userId")
    suspend fun deleteByUser(userId: Int)

    @Query("DELETE FROM enrollment")
    suspend fun clear()
}