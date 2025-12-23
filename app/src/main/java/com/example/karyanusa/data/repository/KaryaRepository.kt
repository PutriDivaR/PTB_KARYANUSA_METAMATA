package com.example.karyanusa.data.repository

import android.util.Log
import com.example.karyanusa.data.local.dao.KaryaDao
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KaryaRepository(
    private val api: ApiService,
    private val dao: KaryaDao
) {

    fun getAllKarya(): Flow<List<KaryaEntity>> {
        return dao.getAllKarya()
    }

    fun getMyKarya(userId: Int): Flow<List<KaryaEntity>> {
        return dao.getKaryaByUser(userId)
    }

    suspend fun syncMyKarya(token: String, userId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMyKarya(token).execute()
                if (response.isSuccessful && response.body()?.status == true) {
                    response.body()?.data?.let { list ->
                        val entities = list.map {
                            KaryaEntity(
                                galeri_id = it.galeri_id,
                                user_id = it.user_id,
                                judul = it.judul,
                                caption = it.caption,
                                gambar = it.gambar,
                                tanggal_upload = it.tanggal_upload,
                                created_at = it.created_at,
                                updated_at = it.updated_at,
                                views = it.views,
                                likes = it.likes,
                                uploader_name = it.uploader_name,
                                last_updated = System.currentTimeMillis()
                            )
                        }
                        dao.insertAll(entities)
                        Log.d("KaryaRepo", "Synced ${entities.size} karya")
                    }
                } else {
                    Log.e("KaryaRepo", "Sync failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KaryaRepo", "Sync error: ${e.message}")
            }
        }
    }

    suspend fun syncAllKarya() {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getKarya().execute()
                if (response.isSuccessful && response.body()?.status == true) {
                    response.body()?.data?.let { list ->
                        val entities = list.map {
                            KaryaEntity(
                                galeri_id = it.galeri_id,
                                user_id = it.user_id,
                                judul = it.judul,
                                caption = it.caption,
                                gambar = it.gambar,
                                tanggal_upload = it.tanggal_upload,
                                created_at = it.created_at,
                                updated_at = it.updated_at,
                                views = it.views,
                                likes = it.likes,
                                uploader_name = it.uploader_name,
                                last_updated = System.currentTimeMillis()
                            )
                        }
                        dao.insertAll(entities)
                        Log.d("KaryaRepo", "Synced ${entities.size} public karya")
                    }
                } else {
                    Log.e("KaryaRepo", "Sync all karya failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KaryaRepo", "Sync all karya error: ${e.message}")
                throw e
            }
        }
    }

    suspend fun deleteKarya(token: String, galeriId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.deleteKarya(token, galeriId).execute()
                if (response.isSuccessful) {
                    dao.deleteById(galeriId)
                    Log.d("KaryaRepo", "Karya deleted successfully: $galeriId")
                } else {
                    Log.e("KaryaRepo", "Delete failed: ${response.code()}")
                    throw Exception("Gagal menghapus dari server: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("KaryaRepo", "Delete error: ${e.message}")
                throw e
            }
        }
    }

    suspend fun clearUserData(userId: Int) {
        dao.deleteByUser(userId)
    }
}