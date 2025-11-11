package com.example.karyanusa.component.kursus

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.karyanusa.R
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.EnrollmentCheckResponse
import com.example.karyanusa.network.Kursus
import com.example.karyanusa.network.Materi
import com.example.karyanusa.network.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(navController: NavController, kursusId: Int) {
    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var enrollmentStatus by remember { mutableStateOf("none") } // none, ongoing, completed
    var isChecking by remember { mutableStateOf(true) }

    val tokenManager = LoginTokenManager(LocalContext.current)
    val token = tokenManager.getToken()

    // Ambil data kursus n materi
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getCourses().enqueue(object : Callback<List<Kursus>> {
            override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                if (response.isSuccessful) kursusList = response.body() ?: emptyList()
                isLoading = false
            }
            override fun onFailure(call: Call<List<Kursus>>, t: Throwable) { isLoading = false }
        })

        RetrofitClient.instance.getMateriByKursus(kursusId).enqueue(object : Callback<List<Materi>> {
            override fun onResponse(call: Call<List<Materi>>, response: Response<List<Materi>>) {
                if (response.isSuccessful) materiList = response.body() ?: emptyList()
            }
            override fun onFailure(call: Call<List<Materi>>, t: Throwable) {
                println("Error load materi: ${t.message}")
            }
        })


        if (token != null) {
            RetrofitClient.instance.checkEnrollment("Bearer $token", kursusId)
                .enqueue(object : Callback<EnrollmentCheckResponse> {
                    override fun onResponse(call: Call<EnrollmentCheckResponse>, response: Response<EnrollmentCheckResponse>) {
                        if (response.isSuccessful) {
                            val data = response.body()
                            if (data != null && data.enrolled) {
                                enrollmentStatus = when (data.status) {
                                    "completed" -> "completed"
                                    "ongoing" -> "ongoing"
                                    else -> "none"
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

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                var pressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (pressed) 0.55f else 1f)

                Button(
                    onClick = {
                        when (enrollmentStatus) {
                            "none" -> enrollToCourse(kursusId, navController)
                            "ongoing", "completed" -> navController.navigate("materi/$kursusId")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48))
                ) {
                    Text(
                        when (enrollmentStatus) {
                            "none" -> "Ikuti Kelas"
                            "ongoing" -> "Lanjutkan Kelas"
                            "completed" -> "Lihat Sertifikat"
                            else -> "Ikuti Kelas"
                        },
                        color = Color.White
                    )
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
                        // Header per kursus
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(kursus.thumbnail)
                                        .crossfade(true)
                                        .diskCacheKey(kursus.thumbnail)
                                        .memoryCacheKey(kursus.thumbnail)
                                        .error(R.drawable.tessampul)
                                        .build()
                                ),
                                contentDescription = kursus.judul,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nama Pengrajin
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.karyanusalogo),
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(kursus.pengrajin_nama, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Tentang kelas
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📝 Tentang Kelas", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(kursus.deskripsi, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Card Daftar Materi
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("📚 Daftar Materi", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                if (materiList.isEmpty()) {
                                    Text("Belum ada materi untuk kursus ini.")
                                } else {
                                    materiList.forEachIndexed { index, materi ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            elevation = CardDefaults.cardElevation(4.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F9))
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text("${index + 1}. ${materi.judul}", fontWeight = FontWeight.Medium)
                                                Text(
                                                    "Durasi: ${materi.durasi} menit",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
                else -> Text("Data kursus tidak ditemukan", Modifier.align(Alignment.Center))
            }
        }
    }
}

fun enrollToCourse(kursusId: Int, navController: NavController) {
    val context = navController.context
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getToken()

    if (token == null) {
        Toast.makeText(context, "Harus login dulu!", Toast.LENGTH_SHORT).show()
        return
    }

    val requestBody = mapOf("kursus_id" to kursusId)

    println("TOKEN: Bearer $token")
    println("REQUEST: $requestBody")

    RetrofitClient.instance.enrollCourse("Bearer $token", requestBody)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                println("ENROLL RESPONSE: ${response.code()} ${response.message()}")
                println("ENROLL BODY: ${response.errorBody()?.string()}")
                println("TOKEN SAAT ENROLL: $token")

                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil mendaftar kursus!", Toast.LENGTH_SHORT).show()
                    navController.navigate("materi/$kursusId")
                } else {
                    Toast.makeText(context, "Gagal daftar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("ENROLL FAILURE: ${t.message}")
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

}

