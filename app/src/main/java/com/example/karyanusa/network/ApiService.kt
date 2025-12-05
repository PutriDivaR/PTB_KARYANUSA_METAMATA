package com.example.karyanusa.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


// AUTH
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String?,
    val user_id: String?,
    val nama: String?
)

data class RegisterRequest(
    val nama: String,
    val username: String,
    val password: String
)

data class RegisterResponse(
    val status: Boolean,
    val message: String,
    val user: UserData?
)

data class UserData(
    val user_id: Int,
    val nama: String,
    val username: String
)


// KURSUS & MATERI

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

// ENROLLMENT / PROGRESS

data class EnrollmentCheckResponse(
    val enrolled: Boolean,
    val status: String?,
    val progress: Int?
)

data class EnrollmentResponse(
    val message: String,
    val data: EnrollmentData?
)

data class EnrollmentData(
    val enrollment_id: Int,
    val user_id: Int,
    val kursus_id: Int,
    val progress: Int,
    val status: String
)

data class MateriCompletedResponse(
    val completed: Boolean
)

data class Notifikasi(
    val notif_id: Int,
    val from_user: Int,
    val to_user: Int,
    val type: String,
    val title: String,
    val message: String,
    val related_id: Int?,
    val is_read: Boolean,
    val created_at: String
)



// API SERVICE

interface ApiService {

    // --- Auth ---
    @POST("api/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>


    // --- Kursus ---
    @GET("api/courses")
    fun getCourses(): Call<List<Kursus>>

    @GET("api/courses/{id}")
    fun getKursusById(
        @Path("id") id: Int
    ): Call<Kursus>

    @GET("api/materi/{kursus_id}")
    fun getMateriByKursus(
        @Path("kursus_id") kursusId: Int
    ): Call<List<Materi>>


    // --- Enrollment ---
    @POST("api/enroll")
    fun enrollCourse(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Call<ResponseBody>

    @GET("api/check-enrollment/{kursus_id}")
    fun checkEnrollment(
        @Header("Authorization") token: String,
        @Path("kursus_id") kursusId: Int
    ): Call<EnrollmentCheckResponse>


    // --- Progress ---
    @POST("api/enroll/progress")
    fun updateProgress(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>
    ): Call<EnrollmentResponse>

    @GET("api/enrollments")
    fun getEnrollments(
        @Header("Authorization") token: String
    ): Call<List<EnrollmentData>>


    @POST("api/materi/complete")
    fun tandaiMateriSelesai(
        @Header("Authorization") token: String,
        @Body body: Map<String, Int>
    ): Call<ResponseBody>

    @GET("api/materi/{enrollmentId}/{materiId}/is-completed")
    fun cekMateriSelesai(
        @Header("Authorization") token: String,
        @Path("enrollmentId") enrollmentId: Int,
        @Path("materiId") materiId: Int
    ): Call<MateriCompletedResponse>

    // NOTIFIKASI
    @POST("api/notifikasi/send")
    fun sendNotification(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>


    @GET("api/notifikasi")
    fun getNotifications(
        @Header("Authorization") token: String
    ): Call<List<Notifikasi>>

    @GET("api/users/search")
    fun searchUser(
        @Query("username") username: String
    ): Call<List<UserData>>

    @GET("api/users")
    fun getAllUsers(
        @Header("Authorization") token: String
    ): Call<List<UserData>>



}
