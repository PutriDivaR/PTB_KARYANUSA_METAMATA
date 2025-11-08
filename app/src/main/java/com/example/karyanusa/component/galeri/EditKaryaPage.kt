package com.example.karyanusa.component.galeri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKaryaPage(
    navController: NavController,
    karyaId: Int
) {
    // ✅ Cari karya berdasarkan ID di Repository
    val karya = KaryaRepository.daftarKarya.find { it.id == karyaId }

    // Jika tidak ditemukan, tampilkan pesan error
    if (karya == null) {
        Text(
            text = "Karya tidak ditemukan.",
            color = Color.Red,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        )
        return
    }

    var nama by remember { mutableStateOf(karya.nama) }
    var deskripsi by remember { mutableStateOf(karya.deskripsi) }
    var pesan by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Karya", fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFE4EC))
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("© Karyanusa", fontSize = 12.sp, color = Color(0xFF4A0E24))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFFFF5F7))
                .padding(20.dp)
        ) {
            Text("Edit Data Karya", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24))
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Karya") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (nama.isNotBlank() && deskripsi.isNotBlank()) {
                        // ✅ Update data langsung ke repository
                        karya.nama = nama
                        karya.deskripsi = deskripsi
                        pesan = "✅ Perubahan berhasil disimpan!"

                        // Kembali ke halaman sebelumnya
                        navController.popBackStack()
                    } else {
                        pesan = "⚠️ Nama dan deskripsi harus diisi!"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
            ) {
                Text("Simpan Perubahan", color = Color.White)
            }

            if (pesan.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(pesan, color = if (pesan.contains("berhasil")) Color(0xFF4A0E24) else Color.Red)
            }
        }
    }
}
