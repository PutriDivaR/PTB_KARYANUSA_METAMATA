package com.example.karyanusa.component.forum

import android.Manifest
import android.content.Context
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPagerIndicator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.karyanusa.network.SimpleResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    var isPosting by remember { mutableStateOf(false) }

    var myQuestions by remember { mutableStateOf<List<ForumPertanyaanResponse>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("all") }
    var isLoadingQuestions by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()
    val userId = tokenManager.getUserId()

    val currentUserDisplayName = remember {
        val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
        prefs.getString("user_name", null)?.takeIf { it.isNotBlank() } ?: "Pengguna"
    }

    val currentUserUsername = remember {
        val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
        prefs.getString("user_username", null)?.takeIf { it.isNotBlank() } ?: "user"
    }

    val currentUserPhotoUrl = remember {
        val prefs = context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)
        prefs.getString("user_foto_profile", null)
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            if (imageUris.size < 4) {
                imageUris.add(cameraImageUri!!)
            } else {
                Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
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

    val multipleImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 4 - imageUris.size
            if (remainingSlots > 0) {
                val imagesToAdd = uris.take(remainingSlots)
                imageUris.addAll(imagesToAdd)

                if (uris.size > remainingSlots) {
                    Toast.makeText(
                        context,
                        "Hanya ${remainingSlots} gambar yang ditambahkan (maksimal 4 gambar)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "${imagesToAdd.size} gambar berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showImagePreview by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    var showCancelDialog by remember { mutableStateOf(false) }


    fun formatTanggal(tanggal: String): String {
        val indonesiaLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
        return try {
            val cleanedDate = tanggal.replace(Regex("\\.\\d+Z"), "Z")
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)
            val date = inputFormat.parse(cleanedDate)
            outputFormat.format(date ?: Date())
        } catch (_: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)
                val date = inputFormat.parse(tanggal)
                outputFormat.format(date ?: Date())
            } catch (_: Exception) {
                "Baru saja"
            }
        }
    }


    fun loadMyQuestions() {
        if (token.isNullOrEmpty()) return

        isLoadingQuestions = true
        RetrofitClient.instance.getPertanyaan("Bearer $token")
            .enqueue(object : Callback<List<ForumPertanyaanResponse>> {
                override fun onResponse(
                    call: Call<List<ForumPertanyaanResponse>>,
                    response: Response<List<ForumPertanyaanResponse>>
                ) {
                    isLoadingQuestions = false
                    if (response.isSuccessful) {
                        // Filter hanya pertanyaan saya
                        myQuestions = response.body()?.filter { it.user_id == userId } ?: emptyList()
                    }
                }

                override fun onFailure(call: Call<List<ForumPertanyaanResponse>>, t: Throwable) {
                    isLoadingQuestions = false
                }
            })
    }

    LaunchedEffect(Unit) {
        loadMyQuestions()
    }

    val filteredQuestions = remember(myQuestions, selectedFilter) {
        when (selectedFilter) {
            "answered" -> myQuestions.filter { !it.jawaban.isNullOrEmpty() }
            "unanswered" -> myQuestions.filter { it.jawaban.isNullOrEmpty() }
            else -> myQuestions
        }
    }

    val allCount = myQuestions.size
    val unansweredCount = myQuestions.count { it.jawaban.isNullOrEmpty() }
    val answeredCount = myQuestions.count { !it.jawaban.isNullOrEmpty() }


    fun compressImage(context: Context, uri: Uri, maxSizeKB: Int = 1024): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            var quality = 90
            var outputStream: ByteArrayOutputStream

            do {
                outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                quality -= 10
            } while (outputStream.size() / 1024 > maxSizeKB && quality > 10)

            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            file.outputStream().use {
                it.write(outputStream.toByteArray())
            }

            Log.d("ForumAddPage", "Compressed: ${outputStream.size() / 1024}KB, Quality: $quality")
            return file

        } catch (e: Exception) {
            Log.e("ForumAddPage", "Error compressing image", e)
            return null
        }
    }

    fun postQuestion() {
        if (question.isBlank()) {
            Toast.makeText(context, "Pertanyaan harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            return
        }

        Log.d("ForumAddPage", "=== MULAI POST PERTANYAAN ===")
        Log.d("ForumAddPage", "Token: ${token.take(20)}...")
        Log.d("ForumAddPage", "User ID: $userId")
        Log.d("ForumAddPage", "Question: $question")
        Log.d("ForumAddPage", "Total gambar: ${imageUris.size}")

        isPosting = true

        val isiRequestBody = question.toRequestBody("text/plain".toMediaTypeOrNull())
        val imageParts = mutableListOf<MultipartBody.Part>()

        if (imageUris.isNotEmpty()) {
            imageUris.forEachIndexed { index, uri ->
                try {
                    Log.d("ForumAddPage", "Processing image $index: $uri")

                    val compressedFile = compressImage(context, uri, maxSizeKB = 1024)

                    if (compressedFile == null || !compressedFile.exists()) {
                        Log.e("ForumAddPage", "Gagal kompres gambar $index")
                        Toast.makeText(context, "Gagal memproses gambar ke-${index + 1}", Toast.LENGTH_SHORT).show()
                        isPosting = false
                        return
                    }

                    val fileSizeKB = compressedFile.length() / 1024
                    Log.d("ForumAddPage", "✓ Compressed file: ${compressedFile.name}, Size: ${fileSizeKB}KB")

                    val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image_forum[]", compressedFile.name, requestFile)
                    imageParts.add(part)

                    Log.d("ForumAddPage", "✓ Part created: image_forum -> ${compressedFile.name}")

                } catch (e: Exception) {
                    Log.e("ForumAddPage", "Error processing image $index", e)
                    Toast.makeText(context, "Error gambar ${index + 1}: ${e.message}", Toast.LENGTH_SHORT).show()
                    isPosting = false
                    return
                }
            }
        }

        Log.d("ForumAddPage", "=== SUMMARY ===")
        Log.d("ForumAddPage", "Total image parts: ${imageParts.size}")
        imageParts.forEachIndexed { idx, part ->
            Log.d("ForumAddPage", "Part $idx headers: ${part.headers}")
        }
        Log.d("ForumAddPage", "Mengirim request ke API...")

        val call = RetrofitClient.instance.tambahPertanyaan(
            "Bearer $token",
            isiRequestBody,
            imageParts
        )

        call.enqueue(object : Callback<ForumPertanyaanResponse> {
            override fun onResponse(
                call: Call<ForumPertanyaanResponse>,
                response: Response<ForumPertanyaanResponse>
            ) {
                isPosting = false
                Log.d("ForumAddPage", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("ForumAddPage", "✓ SUCCESS: Pertanyaan ID = ${result?.pertanyaan_id}")
                    Toast.makeText(context, "Pertanyaan berhasil diposting!", Toast.LENGTH_LONG).show()

                    question = ""
                    imageUris.clear()

                    loadMyQuestions()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ForumAddPage", "✗ FAILED: Error ${response.code()}")
                    Log.e("ForumAddPage", "Error body: $errorBody")

                    val errorMsg = when (response.code()) {
                        401 -> "Sesi berakhir, silakan login kembali"
                        422 -> "Data tidak valid: $errorBody"
                        413 -> "File terlalu besar"
                        500 -> "Server error. Silakan coba lagi"
                        else -> "Gagal memposting (${response.code()}): $errorBody"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()

                    if (response.code() == 401) {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ForumPertanyaanResponse>, t: Throwable) {
                isPosting = false
                Log.e("ForumAddPage", "✗ NETWORK FAILURE", t)
                Log.e("ForumAddPage", "Error message: ${t.message}")

                val errorMsg = when {
                    t.message?.contains("timeout", ignoreCase = true) == true ->
                        "Koneksi timeout. Coba lagi"
                    t.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak ada koneksi internet"
                    else -> "Koneksi gagal: ${t.message}"
                }

                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tambah Pertanyaan",
                            color = Color(0xFF4A0E24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isPosting) {
                            if (question.isNotBlank() || imageUris.isNotEmpty()) {
                                showCancelDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        }
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
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color(0xFF4A0E24))
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
        },
        containerColor = Color(0xFFFFF5F7)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                            ) {
                                if (!currentUserPhotoUrl.isNullOrEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            coil.request.ImageRequest.Builder(context)
                                                .data(currentUserPhotoUrl)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.DarkGray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (currentUserDisplayName.firstOrNull() ?: "?").toString().uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentUserDisplayName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A0E24),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@$currentUserUsername",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

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

                        if (imageUris.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(imageUris) { index, uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedImageIndex = index
                                                showImagePreview = true
                                            }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = "Preview Gambar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
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
                            Text(
                                text = "${imageUris.size}/4 gambar",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { multipleImagePicker.launch("image/*") },
                                    enabled = !isPosting && imageUris.size < 4
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Pilih Gambar",
                                        tint = if (imageUris.size < 4) Color(0xFF4A0E24) else Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    enabled = !isPosting && imageUris.size < 4
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Ambil Foto",
                                        tint = if (imageUris.size < 4) Color(0xFF4A0E24) else Color.Gray
                                    )
                                }
                            }

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

                        Spacer(Modifier.height(24.dp))
                        Divider(color = Color(0xFFFFB3C1), thickness = 1.dp)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Pertanyaan Saya",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A0E24)
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomFilterChip(
                                text = "All ($allCount)",
                                isSelected = selectedFilter == "all",
                                onClick = { selectedFilter = "all" }
                            )
                            CustomFilterChip(
                                text = "Unanswered ($unansweredCount)",
                                isSelected = selectedFilter == "unanswered",
                                onClick = { selectedFilter = "unanswered" }
                            )
                            CustomFilterChip(
                                text = "Answered ($answeredCount)",
                                isSelected = selectedFilter == "answered",
                                onClick = { selectedFilter = "answered" }
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                if (isLoadingQuestions) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4A0E24))
                        }
                    }
                } else if (filteredQuestions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedFilter) {
                                    "answered" -> "Belum ada pertanyaan yang dijawab"
                                    "unanswered" -> "Belum ada pertanyaan yang belum dijawab"
                                    else -> "Kamu belum membuat pertanyaan"
                                },
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredQuestions,
                        key = { it.pertanyaan_id }
                    ) { questionItem ->
                        var expandedMenu by remember { mutableStateOf(false) }

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            QuestionCardItem(
                                question = questionItem,
                                isMyQuestion = true,
                                formatTanggal = { formatTanggal(it) },
                                onCardClick = {
                                    navController.navigate("forumDetail/${questionItem.pertanyaan_id}")
                                },
                                expandedMenu = expandedMenu,
                                onMenuClick = { expandedMenu = !expandedMenu },
                                onDismissMenu = { expandedMenu = false },
                                onEditClick = {
                                    expandedMenu = false
                                    navController.navigate("editPertanyaan/${questionItem.pertanyaan_id}")
                                },
                                onDeleteClick = {
                                    expandedMenu = false
                                    questionToDelete = questionItem.pertanyaan_id
                                    showDeleteDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

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

        if (showDeleteDialog && questionToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isDeleting) {
                        showDeleteDialog = false
                        questionToDelete = null
                    }
                },
                title = {
                    Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold)
                },
                text = {
                    if (isDeleting) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF4A0E24),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Menghapus...")
                        }
                    } else {
                        Text("Apakah kamu yakin ingin menghapus pertanyaan ini?")
                    }
                },
                confirmButton = {
                    if (!isDeleting) {
                        TextButton(onClick = {
                            isDeleting = true
                            val idToDelete = questionToDelete!!

                            RetrofitClient.instance.deletePertanyaan("Bearer $token", idToDelete)
                                .enqueue(object : Callback<SimpleResponse> {
                                    override fun onResponse(
                                        call: Call<SimpleResponse>,
                                        response: Response<SimpleResponse>
                                    ) {
                                        isDeleting = false
                                        if (response.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "Pertanyaan berhasil dihapus",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            loadMyQuestions()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Gagal menghapus: ${response.code()}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        showDeleteDialog = false
                                        questionToDelete = null
                                    }

                                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                        isDeleting = false
                                        Toast.makeText(
                                            context,
                                            "Error: ${t.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showDeleteDialog = false
                                        questionToDelete = null
                                    }
                                })
                        }) {
                            Text("Ya", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!isDeleting) {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            questionToDelete = null
                        }) {
                            Text("Batal", color = Color.Gray)
                        }
                    }
                },
                containerColor = Color.White,
                tonalElevation = 4.dp
            )
        }

        if (showImagePreview) {
            ImagePreviewDialog(
                images = imageUris,
                initialPage = selectedImageIndex,
                onDismiss = { showImagePreview = false }
            )
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = {
                    Text(
                        "Batalkan Postingan?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Perubahan yang belum diposting akan hilang.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            question = ""
                            imageUris.clear()
                            navController.popBackStack()
                        }
                    ) {
                        Text(
                            "Ya, Batalkan",
                            color = Color(0xFF4A0E24),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCancelDialog = false }
                    ) {
                        Text("Tidak", color = Color.Gray)
                    }
                },
                containerColor = Color.White,
                tonalElevation = 4.dp
            )
        }
    }
}


@Composable
fun CustomFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFFFB3C1) else Color.White,
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFFFB3C1)) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) Color(0xFF4A0E24) else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun QuestionCardItem(
    question: ForumPertanyaanResponse,
    isMyQuestion: Boolean,
    formatTanggal: (String) -> String,
    onCardClick: () -> Unit,
    expandedMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val imageUrls = remember(question.image_forum) {
        question.image_forum ?: emptyList()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3C1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔥 AVATAR - Prioritas: Foto profil > Inisial
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                ) {
                    if (!question.user?.foto_profile.isNullOrEmpty()) {
                        // Tampilkan foto profil jika ada
                        Image(
                            painter = rememberAsyncImagePainter(
                                coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(question.user?.foto_profile)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (question.user?.nama?.firstOrNull() ?: "?").toString().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = question.user?.nama ?: "Anonymous",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A0E24),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    Text(
                        text = "@${question.user?.username ?: "user"}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                if (isMyQuestion) {
                    Box {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color(0xFF4A0E24)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = onDismissMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = onEditClick
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus") },
                                onClick = onDeleteClick
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.isi,
                fontSize = 14.sp,
                color = Color(0xFF4A0E24),
                lineHeight = 20.sp
            )

            if (imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                when (imageUrls.size) {
                    1 -> {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrls[0]),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 300.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    }
                    2 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            imageUrls.forEach { imageUrl ->
                                Image(
                                    painter = rememberAsyncImagePainter(imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    3 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUrls[0]),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                imageUrls.drop(1).forEach { imageUrl ->
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.LightGray),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                imageUrls.take(2).forEach { imageUrl ->
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.LightGray),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                imageUrls.drop(2).take(2).forEach { imageUrl ->
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.LightGray),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Balasan",
                        tint = Color(0xFF4A0E24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${question.jawaban?.size ?: 0} Balasan",
                        fontSize = 13.sp,
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImagePreviewDialog(
    images: List<Uri>,
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                count = images.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(images[page]),
                        contentDescription = "Preview Image ${page + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }


            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            if (images.size > 1) {
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    activeColor = Color.White,
                    inactiveColor = Color.Gray
                )
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}