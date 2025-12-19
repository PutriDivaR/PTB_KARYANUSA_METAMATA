package com.example.karyanusa.data.local.dao

import androidx.room.*
import com.example.karyanusa.data.local.entity.KaryaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KaryaDao {

    // Sort by galeri_id DESC (ID besar = data baru)
    @Query("SELECT * FROM karya ORDER BY galeri_id DESC")
    fun getAllKarya(): Flow<List<KaryaEntity>>

    @Query("SELECT * FROM karya WHERE user_id = :userId ORDER BY galeri_id DESC")
    fun getKaryaByUser(userId: Int): Flow<List<KaryaEntity>>

    // Alternatif: Sort by created_at jika ingin pakai timestamp backend
    // @Query("SELECT * FROM karya ORDER BY created_at DESC NULLS LAST")
    // fun getAllKarya(): Flow<List<KaryaEntity>>

    @Query("SELECT * FROM karya WHERE galeri_id = :galeriId")
    suspend fun getKaryaById(galeriId: Int): KaryaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(karya: List<KaryaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(karya: KaryaEntity)

    @Update
    suspend fun update(karya: KaryaEntity)

    @Query("DELETE FROM karya WHERE galeri_id = :galeriId")
    suspend fun deleteById(galeriId: Int)

    @Query("DELETE FROM karya WHERE user_id = :userId")
    suspend fun deleteByUser(userId: Int)

    @Query("DELETE FROM karya")
    suspend fun clear()
}