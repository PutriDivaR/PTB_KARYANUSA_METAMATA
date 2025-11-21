package com.example.karyanusa.component.galeri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    var loading by remember { mutableStateOf(true) }
    var karya by remember { mutableStateOf<KaryaData?>(null) }

    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }

    // 🔥 Load data dari server berdasarkan ID
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getKarya().enqueue(object : Callback<KaryaResponse> {
            override fun onResponse(
                call: Call<KaryaResponse>,
                response: Response<KaryaResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == true) {
                    val all = response.body()!!.data
                    karya = all.find { it.galeri_id == karyaId }

                    karya?.let {
                        nama = it.judul
                        deskripsi = it.caption
                    }
                }
                loading = false
            }

            override fun onFailure(call: Call<KaryaResponse>, t: Throwable) {
                loading = false
            }
        })
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (karya == null) {
        Text(
            text = "Karya tidak ditemukan.",
            color = Color.Red,
            modifier = Modifier.padding(20.dp)
        )
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (nama.isBlank() || deskripsi.isBlank()) {
                        pesan = "⚠ Nama & deskripsi tidak boleh kosong"
                        return@Button
                    }

                    // 🔥 Kirim update ke server
                    val body = HashMap<String, RequestBody>()
                    body["nama"] = nama.toRequestBody("text/plain".toMediaTypeOrNull())
                    body["deskripsi"] = deskripsi.toRequestBody("text/plain".toMediaTypeOrNull())

                    RetrofitClient.instance.updateKarya(karyaId, body)
                        .enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(
                                call: Call<SimpleResponse>,
                                response: Response<SimpleResponse>
                            ) {
                                if (response.isSuccessful && response.body()?.status == true) {
                                    pesan = "Berhasil diperbarui!"

                                    navController.popBackStack() // kembali
                                } else {
                                    pesan = "Gagal update"
                                }
                            }

                            override fun onFailure(
                                call: Call<SimpleResponse>,
                                t: Throwable
                            ) {
                                pesan = "Error koneksi"
                            }
                        })
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
            ) {
                Text("Simpan Perubahan", color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            // kalo dia batal ga jadi edit
            OutlinedButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4A0E24)
                )
            ) {
                Text("Batal")
            }

            if (pesan.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(pesan, color = Color(0xFF4A0E24))
            }
        }
    }
}
