package com.example.karyanusa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.karyanusa.data.local.entity.MateriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriDao {
    @Query("SELECT * FROM materi WHERE kursus_id = :kursusId")
    fun getMateriByKursus(kursusId: Int): Flow<List<MateriEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(materi: List<MateriEntity>)

    @Query("DELETE FROM materi WHERE kursus_id = :kursusId")
    suspend fun deleteByKursusId(kursusId: Int)
}
