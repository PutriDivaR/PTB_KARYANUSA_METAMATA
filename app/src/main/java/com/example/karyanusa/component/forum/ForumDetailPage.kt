package com.example.karyanusa.component.forum

import android.Manifest
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.ForumPertanyaanResponse
import com.example.karyanusa.network.ForumJawabanResponse
import com.example.karyanusa.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailPage(
    navController: NavController,
    questionId: Int
) {
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()

    // State untuk API
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var questionData by remember { mutableStateOf<ForumPertanyaanResponse?>(null) }

    // State untuk reply
    var replyText by remember { mutableStateOf("") }
    var showImagePopup by remember { mutableStateOf(false) }
    val replyImages = remember { mutableStateListOf<Uri>() }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSendingReply by remember { mutableStateOf(false) }

    // State untuk image viewer
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }


    fun loadForumDetail() {
        isLoading = true
        error = null

        if (token.isNullOrEmpty()) {
            isLoading = false
            error = "Token tidak ditemukan, silakan login."
            return
        }

        RetrofitClient.instance.getPertanyaanDetail("Bearer $token", questionId)
            .enqueue(object : Callback<ForumPertanyaanResponse> {

                override fun onResponse(
                    call: Call<ForumPertanyaanResponse>,
                    response: Response<ForumPertanyaanResponse>
                ) {
                    isLoading = false

                    if (response.isSuccessful) {
                        questionData = response.body()
                    } else if (response.code() == 401) {
                        error = "Sesi berakhir. Silakan login kembali."
                    } else {
                        val errorBody = response.errorBody()?.string()
                        error = "Gagal memuat data (${response.code()}): $errorBody"
                    }
                }

                override fun onFailure(
                    call: Call<ForumPertanyaanResponse>,
                    t: Throwable
                ) {
                    isLoading = false
                    error = "Tidak bisa terhubung ke server: ${t.message}"
                }
            })
    }

    // Update fungsi sendReply() di ForumDetailPage.kt

    fun sendReply() {
        if (replyText.isBlank()) {
            Toast.makeText(context, "Isi balasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        isSendingReply = true

        // ✅ Buat RequestBody untuk isi balasan
        val isiRequestBody = replyText.toRequestBody("text/plain".toMediaTypeOrNull())

        // ✅ Buat MultipartBody.Part untuk gambar (jika ada)
        var imagePart: MultipartBody.Part? = null
        if (replyImages.isNotEmpty()) {
            val uri = replyImages.first()
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val file = File(context.cacheDir, "temp_reply_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image_jawaban", file.name, requestFile)
                }
            } catch (e: Exception) {
                Log.e("ForumDetailPage", "Error reading image: ${e.message}")
            }
        }

        // ✅ Panggil API dengan token
        RetrofitClient.instance.tambahJawaban(
            token = "Bearer $token",
            id = questionId,
            isi = isiRequestBody,
            image_jawaban = imagePart
        ).enqueue(object : Callback<ForumJawabanResponse> {

            override fun onResponse(
                call: Call<ForumJawabanResponse>,
                response: Response<ForumJawabanResponse>
            ) {
                isSendingReply = false

                if (response.isSuccessful) {
                    Toast.makeText(context, "Balasan terkirim!", Toast.LENGTH_SHORT).show()
                    replyText = ""
                    replyImages.clear()
                    loadForumDetail() // Reload data
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        context,
                        "Gagal mengirim balasan: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ForumDetailPage", "Error ${response.code()}: $errorBody")
                }
            }

            override fun onFailure(
                call: Call<ForumJawabanResponse>,
                t: Throwable
            ) {
                isSendingReply = false
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ForumDetailPage", "Failure", t)
            }
        })
    }


    fun formatTanggal(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                Locale.getDefault()
            )
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val date = inputFormat.parse(tanggal)
            outputFormat.format(date ?: Date())

        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                val date = inputFormat.parse(tanggal)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                tanggal
            }
        }
    }

    LaunchedEffect(questionId) {
        loadForumDetail()
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                if (replyImages.size < 4) replyImages.add(cameraImageUri!!)
                else Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                val total = replyImages.size + uris.size

                if (total > 4) {
                    Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
                    val allowed = 4 - replyImages.size
                    if (allowed > 0) replyImages.addAll(uris.take(allowed))
                } else {
                    replyImages.addAll(uris)
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Forum",
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFE4EC))
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Kursus", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("gallery") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Galeri", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF4A0E24))
                    }
                }
            }
        },
        containerColor = Color(0xFFFFF5F7)
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tampilan Loading
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
                return@Scaffold
            }

            // Tampilan Error
            if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $error", color = Color.Red, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { loadForumDetail() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                return@Scaffold
            }

            // Jika data tidak ditemukan
            if (questionData == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pertanyaan tidak ditemukan", color = Color.Gray)
                }
                return@Scaffold
            }

            // Main Content
            val data = questionData!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 1.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Pertanyaan
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFE4EC), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        // Header (User info)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD9D9D9))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    data.user?.nama ?: "Anonymous",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A0E24)
                                )
                                Text(
                                    "${data.user?.email ?: "user"} • ${formatTanggal(data.tanggal)}",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Surface(color = Color(0xFF4A0E24), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "${data.jawaban?.size ?: 0} Balasan",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Isi pertanyaan
                        Text(data.isi, fontSize = 15.sp, color = Color.Black)

                        // Gambar pertanyaan (jika ada)
                        if (!data.image_forum.isNullOrEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                painter = rememberAsyncImagePainter(data.image_forum),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 300.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray)
                                    .clickable {
                                        viewerImages = listOf(data.image_forum)
                                        selectedImageIndex = 0
                                        showImageViewer = true
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Input box untuk reply
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = {
                                        if (replyText.isEmpty()) {
                                            Toast.makeText(context, "Isi balasan dulu", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showImagePopup = true
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF4A0E24))
                                }

                                IconButton(onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Kamera", tint = Color(0xFF4A0E24))
                                }

                                BasicTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    decorationBox = { inner ->
                                        if (replyText.isEmpty()) {
                                            Text("Tulis balasan...", color = Color.Gray)
                                        }
                                        inner()
                                    },
                                    enabled = !isSendingReply
                                )

                                if (isSendingReply) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF4A0E24),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = { sendReply() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Kirim",
                                            tint = Color(0xFF4A0E24)
                                        )
                                    }
                                }
                            }
                        }

                        // Preview gambar reply
                        if (replyImages.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(replyImages) { uri ->
                                    Box(
                                        Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { replyImages.remove(uri) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                .size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Balasan",
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // List Jawaban
                if (data.jawaban.isNullOrEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada balasan",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(data.jawaban) { reply ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFE4EC), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD9D9D9))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        reply.user?.nama ?: "Anonymous",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4A0E24)
                                    )
                                    Text(
                                        "${reply.user?.email ?: "user"} • ${formatTanggal(reply.tanggal)}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(reply.isi, fontSize = 14.sp, color = Color.Black)

                            // Gambar jawaban jika ada
                            if (!reply.image_jawaban.isNullOrEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Image(
                                    painter = rememberAsyncImagePainter(reply.image_jawaban),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewerImages = listOf(reply.image_jawaban)
                                            selectedImageIndex = 0
                                            showImageViewer = true
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Popup upload gambar
            if (showImagePopup) {
                Dialog(onDismissRequest = { showImagePopup = false }) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .width(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFD6E8))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = Color(0xFF4A0E24),
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable { showImagePopup = false }
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Pilih gambar",
                                    color = Color(0xFF4A0E24),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Maksimal 4 gambar dapat diunggah.",
                                    color = Color(0xFF4A0E24).copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        showImagePopup = false
                                        imagePicker.launch("image/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                                    shape = RoundedCornerShape(30.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(45.dp)
                                ) {
                                    Text("Upload dari perangkat", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Image Viewer
            if (showImageViewer) {
                Dialog(onDismissRequest = { showImageViewer = false }) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        val pagerState = rememberPagerState(
                            initialPage = selectedImageIndex,
                            pageCount = { viewerImages.size }
                        )
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            Image(
                                painter = rememberAsyncImagePainter(viewerImages[page]),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        IconButton(
                            onClick = { showImageViewer = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(20.dp)
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}