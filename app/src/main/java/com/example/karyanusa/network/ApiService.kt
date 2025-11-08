package com.example.karyanusa.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// DATA TOKEN AKUN //
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String?,
    val user_id: String?,
    val nama: String?
)

// Data class untuk request
data class RegisterRequest(
    val nama: String,
    val email: String,
    val password: String
)

// Data class untuk response
data class RegisterResponse(
    val status: Boolean,
    val message: String,
    val user: UserData?
)

data class UserData(
    val user_id: Int,
    val nama: String,
    val email: String
)

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

    @POST("api/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @GET("api/courses")
    fun getCourses(): Call<List<Kursus>>

    @GET("api/materi/{kursus_id}")
    fun getMateriByKursus(
        @Path("kursus_id") kursusId: Int
    ): Call<List<Materi>>

}
