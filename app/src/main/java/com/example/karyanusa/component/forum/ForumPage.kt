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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Notifications


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumPage(navController: NavController) {
    var selectedTab by remember { mutableStateOf("all") }
    var selectedFilter by remember { mutableStateOf("all") }
    var replyingTo by remember { mutableStateOf<Int?>(null) }
    var replyText by remember { mutableStateOf("") }

    val allQuestions = ForumData.questions

    // Filter pertanyaan
    val filteredQuestions = allQuestions.filter { question ->
        val matchesTab = if (selectedTab == "my") question.isMyQuestion else true
        val matchesFilter = when (selectedFilter) {
            "answered" -> question.isAnswered
            "unanswered" -> !question.isAnswered
            else -> true
        }
        matchesTab && matchesFilter
    }

    // Hitung jumlah filter
    val currentQuestions = if (selectedTab == "my") {
        allQuestions.filter { it.isMyQuestion }
    } else {
        allQuestions
    }
    val allCount = currentQuestions.size
    val unansweredCount = currentQuestions.count { !it.isAnswered }
    val answeredCount = currentQuestions.count { it.isAnswered }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Int?>(null) }


    // Update status answered otomatis
    LaunchedEffect(allQuestions) {
        allQuestions.forEachIndexed { index, question ->
            val shouldBeAnswered = question.replies > 0
            if (question.isAnswered != shouldBeAnswered) {
                allQuestions[index] = question.copy(isAnswered = shouldBeAnswered)
            }
        }
    }

    // Tambah balasan
    fun addReply(questionId: Int) {
        val index = allQuestions.indexOfFirst { it.id == questionId }
        if (index != -1) {
            val updated = allQuestions[index].copy(
                replies = allQuestions[index].replies + 1,
                isAnswered = true
            )
            allQuestions[index] = updated
        }
    }

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
            Column(modifier = Modifier.fillMaxSize()) {
                // 🔹 Tab atas
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

                // 🔹 Filter Chips
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

                // 🔹 List Pertanyaan
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(filteredQuestions) { question ->
                        var expandedMenu by remember { mutableStateOf(false) }

                        QuestionCard(
                            question = question,
                            isReplying = replyingTo == question.id,
                            replyText = replyText,
                            onReplyClick = {
                                replyingTo = if (replyingTo == question.id) null else question.id
                                replyText = ""
                            },
                            onReplyTextChange = { replyText = it },
                            onSendReply = {
                                if (replyText.isNotBlank()) {
                                    addReply(question.id)
                                    replyingTo = null
                                    replyText = ""
                                }
                            },
                            onCardClick = {
                                navController.navigate("forumDetail/${question.id}")
                            },
                            expandedMenu = expandedMenu,
                            onMenuClick = { expandedMenu = !expandedMenu },
                            onDismissMenu = { expandedMenu = false },
                            onEditClick = {
                                expandedMenu = false
                                navController.navigate("editPertanyaan/${question.id}")
                            },
                            onDeleteClick = {
                                expandedMenu = false
                                questionToDelete = question.id
                                showDeleteDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
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

        }
    }
}


// 🔸 Sub-komponen
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
    question: Question,
    isReplying: Boolean,
    replyText: String,
    onReplyClick: () -> Unit,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
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

            // 🔹 Header: Avatar, Nama, Username, dan Titik Tiga
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
                        text = question.displayName.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = question.displayName,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A0E24),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "@${question.username} · ${question.time}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // 🔹 Titik Tiga di Pojok Kanan Atas
                if (question.isMyQuestion) {
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

            // 🔹 Isi Pertanyaan
            Text(
                text = question.question,
                fontSize = 14.sp,
                color = Color(0xFF4A0E24),
                lineHeight = 20.sp
            )

            if (question.imageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                if (question.imageUris.size == 1) {
                    // 1 gambar - full width
                    Image(
                        painter = rememberAsyncImagePainter(question.imageUris.first()),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Banyak gambar - tampil dalam grid
                    val columnCount = if (question.imageUris.size == 2) 2 else 2 // tampil 2 kolom
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false // biar tidak perlu scroll dalam card
                    ) {
                        items(question.imageUris.take(4)) { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Balasan & Kategori (icon balasan bisa diklik)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onReplyClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Balasan",
                        tint = Color(0xFF4A0E24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${question.replies} Balasan",
                        fontSize = 13.sp,
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = question.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 🔹 Input Balasan (muncul ketika diklik)
            if (isReplying) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = onReplyTextChange,
                        placeholder = { Text("Tulis balasan...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4A0E24),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSendReply,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF4A0E24), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


