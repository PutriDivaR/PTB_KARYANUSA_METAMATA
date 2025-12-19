package com.example.karyanusa.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
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

data class UploadResponse(
    val status: Boolean,
    val message: String,
    val file_url: String?
)

data class ViewResponse(
    val status: Boolean,
    val message: String,
    val views: Int
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
    val views: Int = 0,
    val likes: Int = 0,
    val uploader_name: String?,
)


data class SimpleResponse(
    val status: Boolean,
    val message: String
)

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

data class ForumPertanyaanResponse(
    val pertanyaan_id: Int,
    val user_id: Int,
    val image_forum: String?,
    val isi: String,
    val tanggal: String,
    val user: UserData?,
    val jawaban: List<ForumJawabanResponse>?
)

data class ForumJawabanResponse(
    val jawaban_id: Int,
    val user_id: Int,
    val pertanyaan_id: Int,
    val image_jawaban: String?,
    val isi: String,
    val tanggal: String,
    val user: UserData?
)

// Notifikasi Class
data class Notifikasi(
    val notif_id: Int,
    val from_user: Int,
    val to_user: Int,
    val type: String,
    val title: String,
    val message: String,
    val related_id: Int?,
    val is_read: Int,
    val created_at: String
)

data class LikeResponse(
    val status: Boolean,
    val action: String, // "liked" atau "unliked"
    val message: String,
    val likes: Int,
    val is_liked: Boolean
)

data class LikeCheckResponse(
    val status: Boolean,
    val is_liked: Boolean,
    val likes: Int
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


    // ✅ Upload Karya (tambah token)
    @Multipart
    @POST("api/karya/upload")
    fun uploadKarya(
        @Header("Authorization") token: String,
        @Part gambar: MultipartBody.Part?,
        @Part("nama") nama: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody
    ): Call<UploadResponse>

    // ✅ Get Semua Karya (publik)
    @GET("api/karya")
    fun getKarya(): Call<KaryaResponse>

    // ✅ Get Karya Pribadi (butuh token)
    @GET("api/karya/my")
    fun getMyKarya(
        @Header("Authorization") token: String
    ): Call<KaryaResponse>

    // ✅ Delete Karya (butuh token)
    @DELETE("api/karya/{id}")

    fun deleteKarya(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<SimpleResponse>

    // ✅ Update Karya (tambah token)
    @Multipart
    @POST("api/karya/update/{id}")
    fun updateKarya(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Call<SimpleResponse>

    @POST("api/karya/{id}/view")
    fun incrementView(@Path("id") id: Int): Call<ViewResponse>

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

    @DELETE("api/delete-enrollment/{kursus_id}")
    fun cancelEnrollment(
        @Header("Authorization") token: String,
        @Path("kursus_id") kursusId: Int
    ): Call<ResponseBody>



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


    // Ambil semua pertanyaan forum
    @GET("api/pertanyaan")
    fun getPertanyaan(
        @Header("Authorization") token: String
    ): Call<List<ForumPertanyaanResponse>>

    @GET("api/pertanyaan/{id}")
    fun getPertanyaanDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<ForumPertanyaanResponse>

    @Multipart
    @POST("api/pertanyaan")
    fun tambahPertanyaan(
        @Header("Authorization") token: String,
        @Part("isi") isi: RequestBody,
        @Part image_forum: MultipartBody.Part? = null
    ): Call<ForumPertanyaanResponse>

    // Tambah jawaban
    @Multipart
    @POST("api/pertanyaan/{id}/jawaban")
    fun tambahJawaban(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("isi") isi: RequestBody,
        @Part image_jawaban: MultipartBody.Part? = null
    ): Call<ForumJawabanResponse>

    @GET("profile/{id}")
    fun getProfile(
        @Header("Authorization") token: String,  // Sudah format "Bearer xxx"
        @Path("id") userId: Int
    ): Call<UserData>

    @PUT("profile/{id}")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body body: Map<String, String>
    ): Call<UserData>

    @POST("api/notifikasi/send")
    fun sendNotification(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<ResponseBody>

    @GET("api/users")
    fun getAllUsers(
        @Header("Authorization") token: String,
    ): Call<List<UserData>>

    // ✅ Update Pertanyaan
    @Multipart
    @POST("api/pertanyaan/{id}/update")
    fun updatePertanyaan(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Part("isi") isi: RequestBody,
        @Part image_forum: MultipartBody.Part? = null
    ): Call<ForumPertanyaanResponse>

    // ✅ Delete Pertanyaan
    @DELETE("api/pertanyaan/{id}")
    fun deletePertanyaan(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<SimpleResponse>

    @GET("api/notifikasi")
    fun getNotifications(
        @Header("Authorization") token: String
    ): Call<List<Notifikasi>>

    @GET("api/users/search")
    fun searchUser(
        @Query("username") username: String
    ): Call<List<UserData>>

    @POST("api/users/fcm-token")
    fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Call<ResponseBody>

    @POST("api/notifikasi/read/{id}")
    fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("id") notifId: Int
    ): Call<ResponseBody>


    @POST("api/karya/{galeri_id}/like")
    fun toggleLike(
        @Header("Authorization") token: String,
        @Path("galeri_id") galeriId: Int
    ): Call<LikeResponse>

    @GET("api/karya/{galeri_id}/check-like")
    fun checkLike(
        @Header("Authorization") token: String,
        @Path("galeri_id") galeriId: Int
    ): Call<LikeCheckResponse>


    @POST("api/notifikasi/{id}/read")
    fun markNotifRead(
        @Header("Authorization") token: String,
        @Path("id") notifId: Int
    ): Call<ResponseBody>
}

