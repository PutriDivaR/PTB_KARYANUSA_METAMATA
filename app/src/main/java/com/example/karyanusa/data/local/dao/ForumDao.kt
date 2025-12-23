package com.example.karyanusa.data.local.dao

import androidx.room.*
import com.example.karyanusa.data.local.entity.ForumEntity

@Dao
interface ForumDao {

    @Query("SELECT * FROM forum_pertanyaan ORDER BY cached_at DESC")
    suspend fun getAllPertanyaan(): List<ForumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pertanyaan: List<ForumEntity>)

    @Query("DELETE FROM forum_pertanyaan")
    suspend fun deleteAll()

    @Query("DELETE FROM forum_pertanyaan WHERE pertanyaan_id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM forum_pertanyaan WHERE pertanyaan_id = :id")
    suspend fun getPertanyaanById(id: Int): ForumEntity?
}