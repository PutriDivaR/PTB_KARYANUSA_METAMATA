package com.example.karyanusa.component.kursus

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.R
import com.example.karyanusa.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.karyanusa.component.auth.LoginTokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriPage(navController: NavController, kursusId: Int) {

    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var enrollmentStatus by remember { mutableStateOf("none") }
    var serverProgress by remember { mutableIntStateOf(0) }

    var watchedIds by rememberSaveable { mutableStateOf(setOf<Int>()) }

    val context = LocalContext.current
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken()

    var enrollmentId by remember { mutableStateOf<Int?>(null) }


    fun cekProgressMateri() {
        if (token == null) {
            isLoading = false
            return
        }

        RetrofitClient.instance.getEnrollments("Bearer $token").enqueue(object : Callback<List<EnrollmentData>> {
            override fun onResponse(call: Call<List<EnrollmentData>>, response: Response<List<EnrollmentData>>) {
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val found = list.find { it.kursus_id == kursusId }
                    enrollmentId = found?.enrollment_id
                } else {
                    enrollmentId = null
                }

                if (enrollmentId != null && materiList.isNotEmpty()) {
                    watchedIds = emptySet()
                    val tempSet = mutableSetOf<Int>()

                    materiList.forEach { m ->
                        RetrofitClient.instance.cekMateriSelesai("Bearer $token", enrollmentId!!, m.materi_id)
                            .enqueue(object : Callback<MateriCompletedResponse> {
                                override fun onResponse(call: Call<MateriCompletedResponse>, response: Response<MateriCompletedResponse>) {
                                    if (response.isSuccessful) {
                                        val r = response.body()
                                        if (r != null && r.completed) {
                                            tempSet.add(m.materi_id)
                                        }
                                    }

                                    watchedIds = tempSet.toSet()
                                    isLoading = false
                                }
                                override fun onFailure(call: Call<MateriCompletedResponse>, t: Throwable) {

                                    isLoading = false
                                }
                            })
                    }

                    if (materiList.isEmpty()) isLoading = false
                } else {
                    watchedIds = emptySet()
                    isLoading = false
                }
            }

            override fun onFailure(call: Call<List<EnrollmentData>>, t: Throwable) {
                enrollmentId = null
                watchedIds = emptySet()
                isLoading = false
            }
        })
    }

    fun cekEnrollment() {
        if (token == null) {
            enrollmentStatus = "none"
            serverProgress = 0
            watchedIds = emptySet()
            isLoading = false
            return
        }

        RetrofitClient.instance.checkEnrollment("Bearer $token", kursusId)
            .enqueue(object : Callback<EnrollmentCheckResponse> {
                override fun onResponse(call: Call<EnrollmentCheckResponse>, response: Response<EnrollmentCheckResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        if (data != null && data.enrolled) {
                            enrollmentStatus = data.status ?: "ongoing"
                            serverProgress = data.progress ?: 0
                        } else {
                            enrollmentStatus = "none"
                            serverProgress = 0
                        }
                    }
                    cekProgressMateri()
                }

                override fun onFailure(call: Call<EnrollmentCheckResponse>, t: Throwable) {
                    enrollmentStatus = "none"
                    serverProgress = 0
                    cekProgressMateri()
                }
            })
    }

    fun loadKursusMateri() {
        isLoading = true


        RetrofitClient.instance.getCourses().enqueue(object : Callback<List<Kursus>> {
            override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                if (response.isSuccessful) kursusList = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Kursus>>, t: Throwable) { /* ignore */ }
        })


        RetrofitClient.instance.getMateriByKursus(kursusId).enqueue(object : Callback<List<Materi>> {
            override fun onResponse(call: Call<List<Materi>>, response: Response<List<Materi>>) {
                if (response.isSuccessful) {
                    materiList = response.body() ?: emptyList()
                }
                cekEnrollment()
            }

            override fun onFailure(call: Call<List<Materi>>, t: Throwable) {
                materiList = emptyList()
                cekEnrollment()
            }
        })
    }


    fun tandaiMateriSelesai(materiId: Int) {
        if (token == null) {
            Toast.makeText(context, "Harus login dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val enId = enrollmentId
        if (enId == null) {
            Toast.makeText(context, "Belum mendaftar kursus", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf("enrollment_id" to enId, "materi_id" to materiId)
        RetrofitClient.instance.tandaiMateriSelesai("Bearer $token", body).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {

                    RetrofitClient.instance.checkEnrollment("Bearer $token", kursusId)
                        .enqueue(object : Callback<EnrollmentCheckResponse> {
                            override fun onResponse(call: Call<EnrollmentCheckResponse>, response: Response<EnrollmentCheckResponse>) {
                                if (response.isSuccessful) {
                                    val d = response.body()
                                    if (d != null && d.enrolled) {
                                        serverProgress = d.progress ?: serverProgress
                                        enrollmentStatus = d.status ?: enrollmentStatus
                                    }
                                }
                                watchedIds = watchedIds + materiId
                                Toast.makeText(context, "Materi ditandai selesai", Toast.LENGTH_SHORT).show()
                            }

                            override fun onFailure(call: Call<EnrollmentCheckResponse>, t: Throwable) {

                                watchedIds = watchedIds + materiId
                                Toast.makeText(context, "Materi ditandai (offline)", Toast.LENGTH_SHORT).show()
                            }
                        })
                } else {
                    Toast.makeText(context, "Gagal tandai materi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    val materiIdTerakhir = remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val completed = result.data?.getBooleanExtra("video_completed", false) ?: false
            if (completed) {
                tandaiMateriSelesai(materiIdTerakhir.intValue)
            }
        }
    }


    LaunchedEffect(kursusId) {
        loadKursusMateri()
    }


    val kursus = kursusList.find { it.kursus_id == kursusId }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda")}) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("forum") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Kursus", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Galeri", tint = Color(0xFF4A0E24))
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF4A0E24))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF5F7))
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                kursus != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {

                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(R.drawable.tessampul),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentScale = ContentScale.Crop
                            )

                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 16.dp)
                                    .statusBarsPadding()
                                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            }

                            Text(
                                text = kursus.judul,
                                color = Color.White,
                                fontSize = 22.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        val total = materiList.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${watchedIds.size}/$total Bagian", color = Color.Gray)
                            Text(text = "$serverProgress%", color = Color.Gray)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { serverProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = Color(0xFF7A3E48),
                            trackColor = Color.LightGray,
                            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                        )
                        Spacer(Modifier.height(12.dp))

                        materiList.forEach { materi ->
                            val isWatched = materi.materi_id in watchedIds

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE9EE)),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    ) {
                                        AndroidView(
                                            factory = { ctx ->
                                                val player = ExoPlayer.Builder(ctx).build().apply {
                                                    setMediaItem(MediaItem.fromUri(materi.video ?: ""))
                                                    prepare()
                                                }

                                                PlayerView(ctx).apply {
                                                    this.player = player
                                                    useController = true
                                                }
                                            },
                                            update = { view ->
                                                view.player?.playWhenReady = false
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        IconButton(
                                            onClick = {
                                                materiIdTerakhir.intValue = materi.materi_id
                                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                    putExtra("video_url", materi.video)
                                                }
                                                launcher.launch(intent)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(Color(0x66000000), shape = CircleShape)
                                        ) {
                                            Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(materi.judul, fontSize = 16.sp)

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        if (isWatched) {
                                            Button(
                                                onClick = {},
                                                enabled = false,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48)),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Selesai", color = Color.White, fontSize = 12.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = { tandaiMateriSelesai(materi.materi_id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC5866)),
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text("Tandai Selesai", color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))
                                    Text("Durasi: ${materi.durasi} menit", fontSize = 13.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        Spacer(Modifier.height(90.dp))
                    }
                }
                else -> Text("Data kursus tidak ditemukan", Modifier.align(Alignment.Center))
            }

            if (enrollmentStatus == "completed") {
                Box(modifier = Modifier.fillMaxSize()) {
                    val scopeLocal = rememberCoroutineScope()
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                    ) {
                        Button(
                            onClick = {
                                val userName = LoginTokenManager(context).getUserName() ?: "Peserta"
                                val kursusTitle = kursus?.judul ?: "Kursus"
                                scopeLocal.launch {
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
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48))
                        ) {
                            Icon(painter = painterResource(id = R.drawable.karyanusalogo), contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Unduh Sertifikat", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
