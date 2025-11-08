package com.example.karyanusa.component.galeri

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun GaleriPribadiPage(navController: NavController) {
    val karyaList = remember { KaryaRepository.daftarKarya }
    var showDialog by remember { mutableStateOf(false) }
    var karyaDihapus by remember { mutableStateOf<Karya?>(null) }

    val pinkTua = Color(0xFF4A0E24)
    val background = Color(0xFFFFF5F7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(8.dp)
    ) {
        // 🔼 Tombol Upload Karya Baru
        Button(
            onClick = { navController.navigate("upload") },
            colors = ButtonDefaults.buttonColors(containerColor = pinkTua),
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Upload", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Upload Karya Baru", color = Color.White)
        }

        // 📸 Daftar Karya Pribadi
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            if (karyaList.isEmpty()) {
                item {
                    Text(
                        "Belum ada karya yang diunggah.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(karyaList) { karya ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { /* bisa tambahkan detail */ },
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = karya.nama,
                                    fontWeight = FontWeight.Bold,
                                    color = pinkTua
                                )
                                Text(
                                    text = karya.deskripsi,
                                    color = Color.Gray
                                )
                            }

                            // ✏️ Edit
                            IconButton(onClick = {
                                navController.navigate("edit/${karya.id}")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = pinkTua
                                )
                            }

                            // 🗑️ Hapus
                            IconButton(onClick = {
                                karyaDihapus = karya
                                showDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 🩷 Dialog Konfirmasi Hapus
    if (showDialog && karyaDihapus != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        KaryaRepository.daftarKarya.remove(karyaDihapus)
                        showDialog = false
                        karyaDihapus = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pinkTua)
                ) {
                    Text("Ya, Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            title = {
                Text(
                    "Konfirmasi Hapus",
                    fontWeight = FontWeight.Bold,
                    color = pinkTua
                )
            },
            text = { Text("Apakah kamu yakin ingin menghapus karya ini?") },
            containerColor = Color(0xFFFFE4EC),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        )
    }
}
