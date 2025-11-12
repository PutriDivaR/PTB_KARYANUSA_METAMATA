package com.example.karyanusa.component.kursus


import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.karyanusa.network.EnrollmentCheckResponse
import kotlinx.coroutines.launch
import kotlin.math.floor
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.LinearProgressIndicator
import com.example.karyanusa.network.EnrollmentResponse
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriPage(navController: NavController, kursusId: Int) {
    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var enrollmentStatus by remember { mutableStateOf("none") } // none, ongoing, completed
    var isChecking by remember { mutableStateOf(true) }
    var watchedIds by rememberSaveable { mutableStateOf(setOf<Int>()) } // gunakan rememberSaveable agar survive config change
    var watchedCount by remember { mutableIntStateOf(0) }
    var serverProgress by remember { mutableIntStateOf(0) }


    val tokenManager = LoginTokenManager(LocalContext.current)
    val token = tokenManager.getToken()


    fun onVideoCompleted(materiId: Int) {
        if (materiId in watchedIds) return
        watchedIds = watchedIds + materiId
        watchedCount = watchedIds.size

        val total = materiList.size.takeIf { it > 0 } ?: 1
        val progress = floor((watchedCount.toDouble() / total) * 100).toInt()

        serverProgress = progress
        if (progress >= 100) enrollmentStatus = "completed"

        val body = mapOf("kursus_id" to kursusId, "progress" to progress)

        if (token != null) {
            RetrofitClient.instance.updateProgress("Bearer $token", body)
                .enqueue(object : Callback<EnrollmentResponse> {
                    override fun onResponse(
                        call: Call<EnrollmentResponse>,
                        response: Response<EnrollmentResponse>
                    ) {
                        if (response.isSuccessful) {
                            println("Progress updated: $progress%")
                        } else {
                            println("Failed update progress: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<EnrollmentResponse>, t: Throwable) {
                        println("Update progress error: ${t.message}")
                    }
                })
        }
    }


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
                                serverProgress = data.progress ?: 0

                                if (materiList.isNotEmpty()) {
                                    watchedCount =
                                        ((serverProgress * materiList.size) / 100.0).toInt()
                                }
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

    val materiIdTerakhir = remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoCompleted = result.data?.getBooleanExtra("video_completed", false) ?: false
            if (videoCompleted) {
                onVideoCompleted(materiIdTerakhir.intValue)
            }
        }
    }


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
                        val total = materiList.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${watchedCount}/${total} Bagian", color = Color.Gray)
                            Text(text = "$serverProgress%", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { serverProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(0xFF7A3E48),
                            trackColor = Color.LightGray,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                        Spacer(modifier = Modifier.height(12.dp))


                        // Daftar video materi
                        materiList.forEach { materi ->
                            val isWatched = materi.materi_id in watchedIds
                            val context = LocalContext.current

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9EE)),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {

                                    // --- Wrapper biar bisa tambah tombol fullscreen ---
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    ) {
                                        AndroidView(
                                            factory = {
                                                val exoPlayer = ExoPlayer.Builder(context).build().apply {
                                                    val mediaItem = MediaItem.fromUri(Uri.parse(materi.video))
                                                    setMediaItem(mediaItem)
                                                    prepare()
                                                    playWhenReady = false
                                                }

                                                // listener kalau video selesai
                                                exoPlayer.addListener(object : Player.Listener {
                                                    override fun onPlaybackStateChanged(state: Int) {
                                                        if (state == Player.STATE_ENDED) {
                                                            Handler(Looper.getMainLooper()).post {
                                                                onVideoCompleted(materi.materi_id)
                                                            }
                                                        }
                                                    }
                                                })

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
                                                .fillMaxSize()
                                        )

                                        IconButton(
                                            onClick = {
                                                try {
                                                    materiIdTerakhir.value = materi.materi_id
                                                    val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                        putExtra("video_url", materi.video)
                                                    }
                                                    launcher.launch(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Gagal membuka fullscreen", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color(0x66000000), shape = CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = "Fullscreen",
                                                tint = Color.White
                                            )
                                        }



                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(materi.judul, fontWeight = FontWeight.Bold)

                                    if (isWatched) {
                                        Button(
                                            onClick = {},
                                            enabled = false,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF7A3E48),
                                                disabledContainerColor = Color(0xFF7A3E48)
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .padding(top = 4.dp)
                                        ) {
                                            Text("Selesai", color = Color.White, fontSize = 12.sp)
                                        }
                                    }

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


