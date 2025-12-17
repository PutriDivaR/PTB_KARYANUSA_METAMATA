package com.example.karyanusa.data.local.dao

import androidx.room.*
import com.example.karyanusa.data.local.entity.KursusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KursusDao {

    @Query("SELECT * FROM kursus")
    fun getAllKursus(): Flow<List<KursusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(kursus: List<KursusEntity>)

    @Query("DELETE FROM kursus")
    suspend fun clear()
}
