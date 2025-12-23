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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import kotlinx.coroutines.*
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumEditPage(
    navController: NavController,
    questionId: Int
) {
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }

    // State untuk API
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var questionData by remember { mutableStateOf<ForumPertanyaanResponse?>(null) }

    // State untuk edit
    var questionText by remember { mutableStateOf("") }
    val editableImages = remember { mutableStateListOf<Uri>() }

    // ✅ TAMBAHAN: Track original images untuk deteksi perubahan
    var originalImages by remember { mutableStateOf<List<String>>(emptyList()) }

    // State untuk UI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    fun loadQuestionForEdit() {
        isLoading = true
        errorMessage = null

        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            isLoading = false
            errorMessage = "Token tidak ditemukan, silakan login."
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
                        val data = response.body()

                        if (data != null) {
                            questionData = data
                            questionText = data.isi

                            editableImages.clear()
                            data.image_forum?.forEach { imageUrl ->
                                editableImages.add(imageUrl.toUri())
                            }

                            // ✅ Simpan original images
                            originalImages = data.image_forum ?: emptyList()

                        } else {
                            errorMessage = "Data tidak ditemukan"
                        }

                    } else if (response.code() == 401) {
                        errorMessage = "Sesi berakhir. Silakan login kembali."

                    } else {
                        val errorBody = response.errorBody()?.string()
                        errorMessage = "Gagal memuat data (${response.code()}): $errorBody"
                    }
                }

                override fun onFailure(
                    call: Call<ForumPertanyaanResponse>,
                    t: Throwable
                ) {
                    isLoading = false
                    errorMessage = "Koneksi gagal: ${t.message}"
                }
            })
    }

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

            Log.d("ForumEdit", "Compressed: ${outputStream.size() / 1024}KB, Quality: $quality")
            return file

        } catch (e: Exception) {
            Log.e("ForumEdit", "Error compressing image", e)
            return null
        }
    }

    fun updateQuestionWithPartMap() {
        if (questionText.isBlank()) {
            Toast.makeText(context, "Isi pertanyaan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
            isSaving = false
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageParts = mutableListOf<MultipartBody.Part>()
                val keepImages = mutableListOf<String>()

                // ✅ Proses gambar dengan KOMPRESI
                editableImages.forEachIndexed { index, imageUri ->
                    when {
                        // 🆕 Gambar BARU dari galeri/kamera - KOMPRES DULU!
                        imageUri.scheme == "content" || imageUri.scheme == "file" -> {
                            try {
                                Log.d("ForumEdit", "Processing new image $index: $imageUri")

                                // ✅ KOMPRES GAMBAR (sama seperti ForumAddPage)
                                val compressedFile = compressImage(context, imageUri, maxSizeKB = 1024)

                                if (compressedFile == null || !compressedFile.exists()) {
                                    Log.e("ForumEdit", "Gagal kompres gambar $index")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Gagal memproses gambar ke-${index + 1}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isSaving = false
                                    }
                                    return@launch
                                }

                                val fileSizeKB = compressedFile.length() / 1024
                                Log.d("ForumEdit", "✓ Compressed: ${compressedFile.name}, Size: ${fileSizeKB}KB")

                                // ✅ BUAT MULTIPART DARI FILE YANG SUDAH DIKOMPRES
                                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                val part = MultipartBody.Part.createFormData("image_forum[]", compressedFile.name, requestFile)
                                imageParts.add(part)

                                Log.d("ForumEdit", "✓ Part created: ${compressedFile.name}")

                            } catch (e: Exception) {
                                Log.e("ForumEdit", "Error processing image $index: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error gambar ${index + 1}: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSaving = false
                                }
                                return@launch
                            }
                        }

                        // 📌 Gambar LAMA dari server (URL) - Tidak perlu dikompres
                        imageUri.scheme == "http" || imageUri.scheme == "https" -> {
                            keepImages.add(imageUri.toString())
                            Log.d("ForumEdit", "✓ Keep old image: $imageUri")
                        }
                    }
                }

                // ✅ Buat PartMap
                val partMap = mutableMapOf<String, RequestBody>()
                partMap["isi"] = questionText.toRequestBody("text/plain".toMediaTypeOrNull())

                // Tambahkan keep_images
                keepImages.forEachIndexed { index, url ->
                    partMap["keep_images[$index]"] = url.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                Log.d("ForumEdit", "=== SUMMARY ===")
                Log.d("ForumEdit", "PartMap keys: ${partMap.keys}")
                Log.d("ForumEdit", "New images: ${imageParts.size}, Keep images: ${keepImages.size}")

                withContext(Dispatchers.Main) {
                    RetrofitClient.instance.updatePertanyaanWithPartMap(
                        token = "Bearer $token",
                        id = questionId,
                        data = partMap,
                        image_forum = if (imageParts.isEmpty()) null else imageParts
                    ).enqueue(object : Callback<ForumPertanyaanResponse> {
                        override fun onResponse(
                            call: Call<ForumPertanyaanResponse>,
                            response: Response<ForumPertanyaanResponse>
                        ) {
                            isSaving = false
                            if (response.isSuccessful) {
                                Toast.makeText(context, "✓ Berhasil update!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("ForumEdit", "Failed: ${response.code()} - $errorBody")
                                Toast.makeText(
                                    context,
                                    "Error ${response.code()}: ${errorBody?.take(100)}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<ForumPertanyaanResponse>, t: Throwable) {
                            isSaving = false
                            Log.e("ForumEdit", "Network: ${t.message}")
                            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

            } catch (e: Exception) {
                Log.e("ForumEdit", "Exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun formatTanggal(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.Builder().setLanguage("id").setRegion("ID").build()
            )
            val date = inputFormat.parse(tanggal)
            outputFormat.format(date ?: Date())
        } catch (_: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat(
                    "dd MMM yyyy, HH:mm",
                    Locale.Builder().setLanguage("id").setRegion("ID").build()
                )
                val date = inputFormat.parse(tanggal)
                outputFormat.format(date ?: Date())
            } catch (_: Exception) {
                tanggal
            }
        }
    }

    LaunchedEffect(questionId) {
        loadQuestionForEdit()
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            if (editableImages.size < 4) editableImages.add(cameraImageUri!!)
            else Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = ImageUtils.createImageUri(context)
            if (uri != null) {
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } else Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val total = editableImages.size + uris.size
            if (total > 4) {
                Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
                val allowed = 4 - editableImages.size
                if (allowed > 0) editableImages.addAll(uris.take(allowed))
            } else editableImages.addAll(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Pertanyaan",
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (questionData != null) {
                            val textChanged = questionText != questionData!!.isi

                            // ✅ PERBAIKAN: Deteksi perubahan gambar dengan benar
                            val currentImageStrings = editableImages.map { it.toString() }
                            val imageChanged = currentImageStrings != originalImages

                            if (textChanged || imageChanged) {
                                showExitConfirmDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }) {
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
        },
        containerColor = Color(0xFFFFF5F7)
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
                return@Scaffold
            }

            if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $errorMessage", color = Color.Red, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { loadQuestionForEdit() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                return@Scaffold
            }

            if (questionData == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pertanyaan tidak ditemukan", color = Color.Gray)
                }
                return@Scaffold
            }

            val data = questionData!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 1.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3C1))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 🔥 AVATAR - Prioritas: Foto profil > Inisial
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!data.user?.foto_profile.isNullOrEmpty()) {
                                            // Tampilkan foto profil jika ada
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    ImageRequest.Builder(context)
                                                        .data(data.user?.foto_profile)
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
                                            // Tampilkan inisial jika tidak ada foto
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.DarkGray, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (data.user?.nama?.firstOrNull() ?: "?").toString().uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = data.user?.nama ?: "Anonymous",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4A0E24),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "@${data.user?.username ?: "user"}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF4A0E24),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${data.jawaban?.size ?: 0} Balasan",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            BasicTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                decorationBox = { inner ->
                                    if (questionText.isEmpty()) {
                                        Text("Tulis pertanyaan...", color = Color.Gray, fontSize = 15.sp)
                                    }
                                    inner()
                                },
                                enabled = !isSaving
                            )

                            // Preview Gambar dengan Grid Layout
                            if (editableImages.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))

                                when (editableImages.size) {
                                    1 -> {
                                        Box {
                                            Image(
                                                painter = rememberAsyncImagePainter(editableImages[0]),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 200.dp, max = 300.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.LightGray)
                                                    .clickable {
                                                        viewerImages = editableImages.map { it.toString() }
                                                        selectedImageIndex = 0
                                                        showImageViewer = true
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { editableImages.removeAt(0) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                    .size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Hapus",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    2 -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            editableImages.forEachIndexed { index, imageUri ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(imageUri),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(180.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.LightGray)
                                                            .clickable {
                                                                viewerImages = editableImages.map { it.toString() }
                                                                selectedImageIndex = index
                                                                showImageViewer = true
                                                            },
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    IconButton(
                                                        onClick = { editableImages.removeAt(index) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                            .size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Hapus",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    3 -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box {
                                                Image(
                                                    painter = rememberAsyncImagePainter(editableImages[0]),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.LightGray)
                                                        .clickable {
                                                            viewerImages = editableImages.map { it.toString() }
                                                            selectedImageIndex = 0
                                                            showImageViewer = true
                                                        },
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = { editableImages.removeAt(0) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Hapus",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                editableImages.drop(1).forEachIndexed { idx, imageUri ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(imageUri),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(120.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = editableImages.map { it.toString() }
                                                                    selectedImageIndex = idx + 1
                                                                    showImageViewer = true
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        IconButton(
                                                            onClick = { editableImages.removeAt(idx + 1) },
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(8.dp)
                                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                                .size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Hapus",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
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
                                                editableImages.take(2).forEachIndexed { index, imageUri ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(imageUri),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = editableImages.map { it.toString() }
                                                                    selectedImageIndex = index
                                                                    showImageViewer = true
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        IconButton(
                                                            onClick = { editableImages.removeAt(index) },
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(8.dp)
                                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                                .size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Hapus",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                editableImages.drop(2).take(2).forEachIndexed { idx, imageUri ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(imageUri),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = editableImages.map { it.toString() }
                                                                    selectedImageIndex = idx + 2
                                                                    showImageViewer = true
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        IconButton(
                                                            onClick = { editableImages.removeAt(idx + 2) },
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(8.dp)
                                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                                .size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Hapus",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { imagePicker.launch("image/*") },
                                        enabled = !isSaving
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Galeri",
                                            tint = Color(0xFF4A0E24)
                                        )
                                    }
                                    IconButton(
                                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                        enabled = !isSaving
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Kamera",
                                            tint = Color(0xFF4A0E24)
                                        )
                                    }
                                }

                                Text(
                                    text = formatTanggal(data.tanggal),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3C1))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // 🔥 AVATAR BALASAN - Prioritas: Foto profil > Inisial
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!reply.user?.foto_profile.isNullOrEmpty()) {
                                            // Tampilkan foto profil jika ada
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    ImageRequest.Builder(context)
                                                        .data(reply.user?.foto_profile)
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
                                            // Tampilkan inisial jika tidak ada foto
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.DarkGray, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (reply.user?.nama?.firstOrNull() ?: "?").toString().uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reply.user?.nama ?: "Anonymous",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4A0E24),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "@${reply.user?.username ?: "user"}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = formatTanggal(reply.tanggal),
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(reply.isi, fontSize = 14.sp, color = Color.Black)

                                if (!reply.image_jawaban.isNullOrEmpty()) {
                                    Spacer(Modifier.height(8.dp))

                                    val replyImageUrls = when (reply.image_jawaban) {
                                        is List<*> -> reply.image_jawaban.filterIsInstance<String>()
                                        is String -> listOf(reply.image_jawaban)
                                        else -> emptyList()
                                    }

                                    if (replyImageUrls.isNotEmpty()) {
                                        when (replyImageUrls.size) {
                                            1 -> {
                                                Image(
                                                    painter = rememberAsyncImagePainter(replyImageUrls[0]),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 150.dp, max = 250.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            viewerImages = replyImageUrls
                                                            selectedImageIndex = 0
                                                            showImageViewer = true
                                                        },
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            else -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    replyImageUrls.take(2).forEachIndexed { index, imageUrl ->
                                                        Image(
                                                            painter = rememberAsyncImagePainter(imageUrl),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .clickable {
                                                                    viewerImages = replyImageUrls
                                                                    selectedImageIndex = index
                                                                    showImageViewer = true
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Button Simpan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (questionText.isNotBlank()) {
                            showConfirmDialog = true
                        } else {
                            Toast.makeText(context, "Isi pertanyaan dulu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Simpan",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Dialog Konfirmasi Simpan
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSaving) showConfirmDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog = false
                                updateQuestionWithPartMap()
                            },
                            enabled = !isSaving
                        ) {
                            Text("Ya", color = Color(0xFF4A0E24))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showConfirmDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Batal")
                        }
                    },
                    title = { Text("Konfirmasi", fontWeight = FontWeight.Bold) },
                    text = { Text("Apakah Anda yakin ingin menyimpan perubahan?") },
                    containerColor = Color.White,
                    tonalElevation = 6.dp
                )
            }

            // Dialog Konfirmasi Keluar
            if (showExitConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showExitConfirmDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showExitConfirmDialog = false
                            navController.popBackStack()
                        }) {
                            Text("Ya, batalkan", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirmDialog = false }) {
                            Text("Tidak", color = Color(0xFF4A0E24))
                        }
                    },
                    title = { Text("Batalkan perubahan?", fontWeight = FontWeight.Bold) },
                    text = { Text("Perubahan yang belum disimpan akan hilang.") },
                    containerColor = Color.White
                )
            }

            // Image Viewer Dialog dengan Zoom & Slide
            if (showImageViewer && viewerImages.isNotEmpty()) {
                Dialog(
                    onDismissRequest = { showImageViewer = false },
                    properties = androidx.compose.ui.window.DialogProperties(
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
                        val pagerState = rememberPagerState(
                            initialPage = selectedImageIndex,
                            pageCount = { viewerImages.size }
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            var scale by remember { mutableFloatStateOf(1f) }
                            var offsetX by remember { mutableFloatStateOf(0f) }
                            var offsetY by remember { mutableFloatStateOf(0f) }

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
                                    painter = rememberAsyncImagePainter(viewerImages[page]),
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
                            onClick = { showImageViewer = false },
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

                        Text(
                            text = "${pagerState.currentPage + 1} / ${viewerImages.size}",
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
        }
    }
}