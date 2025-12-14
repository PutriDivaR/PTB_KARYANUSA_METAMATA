package com.example.karyanusa.component.forum

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
fun ForumPage(navController: NavController) {

    // State untuk tab dan filter
    var selectedTab by remember { mutableStateOf("all") }
    var selectedFilter by remember { mutableStateOf("all") }

    // State untuk API
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var allQuestions by remember { mutableStateOf<List<ForumPertanyaanResponse>>(emptyList()) }

    // State untuk delete
    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()

    // ✅ PERBAIKAN: Ambil user ID dari token
    val currentUserId = tokenManager.getUserId()

    // Fungsi untuk load semua pertanyaan dari API
    fun loadAllQuestions() {
        isLoading = true
        errorMessage = null

        if (token.isNullOrEmpty()) {
            isLoading = false
            errorMessage = "Sesi Anda telah berakhir. Silakan login kembali."
            return
        }

        RetrofitClient.instance.getPertanyaan("Bearer $token")
            .enqueue(object : Callback<List<ForumPertanyaanResponse>> {
                override fun onResponse(
                    call: Call<List<ForumPertanyaanResponse>>,
                    response: Response<List<ForumPertanyaanResponse>>
                ) {
                    isLoading = false
                    if (response.isSuccessful) {
                        allQuestions = response.body() ?: emptyList()
                    } else if (response.code() == 401) {
                        errorMessage = "Autentikasi gagal. Silakan login kembali."
                    } else {
                        val errorText = response.errorBody()?.string()
                        errorMessage = "Gagal memuat data (${response.code()}): $errorText"
                    }
                }

                override fun onFailure(call: Call<List<ForumPertanyaanResponse>>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Koneksi gagal: ${t.message}"
                }
            })
    }

    // ✅ Fungsi format tanggal
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

    // Load data pertama kali dan saat kembali dari halaman lain
    LaunchedEffect(navController.currentBackStackEntry) {
        loadAllQuestions()
    }

    // ✅ Filter pertanyaan berdasarkan tab dan filter (FIX: gunakan currentUserId dari token)
    val filteredQuestions = remember(allQuestions, selectedTab, selectedFilter, currentUserId) {
        allQuestions.filter { question ->
            // Filter berdasarkan tab
            val matchesTab = if (selectedTab == "my") {
                question.user_id == currentUserId
            } else {
                true
            }

            // Filter berdasarkan status jawaban
            val matchesFilter = when (selectedFilter) {
                "answered" -> !question.jawaban.isNullOrEmpty()
                "unanswered" -> question.jawaban.isNullOrEmpty()
                else -> true
            }

            matchesTab && matchesFilter
        }
    }

    // ✅ Hitung jumlah untuk filter chips (FIX: gunakan currentUserId dari token)
    val currentQuestions = remember(allQuestions, selectedTab, currentUserId) {
        if (selectedTab == "my") {
            allQuestions.filter { it.user_id == currentUserId }
        } else {
            allQuestions
        }
    }

    val allCount = currentQuestions.size
    val unansweredCount = currentQuestions.count { it.jawaban.isNullOrEmpty() }
    val answeredCount = currentQuestions.count { !it.jawaban.isNullOrEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Forum",
                            tint = Color(0xFF4A0E24),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Forum",
                            color = Color(0xFF4A0E24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("notifforum") }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("forum/add") },
                containerColor = Color(0xFF4A0E24),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Question")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF5F7))
        ) {
            // Tampilan Loading
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A0E24))
                }
                return@Scaffold
            }

            // Tampilan Error
            if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $errorMessage", color = Color.Red, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { loadAllQuestions() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
                return@Scaffold
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Tab atas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabButton(
                        text = "Semua Pertanyaan",
                        isSelected = selectedTab == "all",
                        onClick = { selectedTab = "all" }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    TabButton(
                        text = "Pertanyaan Saya",
                        isSelected = selectedTab == "my",
                        onClick = { selectedTab = "my" }
                    )
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        text = "All ($allCount)",
                        isSelected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        text = "Unanswered ($unansweredCount)",
                        isSelected = selectedFilter == "unanswered",
                        onClick = { selectedFilter = "unanswered" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        text = "Answered ($answeredCount)",
                        isSelected = selectedFilter == "answered",
                        onClick = { selectedFilter = "answered" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List Pertanyaan
                if (filteredQuestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == "my") "Kamu belum membuat pertanyaan" else "Belum ada pertanyaan",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredQuestions) { question ->
                            var expandedMenu by remember { mutableStateOf(false) }
                            val isMyQuestion = question.user_id == currentUserId

                            QuestionCard(
                                question = question,
                                isMyQuestion = isMyQuestion,
                                formatTanggal = { formatTanggal(it) },
                                onCardClick = {
                                    navController.navigate("forumDetail/${question.pertanyaan_id}")
                                },
                                expandedMenu = expandedMenu,
                                onMenuClick = { expandedMenu = !expandedMenu },
                                onDismissMenu = { expandedMenu = false },
                                onEditClick = {
                                    expandedMenu = false
                                    navController.navigate("editPertanyaan/${question.pertanyaan_id}")
                                },
                                onDeleteClick = {
                                    expandedMenu = false
                                    questionToDelete = question.pertanyaan_id
                                    showDeleteDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Delete Dialog
            if (showDeleteDialog && questionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = {
                        Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text("Apakah kamu yakin ingin menghapus pertanyaan ini?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            // TODO: Implementasi API delete
                            Toast.makeText(context, "Fitur hapus belum tersedia", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            questionToDelete = null
                        }) {
                            Text("Ya", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            questionToDelete = null
                        }) {
                            Text("Batal", color = Color.Gray)
                        }
                    },
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                )
            }
        }
    }
}

// Sub-komponen
@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (isSelected) Color(0xFF4A0E24) else Color.Gray,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 16.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    )
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFFFB3C1) else Color.White,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3C1)) else null
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
fun QuestionCard(
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4EC))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Avatar, Nama, Username, dan Titik Tiga
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.DarkGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (question.user?.nama?.firstOrNull() ?: "?").toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = question.user?.nama ?: "Anonymous",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A0E24),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@${question.user?.username ?: "user"} · ${formatTanggal(question.tanggal)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Titik Tiga di Pojok Kanan Atas (hanya untuk pertanyaan saya)
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

            // Isi Pertanyaan
            Text(
                text = question.isi,
                fontSize = 14.sp,
                color = Color(0xFF4A0E24),
                lineHeight = 20.sp
            )

            // Gambar (jika ada)
            if (!question.image_forum.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(question.image_forum),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Balasan
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