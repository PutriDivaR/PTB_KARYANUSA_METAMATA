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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.karyanusa.R
import com.example.karyanusa.component.forum.ImageUtils
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.Kursus
import com.example.karyanusa.network.EnrollmentData
import com.example.karyanusa.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(navController: NavController) {
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var nama by remember { mutableStateOf("Putri Diva Riyanti") }
    var username by remember { mutableStateOf("@putrididip") }
    var bio by remember { mutableStateOf("Virgo Ni Bossttt") }

    var originalNama by remember { mutableStateOf(nama) }
    var originalUsername by remember { mutableStateOf(username) }
    var originalBio by remember { mutableStateOf(bio) }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) profileImageUri = cameraImageUri
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = ImageUtils.createImageUri(context)
            if (uri != null) {
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) profileImageUri = uri
    }

    // enrolled kursus (yang dah diikuti)
    var enrolledKursus by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var isLoadingEnrollments by remember { mutableStateOf(true) }

    // ambil token
    val token = LoginTokenManager(context).getToken()

    // Load enroll
    LaunchedEffect(Unit) {
        if (token == null) {
            enrolledKursus = emptyList()
            isLoadingEnrollments = false
            return@LaunchedEffect
        }

        // Get enroll
        RetrofitClient.instance.getEnrollments("Bearer $token")
            .enqueue(object : Callback<List<EnrollmentData>> {
                override fun onResponse(
                    call: Call<List<EnrollmentData>>,
                    response: Response<List<EnrollmentData>>
                ) {
                    if (!response.isSuccessful) {
                        enrolledKursus = emptyList()
                        isLoadingEnrollments = false
                        return
                    }

                    val enrollments = response.body() ?: emptyList()
                    if (enrollments.isEmpty()) {
                        enrolledKursus = emptyList()
                        isLoadingEnrollments = false
                        return
                    }

                    val temp = mutableListOf<Kursus>()
                    var done = 0
                    enrollments.forEach { enroll ->
                        RetrofitClient.instance.getKursusById(enroll.kursus_id)
                            .enqueue(object : Callback<Kursus> {
                                override fun onResponse(call: Call<Kursus>, res: Response<Kursus>) {
                                    res.body()?.let { temp.add(it) }
                                    done++
                                    if (done == enrollments.size) {
                                        enrolledKursus = temp
                                        isLoadingEnrollments = false
                                    }
                                }

                                override fun onFailure(call: Call<Kursus>, t: Throwable) {
                                    done++
                                    if (done == enrollments.size) {
                                        enrolledKursus = temp
                                        isLoadingEnrollments = false
                                    }
                                }
                            })
                    }
                }

                override fun onFailure(call: Call<List<EnrollmentData>>, t: Throwable) {
                    enrolledKursus = emptyList()
                    isLoadingEnrollments = false
                }
            })
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Forum", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Kursus", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Galeri", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF4A0E24))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF5F7)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF4A0E24)
                        )
                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    val changed = nama != originalNama || username != originalUsername || bio != originalBio
                                    if (changed) showConfirmDialog = true else isEditing = false
                                },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF4A0E24))
                            }
                        }
                    }

                    // Profile picture
                    Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.BottomEnd) {
                        if (profileImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUri),
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(100.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4A0E24)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile Picture", tint = Color.White, modifier = Modifier.size(50.dp))
                            }
                        }

                        if (isEditing) {
                            IconButton(
                                onClick = { showPhotoOptions = true },
                                modifier = Modifier.size(32.dp).background(Color.White, CircleShape)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Edit Foto", tint = Color(0xFF4A0E24), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info section
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        if (isEditing) {
                            EditableProfileField("Nama", nama) { nama = it }
                            EditableProfileField("Username", username) { username = it }
                            EditableProfileField("Bio", bio) { bio = it }
                        } else {
                            ProfileInfoRow("Nama", nama)
                            ProfileInfoRow("Username", username)
                            ProfileInfoRow("Bio", bio)
                        }
                        ProfileInfoRow("Karya", "4")
                        ProfileInfoRow("Kursus", enrolledKursus.size.toString())
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (isEditing) {
                                originalNama = nama
                                originalUsername = username
                                originalBio = bio
                                Toast.makeText(context, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
                            }
                            isEditing = !isEditing
                        },
                        colors = if (isEditing) ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)) else ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEditing) "Simpan" else "Edit Profile", color = if (isEditing) Color.White else Color(0xFF4A0E24), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kelas Saya header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kelas Saya", fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24), fontSize = 18.sp)
                Text("${enrolledKursus.size}", color = Color.Gray, fontSize = 14.sp)
            }

            if (isLoadingEnrollments) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (enrolledKursus.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada kelas yang diikuti.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(enrolledKursus) { kursus ->
                        SmallCourseCard(kursus = kursus) {
                            navController.navigate("detail/${kursus.kursus_id}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Confirm batal edit
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Batalkan perubahan?") },
            text = { Text("Perubahan yang belum disimpan akan hilang.") },
            confirmButton = {
                TextButton(onClick = {
                    nama = originalNama
                    username = originalUsername
                    bio = originalBio
                    isEditing = false
                    showConfirmDialog = false
                }) {
                    Text("Ya, batalkan", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Tidak", color = Color(0xFF4A0E24))
                }
            },
            containerColor = Color.White
        )
    }

    // Photo options dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Ubah Foto Profil") },
            text = {
                Column {
                    TextButton(onClick = {
                        showPhotoOptions = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF4A0E24))
                        Spacer(Modifier.width(8.dp))
                        Text("Ambil Foto", color = Color(0xFF4A0E24))
                    }

                    TextButton(onClick = {
                        showPhotoOptions = false
                        imagePicker.launch("image/*")
                    }) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF4A0E24))
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih dari Galeri", color = Color(0xFF4A0E24))
                    }

                    if (profileImageUri != null) {
                        TextButton(onClick = {
                            profileImageUri = null
                            showPhotoOptions = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text("Hapus Foto", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color.White
        )
    }
}

// card daftar kursus diikuti user
@Composable
fun SmallCourseCard(kursus: Kursus, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(160.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(kursus.thumbnail)
                        .placeholder(R.drawable.tessampul)
                        .error(R.drawable.tessampul)
                        .build()
                ),
                contentDescription = kursus.judul,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = kursus.judul,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A0E24),
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// helper components

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF4A0E24), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.DarkGray, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF4A0E24), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        TextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.DarkGray, fontSize = 14.sp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF4A0E24),
                focusedTextColor = Color.DarkGray,
                unfocusedTextColor = Color.DarkGray
            ),
            modifier = Modifier.widthIn(min = 120.dp)
        )
    }
}
