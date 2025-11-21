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
import com.example.karyanusa.network.KaryaData
import com.example.karyanusa.network.KaryaResponse
import com.example.karyanusa.network.RetrofitClient
import com.example.karyanusa.network.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun GaleriPribadiPage(navController: NavController) {

    var karyaList by remember { mutableStateOf<List<KaryaData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

// 🔥 untuk delete
    var showDialog by remember { mutableStateOf(false) }
    var karyaDihapus by remember { mutableStateOf<KaryaData?>(null) }

    val pinkTua = Color(0xFF4A0E24)
    val background = Color(0xFFFFF5F7)

// ============================================================
// LOAD DATA DARI SERVER
// ============================================================
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getMyKarya().enqueue(object : Callback<KaryaResponse> {
            override fun onResponse(
                call: Call<KaryaResponse>,
                response: Response<KaryaResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == true) {
                    karyaList = response.body()?.data ?: emptyList()
                }
                loading = false
            }

            override fun onFailure(call: Call<KaryaResponse>, t: Throwable) {
                loading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(8.dp)
    ) {

        // Tombol Upload
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

        // Loading
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Jika masih kosong
        if (karyaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada karya kamu",
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
            return@Column
        }

        // ============================================================
        // LIST KARYA
        // ============================================================
        LazyColumn {
            items(karyaList) { karya ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {},
                    elevation = CardDefaults.cardElevation(6.dp),
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
                                text = karya.judul,
                                fontWeight = FontWeight.Bold,
                                color = pinkTua
                            )
                            Text(
                                text = karya.caption,
                                color = Color.Gray
                            )
                        }

                        // Tombol hapus
                        IconButton(
                            onClick = {
                                navController.navigate("edit/${karya.galeri_id}")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = pinkTua
                            )
                        }

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

// ============================================================
// DIALOG HAPUS
// ============================================================
    if (showDialog && karyaDihapus != null) {

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {

                        // Panggil API delete
                        RetrofitClient.instance.deleteKarya(karyaDihapus!!.galeri_id)
                            .enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(
                                    call: Call<SimpleResponse>,
                                    response: Response<SimpleResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.status == true) {

                                        // hapus dari list UI
                                        karyaList = karyaList.filter {
                                            it.galeri_id != karyaDihapus!!.galeri_id
                                        }
                                    }

                                    showDialog = false
                                    karyaDihapus = null
                                }

                                override fun onFailure(
                                    call: Call<SimpleResponse>,
                                    t: Throwable
                                ) {
                                    showDialog = false
                                    karyaDihapus = null
                                }
                            })
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