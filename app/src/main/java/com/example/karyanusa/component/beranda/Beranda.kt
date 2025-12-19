package com.example.karyanusa.component.beranda

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.R
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.data.viewmodel.BerandaViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaPage(
    navController: NavController,
    viewModel: BerandaViewModel = viewModel()
) {
    val pinkMuda = Color(0xFFFFE4EC)
    val pinkTua = Color(0xFF4A0E24)

    val context = LocalContext.current
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken()
    val userId = tokenManager.getUserId()
    val userName = tokenManager.getUserName() ?: "User"

    // Collect data dari ViewModel
    val kursusList by viewModel.kursusList.collectAsState()
    val enrollmentsList by viewModel.enrollmentsList.collectAsState()
    val myKaryaList by viewModel.myKaryaList.collectAsState()

    val isLoadingKursus by viewModel.isLoadingKursus.collectAsState()
    val isLoadingEnrollments by viewModel.isLoadingEnrollments.collectAsState()
    val isLoadingKarya by viewModel.isLoadingKarya.collectAsState()

    // Load data saat pertama kali
    LaunchedEffect(Unit) {
        viewModel.loadAllData(token, userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Beranda",
                        color = pinkTua,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("notifikasi") }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = pinkTua
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pinkMuda
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = pinkMuda) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "forum",
                            tint = pinkTua
                        )
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Kursus",
                            tint = pinkTua
                        )
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "galeri", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = pinkTua)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {

            // Header dengan nama user
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pinkMuda)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Hi, Welcome Back", color = pinkTua, fontSize = 14.sp)
                    Text(
                        userName,
                        color = pinkTua,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // FEATURED CLASS
            SectionHeader(title = "FEATURED CLASS", onSeeAll = { navController.navigate("kursus") })

            if (isLoadingKursus && kursusList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = pinkTua)
                }
            } else if (kursusList.isEmpty()) {
                Text(
                    text = "Belum ada kursus tersedia",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(kursusList.take(5)) { kursus ->
                        Card(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .width(200.dp)
                                .height(140.dp)
                                .clickable {
                                    navController.navigate("detail/${kursus.kursus_id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = kursus.thumbnail,
                                        placeholder = painterResource(R.drawable.tessampul),
                                        error = painterResource(R.drawable.tessampul)
                                    ),
                                    contentDescription = kursus.judul,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = kursus.judul,
                                        fontWeight = FontWeight.Bold,
                                        color = pinkTua,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = kursus.pengrajin_nama,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // MY CLASSES
            SectionHeader(title = "MY CLASSES", onSeeAll = { navController.navigate("profile") })

            if (isLoadingEnrollments && enrollmentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = pinkTua)
                }
            } else if (token == null) {
                Text(
                    text = "Login untuk melihat kelas Anda",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else if (enrollmentsList.isEmpty()) {
                Text(
                    text = "Anda belum mengambil kelas",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    enrollmentsList.take(3).forEach { enrollment ->
                        val kursus = kursusList.find { it.kursus_id == enrollment.kursus_id }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    navController.navigate("materi/${enrollment.kursus_id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = pinkMuda)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        (kursus?.judul ?: "Kursus").uppercase(),
                                        color = pinkTua,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${enrollment.progress}%",
                                        color = pinkTua,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { enrollment.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = Color(0xFF7A3E48),
                                    trackColor = Color.LightGray,
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    enrollment.status.uppercase(),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // MY GALLERY
            SectionHeader(
                title = "MY GALLERY",
                onSeeAll = { navController.navigate("galeri") }
            )

            if (isLoadingKarya && myKaryaList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = pinkTua)
                }
            } else if (token == null) {
                Text(
                    text = "Login untuk melihat galeri Anda",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else if (myKaryaList.isEmpty()) {
                Text(
                    text = "Belum ada karya diunggah",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(myKaryaList.take(6)) { karya ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable {
                                    // Navigate ke detail jika perlu
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = "http://10.0.2.2:8000/storage/${karya.gambar}",
                                        placeholder = painterResource(R.drawable.tessampul),
                                        error = painterResource(R.drawable.tessampul)
                                    ),
                                    contentDescription = karya.judul,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = karya.judul,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    val pinkTua = Color(0xFF4A0E24)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = pinkTua)
        TextButton(onClick = onSeeAll) {
            Text("See All", color = pinkTua, fontSize = 14.sp)
        }
    }
}