package com.example.karyanusa.component.galeri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.KaryaData
import com.example.karyanusa.network.KaryaResponse
import com.example.karyanusa.network.RetrofitClient
import com.example.karyanusa.network.SimpleResponse
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKaryaPage(
    navController: NavController,
    karyaId: Int
) {
    val context = LocalContext.current
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken()

    var loading by remember { mutableStateOf(true) }
    var karya by remember { mutableStateOf<KaryaData?>(null) }
    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

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

    // Load data karya
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getMyKarya("Bearer $token")
            .enqueue(object : Callback<KaryaResponse> {
                override fun onResponse(
                    call: Call<KaryaResponse>,
                    response: Response<KaryaResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        val allKarya = response.body()?.data ?: emptyList()
                        karya = allKarya.find { it.galeri_id == karyaId }

                        karya?.let {
                            nama = it.judul
                            deskripsi = it.caption
                        }
                    } else {
                        Toast.makeText(context, "Gagal memuat data karya", Toast.LENGTH_SHORT).show()
                    }
                    loading = false
                }

                override fun onFailure(call: Call<KaryaResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    loading = false
                }
            })
    }

    // Loading state
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4A0E24))
        }
        return
    }

    // Karya tidak ditemukan
    if (karya == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Karya tidak ditemukan atau Anda tidak memiliki akses",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
                ) {
                    Text("Kembali", color = Color.White)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Karya", fontWeight = FontWeight.Bold, color = Color(0xFF4A0E24))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFE4EC))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFFFF5F7))
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Karya") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A0E24),
                    focusedLabelColor = Color(0xFF4A0E24)
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                enabled = !isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A0E24),
                    focusedLabelColor = Color(0xFF4A0E24)
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (nama.isBlank() || deskripsi.isBlank()) {
                        Toast.makeText(context, "Nama dan deskripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSaving = true

                    val body = HashMap<String, RequestBody>()
                    body["nama"] = nama.toRequestBody("text/plain".toMediaTypeOrNull())
                    body["deskripsi"] = deskripsi.toRequestBody("text/plain".toMediaTypeOrNull())

                    RetrofitClient.instance.updateKarya("Bearer $token", karyaId, body)
                        .enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(
                                call: Call<SimpleResponse>,
                                response: Response<SimpleResponse>
                            ) {
                                isSaving = false
                                if (response.isSuccessful && response.body()?.status == true) {
                                    Toast.makeText(context, "Karya berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Gagal memperbarui karya", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                isSaving = false
                                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Simpan Perubahan", color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4A0E24)
                ),
                enabled = !isSaving
            ) {
                Text("Batal")
            }
        }
    }
}