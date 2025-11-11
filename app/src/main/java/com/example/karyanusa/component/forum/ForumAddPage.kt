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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumAddPage(navController: NavController) {
    var question by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showImagePopup by remember { mutableStateOf(false) }
    val imageUris = remember { mutableStateListOf<Uri>() }

    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFilter by remember { mutableStateOf("all") }

    val filteredQuestions = ForumData.questions.filter { q ->
        val isMyQuestion = q.username == ForumData.currentUserUsername
        val matchesFilter = when (selectedFilter) {
            "answered" -> q.isAnswered
            "unanswered" -> !q.isAnswered
            else -> true
        }
        isMyQuestion && matchesFilter
    }

    // Kamera launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            if (imageUris.size < 4) imageUris.add(cameraImageUri!!)
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
            val total = imageUris.size + uris.size
            if (total > 4) {
                Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
                val allowed = 4 - imageUris.size
                if (allowed > 0) imageUris.addAll(uris.take(allowed))
            } else imageUris.addAll(uris)
        }
    }

    var expandedMenuId by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forum", fontSize = 20.sp, color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF4A0E24))
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
                    IconButton(onClick = { navController.navigate("home") }) { Icon(Icons.Default.Home, null, tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("forum") }) { Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("kursus") }) { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("galeri") }) { Icon(Icons.Default.AddAPhoto, null, tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("profile") }) { Icon(Icons.Default.Person, null, tint = Color(0xFF4A0E24)) }
                }
            }
        },
        containerColor = Color(0xFFFFF5F7)
    ) { innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)
        ) {
            // 🔹 Bagian input
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFD9D9D9)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ForumData.currentUserDisplayName, fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24))
                        Text(ForumData.currentUserUsername, color = Color.Gray, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .background(Color(0xFFFFE4EC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (question.isEmpty()) Text("Ketik pertanyaanmu di sini...", color = Color.Gray)
                        inner()
                    }
                )

                Spacer(Modifier.height(12.dp))
                if (imageUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUris) { uri ->
                            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUris.remove(uri) },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        .size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 🔹 Tambah Gambar (Popup kembali)
                        IconButton(onClick = {
                            if (question.isEmpty()) Toast.makeText(context, "Isi pertanyaan dulu", Toast.LENGTH_SHORT).show()
                            else showImagePopup = true
                        }) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF4A0E24)) }

                        IconButton(onClick = {
                            if (question.isEmpty()) Toast.makeText(context, "Isi pertanyaan dulu", Toast.LENGTH_SHORT).show()
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) { Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF4A0E24)) }

                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE4EC))
                            ) {
                                Text(category.ifEmpty { "Kategori" }, color = Color(0xFF4A0E24), fontSize = 14.sp)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Anyaman", "Batik", "Tenun", "Kerajinan Lain").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = {
                                        category = it
                                        expanded = false
                                    })
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (question.isEmpty() || category.isEmpty()) {
                                Toast.makeText(context, "Lengkapi semua data", Toast.LENGTH_SHORT).show()
                            } else {
                                ForumData.addQuestion(
                                    ForumData.currentUserDisplayName,
                                    ForumData.currentUserUsername,
                                    "Baru saja",
                                    question,
                                    category,
                                    imageUris.map { it.toString() }
                                )
                                question = ""
                                category = ""
                                imageUris.clear()
                                Toast.makeText(context, "Pertanyaan berhasil dipost!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Post", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Pertanyaan Saya", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip("All", selectedFilter == "all") { selectedFilter = "all" }
                Spacer(Modifier.width(8.dp))
                FilterChip("Unanswered", selectedFilter == "unanswered") { selectedFilter = "unanswered" }
                Spacer(Modifier.width(8.dp))
                FilterChip("Answered", selectedFilter == "answered") { selectedFilter = "answered" }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 List pertanyaan yang bisa discroll
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredQuestions) { q ->
                    QuestionCard(
                        question = q,
                        isReplying = false,
                        replyText = "",
                        onReplyClick = {},
                        onReplyTextChange = {},
                        onSendReply = {},
                        onCardClick = { navController.navigate("forumDetail/${q.id}") },
                        expandedMenu = expandedMenuId == q.id,
                        onMenuClick = { expandedMenuId = if (expandedMenuId == q.id) null else q.id },
                        onDismissMenu = { expandedMenuId = null },
                        onEditClick = {
                            expandedMenuId = null
                            navController.navigate("editPertanyaan/${q.id}")
                        },
                        onDeleteClick = {
                            expandedMenuId = null
                            questionToDelete = q.id
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        if (showDeleteDialog && questionToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold) },
                text = { Text("Apakah kamu yakin ingin menghapus pertanyaan ini?") },
                confirmButton = {
                    TextButton(onClick = {
                        ForumData.deleteQuestion(questionToDelete!!)
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

        // 🔹 Popup upload gambar kembali aktif
        if (showImagePopup) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))
                    .clickable { showImagePopup = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(300.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFD6E8)).padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                            Icon(
                                Icons.Default.Close, null, tint = Color(0xFF4A0E24),
                                modifier = Modifier.size(22.dp).clickable { showImagePopup = false }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Pilih gambar", color = Color(0xFF4A0E24), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Maksimal 4 gambar dapat diunggah.", color = Color(0xFF4A0E24).copy(alpha = 0.8f))
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                showImagePopup = false
                                imagePicker.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier.fillMaxWidth(0.8f).height(45.dp)
                        ) {
                            Text("Upload dari perangkat", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
