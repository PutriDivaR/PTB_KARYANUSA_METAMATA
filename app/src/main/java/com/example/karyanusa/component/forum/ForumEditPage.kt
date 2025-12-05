package com.example.karyanusa.component.forum

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.ForumPertanyaanResponse
import com.example.karyanusa.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    var selectedCategory by remember { mutableStateOf("") }
    val editableImages = remember { mutableStateListOf<Uri>() }

    // State untuk UI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val kategoriList = listOf("Anyaman", "Batik", "Tenun", "Kerajinan Lain", "Lainnya")

    // ✅ Fungsi untuk load detail pertanyaan dari API
    fun loadQuestionForEdit() {
        isLoading = true
        errorMessage = null

        val token = tokenManager.getToken()

        // Cek token dulu
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
                            selectedCategory = "Anyaman"

                            // Load gambar ke list editableImages
                            editableImages.clear()
                            if (!data.image_forum.isNullOrEmpty()) {
                                editableImages.add(data.image_forum.toUri())
                            }

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

    // ✅ Fungsi untuk update pertanyaan
    fun updateQuestion() {
        isSaving = true

        // TODO: Implementasi API update jika ada endpoint-nya
        Toast.makeText(context, "Fitur update belum tersedia di API", Toast.LENGTH_SHORT).show()
        isSaving = false
        showConfirmDialog = false
    }

    // ✅ Fungsi format tanggal
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

    // Load data pertama kali
    LaunchedEffect(questionId) {
        loadQuestionForEdit()
    }

    // Camera & Image Picker Launchers
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
                            val imageChanged = editableImages.map { it.toString() } !=
                                    listOfNotNull(questionData!!.image_forum)

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
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.Home, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("gallery") }) {
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
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFFFFE4EC), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFD9D9D9))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    data.user?.nama ?: "Anonymous",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A0E24)
                                )
                                Text(
                                    "${data.user?.email ?: "@user"} • ${formatTanggal(data.tanggal)}",
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
                        BasicTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            decorationBox = { inner ->
                                if (questionText.isEmpty()) {
                                    Text("Tulis pertanyaan...", color = Color.Gray)
                                }
                                inner()
                            },
                            enabled = !isSaving
                        )

                        Spacer(Modifier.height(12.dp))
                        if (editableImages.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.heightIn(max = 300.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                userScrollEnabled = false
                            ) {
                                itemsIndexed(editableImages) { index, uri ->
                                    Box {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewerImages = editableImages.map { it.toString() }
                                                    selectedImageIndex = index
                                                    showImageViewer = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { editableImages.remove(uri) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                .size(24.dp),
                                            enabled = !isSaving
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { imagePicker.launch("image/*") },
                                    enabled = !isSaving
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF4A0E24))
                                }
                                IconButton(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    enabled = !isSaving
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF4A0E24))
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
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
                        Column(
                            Modifier.fillMaxWidth()
                                .background(Color(0xFFFFE4EC), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFD9D9D9))
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        reply.user?.nama ?: "Anonymous",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4A0E24)
                                    )
                                    Text(
                                        "${reply.user?.email ?: "@user"} • ${formatTanggal(reply.tanggal)}",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(reply.isi, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF5F5F5),
                    shadowElevation = 6.dp,
                    modifier = Modifier.clickable {
                        if (!isSaving) showCategoryDialog = true
                    }
                ) {
                    Text(
                        text = selectedCategory,
                        color = Color(0xFFFFB3C1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }

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

            if (showCategoryDialog) {
                Dialog(onDismissRequest = { showCategoryDialog = false }) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("Pilih Kategori", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(12.dp))
                            kategoriList.forEach { kategori ->
                                Text(
                                    text = kategori,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedCategory = kategori
                                        showCategoryDialog = false
                                    }.padding(vertical = 10.dp),
                                    color = if (kategori == selectedCategory) Color(0xFF4A0E24) else Color.Black,
                                    fontWeight = if (kategori == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isSaving) showConfirmDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = { updateQuestion() },
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

            if (showImageViewer) {
                Dialog(onDismissRequest = { showImageViewer = false }) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
                                .size(40.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}