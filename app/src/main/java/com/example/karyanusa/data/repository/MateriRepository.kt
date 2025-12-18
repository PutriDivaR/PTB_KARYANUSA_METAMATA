package com.example.karyanusa.data.repository

import com.example.karyanusa.data.local.dao.MateriDao
import com.example.karyanusa.data.local.entity.MateriEntity
import com.example.karyanusa.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MateriRepository(
    private val api: ApiService,
    private val dao: MateriDao
) {

    fun getMateriByKursus(kursusId: Int): Flow<List<MateriEntity>> {
        return dao.getMateriByKursus(kursusId)
    }

    suspend fun syncMateri(kursusId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMateriByKursus(kursusId).execute()
                if (response.isSuccessful) {
                    response.body()?.let { list ->
                        dao.insertAll(
                            list.map {
                                MateriEntity(
                                    materi_id = it.materi_id,
                                    kursus_id = it.kursus_id,
                                    judul = it.judul,
                                    durasi = it.durasi,
                                    video = it.video
                                )
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
