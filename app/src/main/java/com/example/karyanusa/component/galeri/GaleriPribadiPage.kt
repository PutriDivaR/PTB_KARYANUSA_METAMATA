package com.example.karyanusa.component.galeri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.KaryaData
import com.example.karyanusa.network.KaryaResponse
import com.example.karyanusa.network.RetrofitClient
import com.example.karyanusa.network.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun GaleriPribadiPage(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken()

    var karyaList by remember { mutableStateOf<List<KaryaData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var karyaDihapus by remember { mutableStateOf<KaryaData?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val pinkTua = Color(0xFF4A0E24)
    val background = Color(0xFFFFF5F7)

    // Cek token dulu
    if (token == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
        return
    }

    // Load data karya pribadi
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getMyKarya("Bearer $token")
            .enqueue(object : Callback<KaryaResponse> {
                override fun onResponse(
                    call: Call<KaryaResponse>,
                    response: Response<KaryaResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        karyaList = response.body()?.data ?: emptyList()
                    } else {
                        Toast.makeText(context, "Gagal memuat galeri", Toast.LENGTH_SHORT).show()
                    }
                    loading = false
                }

                override fun onFailure(call: Call<KaryaResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
                CircularProgressIndicator(color = pinkTua)
            }
            return@Column
        }

        // Kosong
        if (karyaList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Belum ada karya Anda",
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mulai unggah karya pertama Anda!",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            return@Column
        }

        // LIST KARYA
        LazyColumn {
            items(karyaList) { karya ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                                color = Color.Gray,
                                fontSize = 13.sp
                            )

                            // Tampilan Views
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Views",
                                    tint = Color(0xFF7A4E5A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${karya.views} views",
                                    color = Color(0xFF7A4E5A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Tombol Edit
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

                        // Tombol Hapus
                        IconButton(
                            onClick = {
                                karyaDihapus = karya
                                showDialog = true
                            }
                        ) {
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

    // Dialog Konfirmasi Hapus
    if (showDialog && karyaDihapus != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        RetrofitClient.instance.deleteKarya(
                            "Bearer $token",
                            karyaDihapus!!.galeri_id
                        ).enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(
                                call: Call<SimpleResponse>,
                                response: Response<SimpleResponse>
                            ) {
                                isDeleting = false
                                if (response.isSuccessful && response.body()?.status == true) {
                                    Toast.makeText(context, "Karya berhasil dihapus", Toast.LENGTH_SHORT).show()
                                    karyaList = karyaList.filter {
                                        it.galeri_id != karyaDihapus!!.galeri_id
                                    }
                                } else {
                                    Toast.makeText(context, "Gagal menghapus karya", Toast.LENGTH_SHORT).show()
                                }
                                showDialog = false
                                karyaDihapus = null
                            }

                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                isDeleting = false
                                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                karyaDihapus = null
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Ya, Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    enabled = !isDeleting
                ) {
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
            text = {
                Column {
                    Text("Apakah Anda yakin ingin menghapus karya ini?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"${karyaDihapus?.judul}\"",
                        fontWeight = FontWeight.SemiBold,
                        color = pinkTua
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tindakan ini tidak dapat dibatalkan.",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            },
            containerColor = Color(0xFFFFE4EC),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        )
    }
}