package com.example.karyanusa.component.beranda

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.component.galeri.KaryaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerandaPage(navController: NavController) {
    val pinkMuda = Color(0xFFFFE4EC)
    val pinkTua = Color(0xFF4A0E24)
    val abuMuda = Color(0xFFECECEC)

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
                            contentDescription = "Chat",
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
                    IconButton(onClick = { navController.navigate("profile")}) {
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

            // 🔹 Header
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
                        "JEON WONWOO",
                        color = pinkTua,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 🔹 FEATURED CLASS
            SectionHeader(title = "FEATURED CLASS", onSeeAll = { navController.navigate("kursus")  })
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(2) {
                    Card(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(width = 160.dp, height = 120.dp),
                        colors = CardDefaults.cardColors(containerColor = abuMuda)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("gambar", color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 MY CLASSES
            SectionHeader(title = "MY CLASSES", onSeeAll = {  navController.navigate("profile") })
            Column(Modifier.padding(horizontal = 16.dp)) {
                listOf(
                    "PENJAHITAN BATIK",
                    "AYAMAN TAS",
                    "VAS BUNGA"
                ).forEachIndexed { index, nama ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = pinkMuda)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(nama, color = pinkTua, fontWeight = FontWeight.Medium)
                            Text("${(index + 1) * 4}/12 Bagian", color = pinkTua)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 MY GALLERY
            SectionHeader(
                title = "MY GALLERY",
                onSeeAll = { navController.navigate("galeri") })

            if (KaryaRepository.daftarKarya.isEmpty()) {
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
                    modifier = Modifier.height(200.dp)
                ) {
                    items(KaryaRepository.daftarKarya) { karya ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECECEC))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(karya.gambarUri),
                                contentDescription = karya.nama,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
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

