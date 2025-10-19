package com.example.karyanusa.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

data class Kursus(
    val kursus_id: Int,
    val judul: String,
    val deskripsi: String,
    val pengrajin_nama: String,
    val thumbnail: String?
)

data class Materi(
    val materi_id: Int,
    val kursus_id: Int,
    val judul: String,
    val durasi: Int,
    val video: String?
)


interface ApiService {
    @GET("api/courses")
    fun getCourses(): Call<List<Kursus>>

    @GET("api/materi/{kursus_id}")
    fun getMateriByKursus(
        @Path("kursus_id") kursusId: Int
    ): Call<List<Materi>>
}
