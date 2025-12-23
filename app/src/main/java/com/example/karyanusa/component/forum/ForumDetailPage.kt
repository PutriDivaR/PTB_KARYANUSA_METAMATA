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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import coil.request.ImageRequest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.example.karyanusa.network.SimpleResponse
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
    val currentUserId = tokenManager.getUserId()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var questionData by remember { mutableStateOf<ForumPertanyaanResponse?>(null) }

    var replyText by remember { mutableStateOf("") }
    val replyImages = remember { mutableStateListOf<Uri>() }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSendingReply by remember { mutableStateOf(false) }

    val maxReplyImages = 2

    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }

    var expandedMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }


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

    fun deleteQuestion() {
        isDeleting = true

        RetrofitClient.instance.deletePertanyaan("Bearer $token", questionId)
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
                        navController.popBackStack()
                    } else {
                        Toast.makeText(
                            context,
                            "Gagal menghapus: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showDeleteDialog = false
                }

                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    isDeleting = false
                    Toast.makeText(
                        context,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showDeleteDialog = false
                }
            })
    }

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

        val isiRequestBody = replyText.toRequestBody("text/plain".toMediaTypeOrNull())

        val imageParts = mutableListOf<MultipartBody.Part>()
        replyImages.forEachIndexed { index, uri ->
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val file = File(context.cacheDir, "temp_reply_${System.currentTimeMillis()}_$index.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image_jawaban", file.name, requestFile)
                    imageParts.add(part)
                }
            } catch (e: Exception) {
                Log.e("ForumDetailPage", "Error reading image $index: ${e.message}")
            }
        }

        val imagePart = imageParts.firstOrNull()

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
                    Toast.makeText(
                        context,
                        "Balasan terkirim!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("ForumDetailPage", "Balasan berhasil dikirim")

                    replyText = ""
                    replyImages.clear()
                    loadForumDetail()
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
                Toast.makeText(context, "❌ Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ForumDetailPage", "Failure", t)
            }
        })
    }

    fun formatTanggal(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val outputFormat = SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale("id", "ID")
            ).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }

            val date = inputFormat.parse(tanggal)
            outputFormat.format(date ?: Date())

        } catch (e: Exception) {
            tanggal
        }
    }


    fun isEdited(tanggal: String, updatedAt: String?): Boolean {
        if (updatedAt == null || updatedAt.isEmpty()) return false
        if (tanggal == updatedAt) return false

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")

            val createdDate = format.parse(tanggal)
            val updatedDate = format.parse(updatedAt)

            updatedDate?.after(createdDate) == true
        } catch (e: Exception) {
            try {
                val altFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                altFormat.timeZone = TimeZone.getTimeZone("UTC")

                val cleanedCreated = tanggal.replace(Regex("\\.\\d+Z"), "Z")
                val cleanedUpdated = updatedAt.replace(Regex("\\.\\d+Z"), "Z")

                val createdDate = altFormat.parse(cleanedCreated)
                val updatedDate = altFormat.parse(cleanedUpdated)

                updatedDate?.after(createdDate) == true
            } catch (e: Exception) {
                false
            }
        }
    }

    LaunchedEffect(questionId) {
        loadForumDetail()
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                if (replyImages.size < maxReplyImages) replyImages.add(cameraImageUri!!)
                else Toast.makeText(context, "Maksimal $maxReplyImages gambar", Toast.LENGTH_SHORT).show()
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
                val remainingSlots = maxReplyImages - replyImages.size

                if (remainingSlots > 0) {
                    val imagesToAdd = uris.take(remainingSlots)
                    replyImages.addAll(imagesToAdd)

                    if (uris.size > remainingSlots) {
                        Toast.makeText(
                            context,
                            "Hanya ${remainingSlots} gambar yang ditambahkan (maksimal $maxReplyImages gambar)",
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
                    Toast.makeText(context, "Maksimal $maxReplyImages gambar", Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = { navController.navigate("beranda") }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Kursus",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Galeri",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF4A0E24)
                        )
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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
                return@Scaffold
            }

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

            if (questionData == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pertanyaan tidak ditemukan", color = Color.Gray)
                }
                return@Scaffold
            }

            val data = questionData!!
            val isMyQuestion = data.user_id == currentUserId

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
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!data.user?.foto_profile.isNullOrEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    coil.request.ImageRequest.Builder(LocalContext.current)
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF4A0E24),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${data.jawaban?.size ?: 0} Balasan",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            )
                                        )
                                    }

                                    if (isMyQuestion) {
                                        Box {
                                            IconButton(onClick = { expandedMenu = !expandedMenu }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Menu",
                                                    tint = Color(0xFF4A0E24)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = expandedMenu,
                                                onDismissRequest = { expandedMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit") },
                                                    onClick = {
                                                        expandedMenu = false
                                                        navController.navigate("editPertanyaan/$questionId")
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Hapus") },
                                                    onClick = {
                                                        expandedMenu = false
                                                        showDeleteDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(data.isi, fontSize = 15.sp, color = Color.Black)

                            if (!data.image_forum.isNullOrEmpty()) {
                                val imageUrls = data.image_forum ?: emptyList()
                                Spacer(Modifier.height(8.dp))

                                when (imageUrls.size) {
                                    1 -> {
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrls[0]),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 200.dp, max = 300.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.LightGray)
                                                .clickable {
                                                    viewerImages = imageUrls
                                                    selectedImageIndex = 0
                                                    showImageViewer = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    2 -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            imageUrls.forEachIndexed { index, imageUrl ->
                                                Image(
                                                    painter = rememberAsyncImagePainter(imageUrl),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.LightGray)
                                                        .clickable {
                                                            viewerImages = imageUrls
                                                            selectedImageIndex = index
                                                            showImageViewer = true
                                                        },
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
                                                    .background(Color.LightGray)
                                                    .clickable {
                                                        viewerImages = imageUrls
                                                        selectedImageIndex = 0
                                                        showImageViewer = true
                                                    },
                                                contentScale = ContentScale.Crop
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                imageUrls.drop(1)
                                                    .forEachIndexed { index, imageUrl ->
                                                        Image(
                                                            painter = rememberAsyncImagePainter(
                                                                imageUrl
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(120.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = imageUrls
                                                                    selectedImageIndex = index + 1
                                                                    showImageViewer = true
                                                                },
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
                                                imageUrls.take(2)
                                                    .forEachIndexed { index, imageUrl ->
                                                        Image(
                                                            painter = rememberAsyncImagePainter(
                                                                imageUrl
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = imageUrls
                                                                    selectedImageIndex = index
                                                                    showImageViewer = true
                                                                },
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                imageUrls.drop(2).take(2)
                                                    .forEachIndexed { index, imageUrl ->
                                                        Image(
                                                            painter = rememberAsyncImagePainter(
                                                                imageUrl
                                                            ),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color.LightGray)
                                                                .clickable {
                                                                    viewerImages = imageUrls
                                                                    selectedImageIndex = index + 2
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

                            Spacer(Modifier.height(12.dp))

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
                                            if (replyImages.size < maxReplyImages) {
                                                imagePicker.launch("image/*")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Maksimal $maxReplyImages gambar",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            null,
                                            tint = Color(0xFF4A0E24)
                                        )
                                    }

                                    IconButton(onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Kamera",
                                            tint = Color(0xFF4A0E24)
                                        )
                                    }

                                    BasicTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        textStyle = TextStyle(
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        ),
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
                                                    .background(
                                                        Color.Black.copy(alpha = 0.4f),
                                                        CircleShape
                                                    )
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

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${replyImages.size}/$maxReplyImages gambar",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTanggal(data.tanggal),
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )

                                    if (isEdited(data.tanggal, data.updated_at)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFB3C1)
                                        ) {
                                            Text(
                                                text = "edited",
                                                fontSize = 9.sp,
                                                color = Color(0xFF4A0E24),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFFFB3C1)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (!reply.user?.foto_profile.isNullOrEmpty()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    coil.request.ImageRequest.Builder(LocalContext.current)
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

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatTanggal(reply.tanggal),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )

                                        if (isEdited(reply.tanggal, reply.updated_at)) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFB3C1)
                                            ) {
                                                Text(
                                                    text = "edited",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF4A0E24),
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
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

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isDeleting) {
                            showDeleteDialog = false
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
                            Text("Apakah kamu yakin ingin menghapus pertanyaan ini? Semua balasan juga akan terhapus.")
                        }
                    },
                    confirmButton = {
                        if (!isDeleting) {
                            TextButton(onClick = { deleteQuestion() }) {
                                Text("Ya", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!isDeleting) {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Batal", color = Color.Gray)
                            }
                        }
                    },
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                )
            }

            if (showImageViewer && viewerImages.isNotEmpty()) {
                ImageViewerDialog(
                    images = viewerImages,
                    initialPage = selectedImageIndex,
                    onDismiss = { showImageViewer = false }
                )
            }
        }
    }
}

@Composable
fun ImageViewerDialog(
    images: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { images.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
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