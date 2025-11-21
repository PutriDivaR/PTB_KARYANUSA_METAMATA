package com.example.karyanusa.component.beranda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifikasiPage(navController: NavController) {
    val pinkMuda = Color(0xFFFFE4EC)
    val pinkTua = Color(0xFF4A0E24)
    val notifikasiList = remember { NotifikasiRepository.daftarNotifikasi }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Notifikasi", color = pinkTua, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = pinkTua)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pinkMuda)
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
                    IconButton(onClick = { navController.navigate("chat") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Kursus", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Galeri", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = pinkTua)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF5F7))
                .padding(innerPadding)
        ) {
            if (notifikasiList.isEmpty()) {
                Text(
                    text = "Belum ada notifikasi.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(notifikasiList) { notif ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = notif.judul,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pinkTua
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = notif.pesan,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = notif.waktu,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}