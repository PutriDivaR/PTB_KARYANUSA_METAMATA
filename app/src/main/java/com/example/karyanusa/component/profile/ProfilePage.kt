package com.example.karyanusa.component.profile

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.karyanusa.R
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.component.forum.ImageUtils
import com.example.karyanusa.network.Kursus
import com.example.karyanusa.network.RetrofitClient
import android.util.Log
import android.content.Context
import com.example.karyanusa.network.EnrollmentData
import com.example.karyanusa.network.ProfileResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }

    // === STATE ===
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    var nama by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("—") }

    var originalNama by remember { mutableStateOf("") }
    var originalBio by remember { mutableStateOf("—") }

    // 🔥 STATE FOTO PROFIL
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var originalPhotoUrl by remember { mutableStateOf<String?>(null) }

    var showPhotoOptions by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔥 STATE UNTUK KURSUS USER
    var userCourses by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var isLoadingCourses by remember { mutableStateOf(false) }

    // ============================================================
    // 🔥 LOAD PROFILE FROM CACHE (Fallback)
    // ============================================================
    fun loadProfileFromCache() {
        try {
            val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
            val cachedName = prefs.getString("user_name", null)
            val cachedUsername = prefs.getString("user_username", null)
            val cachedBio = prefs.getString("user_bio", "—")
            val cachedPhoto = prefs.getString("user_foto_profile", null)

            if (!cachedName.isNullOrEmpty()) {
                nama = cachedName
                username = cachedUsername ?: ""
                bio = cachedBio ?: "—"
                profilePhotoUrl = cachedPhoto

                originalNama = nama
                originalBio = bio
                originalPhotoUrl = profilePhotoUrl

                Log.d("ProfilePage", "✅ Profile loaded from cache")
            }
        } catch (e: Exception) {
            Log.e("ProfilePage", "❌ Error loading from cache", e)
        }
    }

    // ============================================================
    // 🔥 LOAD PROFILE - Ambil dari Backend API
    // ============================================================
    fun loadProfile() {
        isLoading = true
        errorMessage = null

        val token = tokenManager.getToken()
        val userId = tokenManager.getUserId()

        if (token.isNullOrEmpty() || userId == null) {
            Log.e("ProfilePage", "Token atau userId tidak tersedia")
            errorMessage = "Sesi tidak valid"
            isLoading = false
            return
        }

        Log.d("ProfilePage", "🔄 Loading profile from API...")

        RetrofitClient.instance.getProfile("Bearer $token", userId)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    isLoading = false

                    if (response.isSuccessful) {
                        val profileData = response.body()?.data

                        if (profileData != null) {
                            nama = profileData.nama
                            username = profileData.username
                            bio = profileData.bio ?: "—"
                            profilePhotoUrl = profileData.foto_profile // ✅ Load foto dari server

                            originalNama = nama
                            originalBio = bio
                            originalPhotoUrl = profilePhotoUrl

                            // 🔥 Simpan ke SharedPreferences sebagai cache
                            val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("user_name", nama)
                                putString("user_username", username)
                                putString("user_bio", bio)
                                putString("user_foto_profile", profilePhotoUrl) // ✅ Cache foto
                                apply()
                            }

                            Log.d("ProfilePage", "✅ Profile loaded - Nama: $nama, Bio: $bio, Foto: $profilePhotoUrl")
                        } else {
                            errorMessage = "Data profil kosong"
                            Log.e("ProfilePage", "❌ Profile data is null")
                        }
                    } else {
                        errorMessage = "Gagal memuat profil: ${response.code()}"
                        Log.e("ProfilePage", "❌ API Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Koneksi gagal: ${t.message}"
                    Log.e("ProfilePage", "❌ Network error loading profile", t)

                    // Fallback ke SharedPreferences jika offline
                    loadProfileFromCache()
                }
            })
    }

    // ============================================================
    // 🔥 UPLOAD FOTO PROFIL KE SERVER
    // ============================================================
    fun uploadProfilePhoto(uri: Uri) {
        val token = tokenManager.getToken()
        val userId = tokenManager.getUserId()

        if (token.isNullOrEmpty() || userId == null) {
            Toast.makeText(context, "Sesi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        isUploadingPhoto = true
        Log.d("ProfilePage", "🔄 Uploading profile photo...")

        try {
            // Baca file dari URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Buat MultipartBody
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("foto_profile", file.name, requestFile)

            // Upload ke server
            RetrofitClient.instance.uploadProfilePhoto("Bearer $token", userId, body)
                .enqueue(object : Callback<ProfileResponse> {
                    override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                        isUploadingPhoto = false

                        if (response.isSuccessful) {
                            val profileData = response.body()?.data
                            if (profileData != null) {
                                profilePhotoUrl = profileData.foto_profile
                                originalPhotoUrl = profilePhotoUrl

                                // Update cache
                                val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
                                prefs.edit().putString("user_foto_profile", profilePhotoUrl).apply()

                                Toast.makeText(context, "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                Log.d("ProfilePage", "✅ Photo uploaded: $profilePhotoUrl")
                            }
                        } else {
                            Toast.makeText(context, "Gagal upload foto: ${response.code()}", Toast.LENGTH_SHORT).show()
                            Log.e("ProfilePage", "❌ Upload error: ${response.code()}")

                            // Reset preview jika gagal
                            profileImageUri = null
                        }

                        // Hapus file temporary
                        file.delete()
                    }

                    override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                        isUploadingPhoto = false
                        Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("ProfilePage", "❌ Network error uploading photo", t)

                        // Reset preview jika gagal
                        profileImageUri = null

                        // Hapus file temporary
                        file.delete()
                    }
                })

        } catch (e: Exception) {
            isUploadingPhoto = false
            Log.e("ProfilePage", "❌ Error preparing photo", e)
            Toast.makeText(context, "Gagal memproses foto", Toast.LENGTH_SHORT).show()
            profileImageUri = null
        }
    }

    // ============================================================
    // 🔥 LOAD KURSUS USER
    // ============================================================
    fun loadUserCourses() {
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Log.d("ProfilePage", "Token kosong, skip load courses")
            return
        }

        isLoadingCourses = true
        Log.d("ProfilePage", "🔄 Loading user enrollments...")

        RetrofitClient.instance.getCourses()
            .enqueue(object : Callback<List<Kursus>> {
                override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                    val allCourses = response.body() ?: emptyList()

                    RetrofitClient.instance.getEnrollments("Bearer $token")
                        .enqueue(object : Callback<List<EnrollmentData>> {
                            override fun onResponse(call: Call<List<EnrollmentData>>, response: Response<List<EnrollmentData>>) {
                                isLoadingCourses = false
                                if (response.isSuccessful) {
                                    val enrollments = response.body() ?: emptyList()
                                    val enrolledCourseIds = enrollments.map { it.kursus_id }
                                    userCourses = allCourses.filter { it.kursus_id in enrolledCourseIds }
                                    Log.d("ProfilePage", "✅ Loaded ${userCourses.size} enrolled courses")
                                } else {
                                    Log.e("ProfilePage", "❌ Failed to get enrollments: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<List<EnrollmentData>>, t: Throwable) {
                                isLoadingCourses = false
                                Log.e("ProfilePage", "❌ Error loading enrollments", t)
                            }
                        })
                }

                override fun onFailure(call: Call<List<Kursus>>, t: Throwable) {
                    isLoadingCourses = false
                    Log.e("ProfilePage", "❌ Error loading courses", t)
                }
            })
    }

    // ============================================================
    // 🔥 UPDATE PROFILE - SIMPAN KE DATABASE VIA API
    // ============================================================
    fun updateProfile() {
        if (nama.isBlank()) {
            Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Tidak ada perubahan
        if (nama == originalNama && bio == originalBio) {
            Toast.makeText(context, "Tidak ada perubahan", Toast.LENGTH_SHORT).show()
            isEditing = false
            return
        }

        val token = tokenManager.getToken()
        val userId = tokenManager.getUserId()

        if (token.isNullOrEmpty() || userId == null) {
            Toast.makeText(context, "Sesi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        isUpdating = true
        Log.d("ProfilePage", "🔄 Updating profile to API...")

        val requestBody = mapOf(
            "nama" to nama,
            "bio" to bio
        )

        RetrofitClient.instance.updateProfile("Bearer $token", userId, requestBody)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    isUpdating = false

                    if (response.isSuccessful) {
                        val profileData = response.body()?.data

                        if (profileData != null) {
                            // Update data lokal
                            nama = profileData.nama
                            bio = profileData.bio ?: "—"

                            originalNama = nama
                            originalBio = bio

                            // 🔥 Update cache di SharedPreferences
                            val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("user_name", nama)
                                putString("user_bio", bio)
                                apply()
                            }

                            // Update token manager
                            tokenManager.saveToken(
                                token = token,
                                userId = userId.toString(),
                                name = nama,
                                username = username // 🔥 JANGAN HILANG
                            )

                            Toast.makeText(context, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            Log.d("ProfilePage", "✅ Profile updated successfully")
                            isEditing = false
                        }
                    } else {
                        Toast.makeText(context, "Gagal memperbarui profil: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e("ProfilePage", "❌ API Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    isUpdating = false
                    Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfilePage", "❌ Network error updating profile", t)
                }
            })
    }

    // ============================================================
    // 🔥 LOGOUT FUNCTION - HAPUS DATA LOKAL
    // ============================================================
    fun handleLogout() {
        try {
            // Hapus token dan data user dari SharedPreferences
            val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Toast.makeText(context, "Berhasil logout", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            Log.d("ProfilePage", "✅ Logout berhasil, data dihapus")
        } catch (e: Exception) {
            Log.e("ProfilePage", "❌ Error saat logout", e)
            Toast.makeText(context, "Gagal logout", Toast.LENGTH_SHORT).show()
        }
    }

    // Load pertama kali
    LaunchedEffect(Unit) {
        loadProfile()
        loadUserCourses()
    }

    // ============================================================
    // KAMERA & GALERI
    // ============================================================
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            profileImageUri = cameraImageUri
            cameraImageUri?.let { uploadProfilePhoto(it) } // ✅ Langsung upload
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ImageUtils.createImageUri(context)?.let { safeUri ->
                cameraImageUri = safeUri
                cameraLauncher.launch(safeUri)
            } ?: Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            profileImageUri = uri // Tampilkan preview
            uploadProfilePhoto(uri) // ✅ Langsung upload ke server
        }
    }

    // ============================================================
    // UI
    // ============================================================
    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda") }) {
                        Icon(Icons.Default.Home, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF4A0E24))
                    }
                }
            }
        }
    ) { innerPadding ->

        // Loading
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4A0E24))
            }
            return@Scaffold
        }

        // ERROR
        if (errorMessage != null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        errorMessage ?: "Error",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                    ) {
                        Text("Coba Lagi", color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        // MAIN CONTENT
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF5F7))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Card
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(Color(0xFFFFE4EC))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header dengan tombol logout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(48.dp))

                        Text(
                            "Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF4A0E24)
                        )

                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFF4A0E24)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 🔥 FOTO PROFIL - Prioritas: Preview lokal > Foto dari server > Inisial
                    Box(Modifier.size(110.dp), Alignment.BottomEnd) {
                        // Bungkus dengan clickable untuk preview
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable {
                                    // Hanya bisa preview jika ada foto
                                    if (profileImageUri != null || !profilePhotoUrl.isNullOrEmpty()) {
                                        showPhotoPreview = true
                                    }
                                }
                        ) {
                            when {
                                // 1. Preview foto yang baru dipilih (belum diupload/sedang diupload)
                                profileImageUri != null -> {
                                    Image(
                                        rememberAsyncImagePainter(profileImageUri),
                                        null,
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                // 2. Foto dari server (sudah diupload)
                                !profilePhotoUrl.isNullOrEmpty() -> {
                                    Image(
                                        rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(profilePhotoUrl)
                                                .crossfade(true)
                                                .error(R.drawable.tessampul)
                                                .build()
                                        ),
                                        null,
                                        Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                // 3. Default: Inisial nama
                                else -> {
                                    Box(
                                        Modifier.fillMaxSize().background(Color(0xFF4A0E24)),
                                        Alignment.Center
                                    ) {
                                        Text(
                                            nama.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Loading overlay saat upload foto
                        if (isUploadingPhoto) {
                            Box(
                                Modifier.size(100.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                                Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        // Tombol kamera (selalu tampil di mode edit)
                        if (isEditing) {
                            IconButton(
                                onClick = { showPhotoOptions = true },
                                Modifier.size(32.dp).background(Color.White, CircleShape),
                                enabled = !isUploadingPhoto
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF4A0E24))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Informasi Profil
                    if (!isEditing) {
                        ProfileRow("Nama", nama)
                        ProfileRow("Username", username)
                        ProfileRow("Bio", bio)
                    } else {
                        // Mode Edit - Hanya Nama dan Bio yang bisa diedit
                        OutlinedTextField(
                            value = nama,
                            onValueChange = { nama = it },
                            label = { Text("Nama") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A0E24),
                                focusedLabelColor = Color(0xFF4A0E24)
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // 🔥 USERNAME TIDAK BISA DIEDIT (Read-only)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.Gray,
                                disabledTextColor = Color.DarkGray
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A0E24),
                                focusedLabelColor = Color(0xFF4A0E24)
                            )
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Tombol Edit/Save dan Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isEditing) Arrangement.spacedBy(8.dp) else Arrangement.Center
                    ) {
                        if (isEditing) {
                            OutlinedButton(
                                onClick = {
                                    nama = originalNama
                                    bio = originalBio
                                    profileImageUri = null // Reset preview foto
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4A0E24)
                                )
                            ) {
                                Text("Batal", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { updateProfile() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A0E24)
                                ),
                                enabled = !isUpdating
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Text(
                                    "Edit Profile",
                                    color = Color(0xFF4A0E24),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Kelas Saya
            Text(
                "Kelas Saya",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A0E24),
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            if (isLoadingCourses) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
            } else if (userCourses.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).padding(16.dp),
                    Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Belum ada kursus yang diikuti",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    userCourses.chunked(2).forEach { rowCourses ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowCourses.forEach { kursus ->
                                Box(modifier = Modifier.weight(1f)) {
                                    CourseCard(kursus = kursus, onClick = {
                                    navController.navigate("detail/${kursus.kursus_id}")
                                })
                                }
                            }
                            if (rowCourses.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

// Dialog Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFF4A0E24)
                )
            },
            title = {
                Text("Konfirmasi Logout", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin keluar dari akun ini?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        handleLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A0E24)
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4A0E24)
                    )
                ) {
                    Text("Batal")
                }
            },
            containerColor = Color.White
        )
    }

// Dialog Pilihan Foto
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Ubah Foto Profil", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showPhotoOptions = false
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.padding(end = 8.dp))
                        Text("Ambil Foto")
                    }

                    TextButton(
                        onClick = {
                            showPhotoOptions = false
                            galleryPicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.padding(end = 8.dp))
                        Text("Pilih Dari Galeri")
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White
        )
    }
    // Dialog Preview Foto Profil
    if (showPhotoPreview) {
        AlertDialog(
            onDismissRequest = { showPhotoPreview = false },
            title = {
                Text(
                    "Foto Profil",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        profileImageUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUri),
                                contentDescription = "Preview Foto Profil",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        !profilePhotoUrl.isNullOrEmpty() -> {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(profilePhotoUrl)
                                        .crossfade(true)
                                        .error(R.drawable.tessampul)
                                        .build()
                                ),
                                contentDescription = "Preview Foto Profil",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPhotoPreview = false }
                ) {
                    Text("Tutup", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }
}
@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF4A0E24), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.DarkGray, fontSize = 14.sp)
    }
}
@Composable
fun CourseCard(kursus: Kursus, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(kursus.thumbnail)
                        .crossfade(true)
                        .error(R.drawable.tessampul)
                        .build()
                ),
                contentDescription = kursus.judul,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    kursus.judul,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF4A0E24)
                )
            }
        }
    }
}