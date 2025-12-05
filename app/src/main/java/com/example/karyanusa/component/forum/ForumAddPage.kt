package com.example.karyanusa.component.forum

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.ForumPertanyaanResponse
import com.example.karyanusa.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Helper object untuk membuat file gambar sementara
object ImageUtils {
    fun createImageUri(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.externalCacheDir ?: context.cacheDir
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumAddPage(navController: NavController) {
    // State untuk input pengguna
    var question by remember { mutableStateOf("") }
    val imageUris = remember { mutableStateListOf<Uri>() }

    // State untuk UI
    var isPosting by remember { mutableStateOf(false) }

    // Context dan manager untuk info user & token
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()
    val userId = tokenManager.getUserId()
    val currentUserUsername = tokenManager.getUserName() ?: "pengguna@email.com"
    val currentUserDisplayName = tokenManager.getUserName() ?: "Nama Pengguna"

    // --- Launchers untuk gambar & izin ---
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            if (imageUris.size < 1) {
                imageUris.add(cameraImageUri!!)
            } else {
                Toast.makeText(context, "Maksimal 1 gambar", Toast.LENGTH_SHORT).show()
            }
        }
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
        } else {
            Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (imageUris.isEmpty()) {
                imageUris.add(uri)
            } else {
                Toast.makeText(context, "Hanya dapat memilih 1 gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Fungsi untuk Post Pertanyaan ke API ---
    fun postQuestion() {
        // Validasi input
        if (question.isBlank()) {
            Toast.makeText(context, "Pertanyaan harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        isPosting = true

        // ✅ Hanya kirim 'isi' (user_id diambil dari auth()->id() di backend)
        val isiRequestBody = question.toRequestBody("text/plain".toMediaTypeOrNull())

        // Membuat MultipartBody.Part untuk file gambar
        var imagePart: MultipartBody.Part? = null
        if (imageUris.isNotEmpty()) {
            val uri = imageUris.first()
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image_forum", file.name, requestFile)
                }
            } catch (e: Exception) {
                Log.e("ForumAddPage", "Error reading image: ${e.message}")
                Toast.makeText(context, "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
                isPosting = false
                return
            }
        }

        // ✅ Panggil API dengan Bearer token
        val call = RetrofitClient.instance.tambahPertanyaan(
            "Bearer $token",
            isiRequestBody,
            imagePart
        )

        call.enqueue(object : Callback<ForumPertanyaanResponse> {
            override fun onResponse(
                call: Call<ForumPertanyaanResponse>,
                response: Response<ForumPertanyaanResponse>
            ) {
                isPosting = false
                if (response.isSuccessful) {
                    Toast.makeText(context, "Pertanyaan berhasil diposting!", Toast.LENGTH_LONG).show()
                    navController.popBackStack() // Kembali ke halaman forum
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = "Gagal memposting: ${response.code()}"
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("ForumAddPage", "Error ${response.code()}: $errorBody")
                }
            }

            override fun onFailure(call: Call<ForumPertanyaanResponse>, t: Throwable) {
                isPosting = false
                Toast.makeText(context, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("ForumAddPage", "Failure", t)
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Buat Pertanyaan",
                        fontSize = 20.sp,
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isPosting) navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFE4EC)
                )
            )
        },
        containerColor = Color(0xFFFFF5F7)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Info User
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Placeholder untuk foto profil
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9D9D9))
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp),
                            tint = Color.Gray
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            currentUserDisplayName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A0E24)
                        )
                        Text(
                            currentUserUsername,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Input Pertanyaan
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFFFE4EC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (question.isEmpty()) {
                            Text("Ketik pertanyaanmu di sini...", color = Color.Gray)
                        }
                        inner()
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Preview Gambar
                if (imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Preview Gambar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Tombol hapus gambar
                                IconButton(
                                    onClick = { imageUris.remove(uri) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        .size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Hapus Gambar",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Tombol-tombol Aksi
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Tambah dari galeri
                        IconButton(
                            onClick = { imagePicker.launch("image/*") },
                            enabled = !isPosting
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pilih Gambar",
                                tint = Color(0xFF4A0E24)
                            )
                        }
                        // Tambah dari kamera
                        IconButton(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            enabled = !isPosting
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Ambil Foto",
                                tint = Color(0xFF4A0E24)
                            )
                        }
                    }

                    // Tombol Post
                    Button(
                        onClick = { postQuestion() },
                        enabled = !isPosting && question.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A0E24),
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Tampilan Loading Overlay saat posting
            if (isPosting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}