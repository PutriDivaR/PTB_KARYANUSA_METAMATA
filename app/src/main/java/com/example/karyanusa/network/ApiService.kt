package com.example.karyanusa.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
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

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val file_url: String?
)

data class KaryaResponse(
    val status: Boolean,
    val data: List<KaryaData>
)

data class KaryaData(
    val galeri_id: Int,
    val user_id: Int,
    val judul: String,
    val caption: String,
    val gambar: String,
    val tanggal_upload: String?,
    val created_at: String?,
    val updated_at: String?,
    val uploader_name: String?
)

data class SimpleResponse(
    val status: Boolean,
    val message: String
)

data class NotifikasiData(
    val judul: String,
    val pesan: String,
    val waktu: String
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

    @Multipart
    @POST("api/karya/upload")
    fun uploadKarya(
        @Part gambar: MultipartBody.Part?,
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody
    ): Call<UploadResponse>

    @GET("api/karya")
    fun getKarya(): Call<KaryaResponse>

    @GET("api/karya/my")
    fun getMyKarya(): Call<KaryaResponse>

    @DELETE("api/karya/{id}")
    fun deleteKarya(
        @Path("id") id: Int
    ): Call<SimpleResponse>

    @Multipart
    @POST("api/karya/update/{id}")
    fun updateKarya(
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Call<SimpleResponse>

}
