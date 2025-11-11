package com.example.karyanusa.component.kursus


import android.net.Uri
import android.widget.Toast
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
import com.example.karyanusa.component.auth.LoginTokenManager
import okhttp3.ResponseBody
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.example.karyanusa.network.EnrollmentCheckResponse
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriPage(navController: NavController, kursusId: Int) {
    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var enrollmentStatus by remember { mutableStateOf("none") } // none, ongoing, completed
    var isChecking by remember { mutableStateOf(true) }

    val tokenManager = LoginTokenManager(LocalContext.current)
    val token = tokenManager.getToken()

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

        if (token != null) {
            RetrofitClient.instance.checkEnrollment("Bearer $token", kursusId)
                .enqueue(object : Callback<EnrollmentCheckResponse> {
                    override fun onResponse(call: Call<EnrollmentCheckResponse>, response: Response<EnrollmentCheckResponse>) {
                        if (response.isSuccessful) {
                            val data = response.body()
                            if (data != null && data.enrolled) {
                                enrollmentStatus = data.status ?: "none"
                            }
                        }
                        isChecking = false
                    }

                    override fun onFailure(call: Call<EnrollmentCheckResponse>, t: Throwable) {
                        isChecking = false
                    }
                })
        } else {
            isChecking = false
        }
    }


    val kursus = kursusList.find { it.kursus_id == kursusId }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (enrollmentStatus == "completed") { // hanya tampil kalau kursus selesai
                BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                    val scope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            val userName = LoginTokenManager(context).getUserName() ?: "Peserta"
                            val kursusTitle = kursus?.judul ?: "Kursus"

                            scope.launch {
                                Toast.makeText(context, "Membuat sertifikat...", Toast.LENGTH_SHORT).show()
                                val savedUri = generateCertificatePdf(context, userName, kursusTitle)
                                if (savedUri != null) {
                                    Toast.makeText(context, "Sertifikat tersimpan.", Toast.LENGTH_LONG).show()
                                    openPdf(context, savedUri)
                                } else {
                                    Toast.makeText(context, "Gagal menyimpan sertifikat.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
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

                        // Daftar video materi
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
                                            val exoPlayer = ExoPlayer.Builder(context).build().apply {
                                                val mediaItem = MediaItem.fromUri(Uri.parse(materi.video))
                                                setMediaItem(mediaItem)
                                                prepare()
                                                playWhenReady = false
                                            }

                                            PlayerView(context).apply {
                                                player = exoPlayer
                                                useController = true
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
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

fun updateProgress(navController: NavController, kursusId: Int, watchedCount: Int, total: Int) {
    val context = navController.context
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken() ?: return

    val progress = ((watchedCount.toFloat() / total) * 100).toInt()

    val body = mapOf(
        "kursus_id" to kursusId,
        "progress" to progress
    )

    RetrofitClient.instance.updateProgress("Bearer $token", body)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Progress: $progress%", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal update progress", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}


