package com.example.karyanusa.component.kursus

import android.R.id.message
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import com.example.karyanusa.network.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(navController: NavController, kursusId: Int, notifId: Int? = null) {

    // STATE
    var kursusList by remember { mutableStateOf<List<Kursus>>(emptyList()) }
    var materiList by remember { mutableStateOf<List<Materi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var enrollmentStatus by remember { mutableStateOf("none") }
    var isChecking by remember { mutableStateOf(true) }
    var showShareSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserData>>(emptyList()) }

    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()

    // Find the course details
    val kursus = kursusList.find { it.kursus_id == kursusId }


    LaunchedEffect(notifId) {
        if (notifId != null && token != null) {
            RetrofitClient.instance
                .markNotifRead(token, notifId)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(c: Call<ResponseBody>, r: Response<ResponseBody>) {}
                    override fun onFailure(c: Call<ResponseBody>, t: Throwable) {}
                })
        }
    }


    // Function to send notification when sharing
    fun sendCourseShareNotif(toUser: Int) {
        val currentIdInt = tokenManager.getUserId()
        val currentUsername = tokenManager.getUserName()
        if (kursus == null) {
            Toast.makeText(context, "Kursus belum dimuat", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentIdInt == null) {
            Toast.makeText(context, "Gagal mendapatkan ID pengguna", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUsername == null) {
            Toast.makeText(context, "Gagal mendapatkan username pengguna", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf(
            "from_user" to currentIdInt,
            "to_user" to toUser,
            "type" to "share_kursus",
            "title" to "Kursus Dibagikan",
            "message" to "$currentUsername membagikan kursus: ${kursus.judul}",
            "related_id" to kursusId
        )

        RetrofitClient.instance.sendNotification("Bearer $token", body)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("NOTIF_ERROR", "Error: $errorBody")
                        Toast.makeText(context, "Gagal: $errorBody", Toast.LENGTH_SHORT).show()
                    }

                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun loadAllUsers() {
        val currentIdInt = tokenManager.getUserId()
        RetrofitClient.instance.getAllUsers("Bearer $token")
            .enqueue(object : Callback<List<UserData>> {
                override fun onResponse(
                    call: Call<List<UserData>>,
                    response: Response<List<UserData>>
                ) {
                    if (response.isSuccessful) {
                        val allUsers = response.body() ?: emptyList()
                        if (currentIdInt != null) {
                            searchResults = allUsers.filter { it.user_id != currentIdInt }
                        } else {
                            searchResults = allUsers
                        }
                    }
                }

                override fun onFailure(call: Call<List<UserData>>, t: Throwable) {}
            })
    }

    // Search function for sharing
    fun searchUser(query: String) {
        if (query.isEmpty()) {
            loadAllUsers() // jika dikosongkan tampilkan semua
            return
        }

        searchResults = searchResults.filter {
            it.username.contains(query, ignoreCase = true)
        }
    }


    // LOAD DATA
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getCourses().enqueue(object : Callback<List<Kursus>> {
            override fun onResponse(call: Call<List<Kursus>>, response: Response<List<Kursus>>) {
                if (response.isSuccessful) kursusList = response.body() ?: emptyList()
                isLoading = false
            }
            override fun onFailure(call: Call<List<Kursus>>, t: Throwable) { isLoading = false }
        })

        RetrofitClient.instance.getMateriByKursus(kursusId)
            .enqueue(object : Callback<List<Materi>> {
                override fun onResponse(call: Call<List<Materi>>, response: Response<List<Materi>>) {
                    if (response.isSuccessful) materiList = response.body() ?: emptyList()
                }
                override fun onFailure(call: Call<List<Materi>>, t: Throwable) {}
            })

        if (token != null) {
            RetrofitClient.instance.checkEnrollment("Bearer $token", kursusId)
                .enqueue(object : Callback<EnrollmentCheckResponse> {
                    override fun onResponse(call: Call<EnrollmentCheckResponse>, response: Response<EnrollmentCheckResponse>) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                if (it.enrolled) {
                                    enrollmentStatus = if (it.status == "completed") "completed" else "ongoing"
                                }
                            }
                        }
                        isChecking = false
                    }
                    override fun onFailure(call: Call<EnrollmentCheckResponse>, t: Throwable) { isChecking = false }
                })
        } else {
            isChecking = false
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFE4EC)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda")}) { Icon(Icons.Default.Home, "Home", tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("forum") }) { Icon(Icons.AutoMirrored.Filled.Chat, "Chat", tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("kursus") }) { Icon(Icons.AutoMirrored.Filled.MenuBook, "Kursus", tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("galeri") }) { Icon(Icons.Default.AddAPhoto, "Galeri", tint = Color(0xFF4A0E24)) }
                    IconButton(onClick = { navController.navigate("profile") }) { Icon(Icons.Default.Person, "Profile", tint = Color(0xFF4A0E24)) }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFFFF5F7))) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (kursus != null) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 92.dp)) {
                    // Header Image
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current).data(kursus.thumbnail).crossfade(true).error(R.drawable.tessampul).build()
                            ),
                            contentDescription = kursus.judul,
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        IconButton(onClick = {
                            showShareSheet = true
                            loadAllUsers() },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Icon(Icons.Default.Share, "Share", tint = Color.White)
                        }
                        Text(
                            text = kursus.judul,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Pengrajin Info
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(id = R.drawable.karyanusalogo), null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text(kursus.pengrajin_nama, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // About Class
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("📝 Tentang Kelas", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(kursus.deskripsi, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Materi List
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("📚 Daftar Materi", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            materiList.forEachIndexed { index, materi ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F9))) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${index + 1}. ${materi.judul}", fontWeight = FontWeight.Medium)
                                        Text("Durasi: ${materi.durasi} menit", fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Button
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).align(Alignment.BottomCenter)) {
                Button(
                    onClick = {
                        when (enrollmentStatus) {
                            "none" -> enrollToCourse(kursusId, navController)
                            "ongoing", "completed" -> navController.navigate("materi/$kursusId")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        when (enrollmentStatus) {
                            "none" -> "Ikuti Kelas"
                            "ongoing" -> "Lanjutkan Kelas"
                            "completed" -> "Lihat Sertifikat"
                            else -> "Memuat..."
                        },
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    searchUser(it)
                },
                label = { Text("Cari username...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn {
                items(searchResults) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            sendCourseShareNotif(user.user_id)
                            showShareSheet = false
                        }.padding(16.dp)
                    ) {
                        Text(user.username, fontSize = 16.sp)
                    }
                }
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

    val requestBody = mapOf("kursus_id" to kursusId.toString())
    RetrofitClient.instance.enrollCourse("Bearer $token", requestBody)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Berhasil mendaftar!", Toast.LENGTH_SHORT).show()
                    navController.navigate("materi/$kursusId")
                } else {
                    Toast.makeText(context, "Gagal mendaftar", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
