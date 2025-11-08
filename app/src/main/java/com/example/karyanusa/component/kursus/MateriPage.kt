package com.example.karyanusa.component.kursus


import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.R
import com.example.karyanusa.network.Kursus
import com.example.karyanusa.network.Materi
import com.example.karyanusa.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriPage(navController: NavController, kursusId: Int) {
    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ambil data kursus n materi
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getCourses().enqueue(object : Callback<List<Kursus>> {
            override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                if (response.isSuccessful) kursusList = response.body() ?: emptyList()
                isLoading = false
            }

            override fun onFailure(call: Call<List<Kursus>>, t: Throwable) {
                isLoading = false
            }
        })

        RetrofitClient.instance.getMateriByKursus(kursusId).enqueue(object : Callback<List<Materi>> {
            override fun onResponse(call: Call<List<Materi>>, response: Response<List<Materi>>) {
                if (response.isSuccessful) materiList = response.body() ?: emptyList()
            }

            override fun onFailure(call: Call<List<Materi>>, t: Throwable) {
                println("Gagal ambil materi: ${t.message}")
            }
        })
    }

    val kursus = kursusList.find { it.kursus_id == kursusId }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Button(
                    onClick = { /* aksi download sertifikat */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.karyanusalogo),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unduh Sertifikat", color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF5F7))
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                kursus != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(R.drawable.tessampul),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = kursus.judul,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${materiList.size} Bagian",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 🎬 Daftar video materi
                        materiList.forEach { materi ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9EE)),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Video
                                    AndroidView(
                                        factory = {
                                            VideoView(context).apply {
                                                val uri =
                                                    "http://10.0.2.2:8000/video/${materi.video}".toUri()
                                                setVideoURI(uri)
                                                val mediaController = MediaController(context)
                                                mediaController.setAnchorView(this)
                                                setMediaController(mediaController)
                                            }
                                        },
                                        update = { view ->
                                            view.setOnPreparedListener { it.start() }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(materi.judul, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Durasi: ${materi.durasi} menit",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
                else -> Text("Data kursus tidak ditemukan", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

