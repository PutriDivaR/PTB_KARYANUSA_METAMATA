package com.example.karyanusa.component.galeri

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.network.KaryaData
import com.example.karyanusa.network.KaryaResponse
import com.example.karyanusa.network.RetrofitClient

@Composable
fun GaleriPublikPage(navController: NavController) {

    var search by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<KaryaData?>(null) }

    var karyaList by remember { mutableStateOf<List<KaryaData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getKarya().enqueue(object : Callback<KaryaResponse> {
            override fun onResponse(
                call: Call<KaryaResponse>,
                response: Response<KaryaResponse>
            ) {
                if (response.isSuccessful && response.body()?.status == true) {
                    karyaList = response.body()?.data ?: emptyList()
                }

                Log.d("API_DATA", response.body().toString())
                loading = false
            }

            override fun onFailure(call: Call<KaryaResponse>, t: Throwable) {
                loading = false
            }
        })
    }

// =====================================================================
// UI
// =====================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F7))
            .padding(12.dp)
    ) {

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {

            Column {

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Cari karya...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn {
                    items(
                        karyaList.filter {
                            it.judul.contains(search, ignoreCase = true)
                        }
                    ) { karya ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { selectedItem = karya }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = "http://10.0.2.2:8000/storage/${karya.gambar}"
                                ),
                                contentDescription = karya.judul,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column(Modifier.padding(12.dp)) {
                                Text(karya.judul, fontWeight = FontWeight.Bold)
                                karya.uploader_name?.let { Text(it, fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }

            if (selectedItem != null) {
                DetailKaryaDialog(
                    karya = selectedItem!!,
                    onDismiss = { selectedItem = null }
                )
            }
        }
    }
}