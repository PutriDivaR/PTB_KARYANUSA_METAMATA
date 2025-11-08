package com.example.karyanusa.component.galeri

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.R

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaleriPublikPage(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<Karya?>(null) }

    val karyaList by remember { mutableStateOf(KaryaRepository.daftarKarya) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F7))
            .padding(12.dp)
    ) {
        Column {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Cari karya...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(karyaList.filter { it.nama.contains(search, ignoreCase = true) }) { karya ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { selectedItem = karya },
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column {
                            // ✅ tampilkan gambar dari kamera atau galeri
                            if (karya.gambarBitmap != null) {
                                Image(
                                    bitmap = karya.gambarBitmap.asImageBitmap(),
                                    contentDescription = karya.nama,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = karya.gambarUri ?: R.drawable.sample_karya
                                    ),
                                    contentDescription = karya.nama,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    karya.nama,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4A0E24)
                                )
                                Text(
                                    "oleh ${karya.uploader}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF7A4E5A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ✅ tampilkan dialog detail di atas semua elemen
        if (selectedItem != null) {
            DetailKaryaDialog(
                karya = selectedItem!!,
                onDismiss = { selectedItem = null }
            )
        }
    }
}
