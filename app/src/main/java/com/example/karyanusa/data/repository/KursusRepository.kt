package com.example.karyanusa.data.repository


import com.example.karyanusa.data.local.dao.KursusDao
import com.example.karyanusa.data.local.entity.KursusEntity
import com.example.karyanusa.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KursusRepository(
    private val api: ApiService,
    private val dao: KursusDao
) {

    fun getKursus(): Flow<List<KursusEntity>> {
        return dao.getAllKursus()
    }

    suspend fun syncKursus() {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getCourses().execute()
                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        dao.insertAll(
                            list.map {
                                KursusEntity(
                                    kursus_id = it.kursus_id,
                                    judul = it.judul,
                                    deskripsi = it.deskripsi,
                                    pengrajin_nama = it.pengrajin_nama,
                                    thumbnail = it.thumbnail
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

}
