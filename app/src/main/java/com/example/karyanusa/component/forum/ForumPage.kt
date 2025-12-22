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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karyanusa.data.viewmodel.ForumViewModel
import com.example.karyanusa.data.viewmodel.ForumUiState
import com.example.karyanusa.data.viewmodel.DeleteState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.ForumPertanyaanResponse
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumPage(navController: NavController) {

    // State untuk tab dan filter
    var selectedTab by remember { mutableStateOf("all") }
    var selectedFilter by remember { mutableStateOf("all") }

    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()

    // ViewModel
    val viewModel: ForumViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    // State untuk delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }

    // ✅ PERBAIKAN: Ambil user ID dari token
    val currentUserId = tokenManager.getUserId()

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

    // ✅ BARU: Fungsi untuk cek apakah pertanyaan sudah diedit
    fun isQuestionEdited(tanggal: String, updatedAt: String?): Boolean {
        if (updatedAt == null || updatedAt.isEmpty()) return false
        if (tanggal == updatedAt) return false

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")

            val cleanedCreated = tanggal.replace(Regex("\\.\\d+Z"), "Z")
            val cleanedUpdated = updatedAt.replace(Regex("\\.\\d+Z"), "Z")

            val createdDate = format.parse(cleanedCreated)
            val updatedDate = format.parse(cleanedUpdated)

            updatedDate?.after(createdDate) == true
        } catch (e: Exception) {
            false
        }
    }

    // ✅ FIX: Lifecycle observer untuk auto-refresh saat kembali ke halaman
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh data setiap kali halaman muncul kembali
                if (!token.isNullOrEmpty()) {
                    viewModel.refreshPertanyaan(token)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load data pertama kali
    LaunchedEffect(token) {
        if (!token.isNullOrEmpty()) {
            viewModel.loadPertanyaan(token)
        }
    }

    // ✅ Filter pertanyaan berdasarkan tab dan filter (FIX: gunakan currentUserId dari token)
    val filteredQuestions = remember(uiState, selectedTab, selectedFilter, currentUserId) {
        viewModel.getFilteredQuestions(selectedTab, selectedFilter, currentUserId)
    }

    // ✅ Hitung jumlah untuk filter chips (FIX: gunakan currentUserId dari token)
    val filterCounts = remember(uiState, selectedTab, currentUserId) {
        viewModel.getFilterCounts(selectedTab, currentUserId)
    }

    val allCount = filterCounts.all
    val unansweredCount = filterCounts.unanswered
    val answeredCount = filterCounts.answered

    // Handle delete success/error
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                Toast.makeText(
                    context,
                    (deleteState as DeleteState.Success).message,
                    Toast.LENGTH_SHORT
                ).show()
                showDeleteDialog = false
                questionToDelete = null
                viewModel.resetDeleteState()
            }
            is DeleteState.Error -> {
                Toast.makeText(
                    context,
                    (deleteState as DeleteState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
                showDeleteDialog = false
                questionToDelete = null
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Forum",
                            color = Color(0xFF4A0E24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
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
            when (uiState) {
                is ForumUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4A0E24))
                    }
                }
                is ForumUiState.Error -> {
                    val errorMessage = (uiState as ForumUiState.Error).message
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: $errorMessage", color = Color.Red, fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (!token.isNullOrEmpty()) {
                                        viewModel.loadPertanyaan(token)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                is ForumUiState.Success -> {
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
                                        isEdited = isQuestionEdited(question.tanggal, question.updated_at),
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
                }
            }

            // ✅ Delete Dialog
            if (showDeleteDialog && questionToDelete != null) {
                val isDeleting = deleteState is DeleteState.Deleting

                AlertDialog(
                    onDismissRequest = {
                        if (!isDeleting) {
                            showDeleteDialog = false
                            questionToDelete = null
                            viewModel.resetDeleteState()
                        }
                    },
                    title = {
                        Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        when (deleteState) {
                            is DeleteState.Deleting -> {
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
                            }
                            else -> {
                                Text("Apakah kamu yakin ingin menghapus pertanyaan ini?")
                            }
                        }
                    },
                    confirmButton = {
                        if (!isDeleting) {
                            TextButton(onClick = {
                                if (!token.isNullOrEmpty()) {
                                    viewModel.deletePertanyaan(token, questionToDelete!!)
                                }
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
                                viewModel.resetDeleteState()
                            }) {
                                Text("Batal", color = Color.Gray)
                            }
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
    isEdited: Boolean,
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

            // Header: Avatar, Nama, Username, dan Menu
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
                                ImageRequest.Builder(LocalContext.current)
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
                        // Tampilkan inisial jika tidak ada foto
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
                    // Nama
                    Text(
                        text = question.user?.nama ?: "Anonymous",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A0E24),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(0.dp))
                    // Username
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

            // Isi Pertanyaan
            Text(
                text = question.isi,
                fontSize = 14.sp,
                color = Color(0xFF4A0E24),
                lineHeight = 20.sp
            )

            // Layout Gambar
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

            // Balasan dan Tanggal
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

                // ✅ BARU: Tanggal dengan badge "edited"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTanggal(question.tanggal),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    // Badge "edited" jika pertanyaan sudah diedit
                    if (isEdited) {
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
}