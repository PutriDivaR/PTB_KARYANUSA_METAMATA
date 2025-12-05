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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.karyanusa.R
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.component.forum.ImageUtils
import com.example.karyanusa.network.Kursus
import com.example.karyanusa.network.RetrofitClient
import android.util.Log
import com.example.karyanusa.network.EnrollmentData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

    var nama by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("—") }

    var originalNama by remember { mutableStateOf("") }
    var originalUsername by remember { mutableStateOf("") }
    var originalBio by remember { mutableStateOf("—") }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔥 STATE UNTUK KURSUS USER
    var userCourses by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var isLoadingCourses by remember { mutableStateOf(false) }

    // ============================================================
    // 🔥 LOAD PROFILE
    // ============================================================
    fun loadProfile() {
        isLoading = true
        errorMessage = null

        try {
            val token = tokenManager.getToken()
            val userId = tokenManager.getUserId()
            val userName = tokenManager.getUserName()

            Log.d("ProfilePage", "=== DEBUG PROFILE ===")
            Log.d("ProfilePage", "Token: ${token ?: "NULL"}")
            Log.d("ProfilePage", "UserId: ${userId ?: "NULL"}")
            Log.d("ProfilePage", "UserName: ${userName ?: "NULL"}")

            // 🔥 NAMA DIAMBIL DARI LOGIN TOKEN (fallback ke default)
            nama = userName?.takeIf { it.isNotBlank() } ?: "Pengguna"

            // 🔥 Username auto-generate dari nama
            username = "@${nama.lowercase().replace(" ", "_")}"

            originalNama = nama
            originalUsername = username

            Log.d("ProfilePage", "✅ Profile loaded: $nama")
            isLoading = false

        } catch (e: Exception) {
            Log.e("ProfilePage", "❌ Error loading profile", e)
            errorMessage = "Gagal memuat profil: ${e.message}"
            isLoading = false
        }
    }

    // ============================================================
    // 🔥 LOAD KURSUS USER (ambil enrollments, lalu map ke kursus)
    // ============================================================
    fun loadUserCourses() {
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Log.d("ProfilePage", "Token kosong, skip load courses")
            return
        }

        isLoadingCourses = true
        Log.d("ProfilePage", "🔄 Loading user enrollments...")

        // Step 1: Get all courses
        RetrofitClient.instance.getCourses()
            .enqueue(object : Callback<List<Kursus>> {
                override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                    val allCourses = response.body() ?: emptyList()

                    // Step 2: Get user enrollments
                    RetrofitClient.instance.getEnrollments("Bearer $token")
                        .enqueue(object : Callback<List<EnrollmentData>> {
                            override fun onResponse(call: Call<List<EnrollmentData>>, response: Response<List<EnrollmentData>>) {
                                isLoadingCourses = false
                                if (response.isSuccessful) {
                                    val enrollments = response.body() ?: emptyList()
                                    val enrolledCourseIds = enrollments.map { it.kursus_id }

                                    // Filter courses yang user sudah enroll
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
    // 🔥 UPDATE PROFILE
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

        isUpdating = true

        try {
            val token = tokenManager.getToken()
            val userId = tokenManager.getUserId()

            // 🔥 Update lokal (karena backendmu belum support update profil)
            if (!token.isNullOrEmpty() && userId != null) {
                tokenManager.saveToken(
                    token = token,
                    userId = userId.toString(),
                    userName = nama
                )
            }

            originalNama = nama
            originalBio = bio
            originalUsername = "@${nama.lowercase().replace(" ", "_")}"
            username = originalUsername

            Toast.makeText(context, "✅ Profil diperbarui!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("ProfilePage", "❌ Error updating profile", e)
            Toast.makeText(context, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
        } finally {
            isUpdating = false
            isEditing = false
        }
    }

    // Load pertama kali
    LaunchedEffect(Unit) {
        loadProfile()
        loadUserCourses() // 🔥 Load kursus user
    }

    // ============================================================
    // KAMERA & GALERI
    // ============================================================
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) profileImageUri = cameraImageUri
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ImageUtils.createImageUri(context)?.let { safeUri ->
                cameraImageUri = safeUri
                cameraLauncher.launch(safeUri)
            } ?: Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) profileImageUri = uri
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
                Text(errorMessage ?: "Error", color = Color.Red)
            }
            return@Scaffold
        }

        // MAIN CONTENT (dengan scroll)
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF5F7))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(Color(0xFFFFE4EC))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header
                    Text("Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4A0E24))

                    Spacer(Modifier.height(16.dp))

                    // Foto Profil
                    Box(Modifier.size(110.dp), Alignment.BottomEnd) {

                        if (profileImageUri != null) {
                            Image(
                                rememberAsyncImagePainter(profileImageUri),
                                null,
                                Modifier.size(100.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF4A0E24)),
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

                        if (isEditing) {
                            IconButton(
                                onClick = { showPhotoOptions = true },
                                Modifier.size(32.dp).background(Color.White, CircleShape)
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
                        // Mode Edit - Input Fields
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

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth(),
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
                            // Tombol Cancel
                            OutlinedButton(
                                onClick = {
                                    // Reset ke nilai original
                                    nama = originalNama
                                    bio = originalBio
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4A0E24)
                                )
                            ) {
                                Text("Batal", fontWeight = FontWeight.Bold)
                            }

                            // Tombol Save
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
                            // Tombol Edit
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

            // 🔥 KELAS SAYA (List Kursus)
            Text(
                "Kelas Saya",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A0E24),
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Loading kursus
            if (isLoadingCourses) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
            }
            // List kursus
            else if (userCourses.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp),
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
                // Grid Kursus (2 kolom) - TANPA LazyVerticalGrid
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
                            // Jika hanya 1 item di row terakhir, tambahkan spacer
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

    // Dialog foto profil
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Ubah Foto Profil") },
            text = {
                Column {
                    TextButton(onClick = {
                        showPhotoOptions = false
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ambil Foto")
                    }

                    TextButton(onClick = {
                        showPhotoOptions = false
                        galleryPicker.launch("image/*")
                    }) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih Dari Galeri")
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White
        )
    }
}

// ============================================================
// COMPONENT KECIL
// ============================================================
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

// 🔥 CARD KURSUS (Simple - hanya thumbnail & judul)
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
            // Thumbnail
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

            // Judul Kursus
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