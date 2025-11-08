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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailPage(
    navController: NavController,
    questionId: Int
) {
    val questionState = remember { mutableStateOf(ForumData.questions.find { it.id == questionId }) }
    val question = questionState.value

    if (question == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pertanyaan tidak ditemukan", color = Color.Gray)
        }
        return
    }

    val context = LocalContext.current
    var replyText by remember { mutableStateOf("") }
    var showImagePopup by remember { mutableStateOf(false) }
    val replyImages = remember { mutableStateListOf<Uri>() }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            if (replyImages.size < 4) replyImages.add(cameraImageUri!!)
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
            val total = replyImages.size + uris.size
            if (total > 4) {
                Toast.makeText(context, "Maksimal 4 gambar", Toast.LENGTH_SHORT).show()
                val allowed = 4 - replyImages.size
                if (allowed > 0) replyImages.addAll(uris.take(allowed))
            } else replyImages.addAll(uris)
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 1.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFE4EC), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD9D9D9))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(question.displayName, fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24))
                                    Text("${question.username} • ${question.time}", color = Color.Gray, fontSize = 13.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Surface(color = Color(0xFF4A0E24), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        "${question.replies} Balasan",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(question.question, fontSize = 15.sp, color = Color.Black)

                            if (question.imageUris.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                if (question.imageUris.size == 1) {
                                    Image(
                                        painter = rememberAsyncImagePainter(question.imageUris.first()),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = 300.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.LightGray)
                                            .clickable {
                                                viewerImages = question.imageUris
                                                selectedImageIndex = 0
                                                showImageViewer = true
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        userScrollEnabled = false
                                    ) {
                                        itemsIndexed(question.imageUris.take(4)) { index, uri ->
                                            Image(
                                                painter = rememberAsyncImagePainter(uri),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.LightGray)
                                                    .clickable {
                                                        viewerImages = question.imageUris
                                                        selectedImageIndex = index
                                                        showImageViewer = true
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    IconButton(onClick = {
                                        if (replyText.isEmpty()) Toast.makeText(context, "Isi balasan dulu", Toast.LENGTH_SHORT).show()
                                        else showImagePopup = true
                                    }) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF4A0E24))
                                    }

                                    IconButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
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
                                            if (replyText.isEmpty()) Text("Tulis balasan...", color = Color.Gray)
                                            inner()
                                        }
                                    )

                                    IconButton(
                                        onClick = {
                                            if (replyText.isNotBlank()) {
                                                ForumData.addReplyDetail(
                                                    questionId = question.id,
                                                    text = replyText,
                                                    imageUris = replyImages.map { it.toString() }
                                                )
                                                replyText = ""
                                                replyImages.clear()
                                                questionState.value = ForumData.questions.find { it.id == questionId }
                                                Toast.makeText(context, "Balasan terkirim!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", tint = Color(0xFF4A0E24))
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
                                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                                    .size(22.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Balasan", color = Color(0xFF4A0E24), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                items(question.replyList) { reply ->
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
                                Text(reply.displayName, fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24))
                                Text(reply.username + " • " + reply.time, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(reply.text, fontSize = 14.sp, color = Color.Black)
                        if (reply.imageUris.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(reply.imageUris) { index, uri ->
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewerImages = reply.imageUris
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
            // 🔹 Floating kategori di pojok kiri bawah
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF5F5F5),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
            ) {
                Text(
                    text = question.category,
                    color = Color(0xFFFFB3C1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // Popup dan viewer (tidak diubah)
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
                                        Icons.Default.Close, null, tint = Color(0xFF4A0E24),
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable { showImagePopup = false }
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

