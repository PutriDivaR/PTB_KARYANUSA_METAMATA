package com.example.karyanusa.component.kursus

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.karyanusa.R
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.data.viewmodel.KursusViewModel
import com.example.karyanusa.data.viewmodel.MateriViewModel
import com.example.karyanusa.network.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(navController: NavController, kursusId: Int, notifId: Int? = null) {

    // VIEW MODELS (Pengganti direct Retrofit call untuk data)
    val kursusViewModel: KursusViewModel = viewModel()
    val materiViewModel: MateriViewModel = viewModel()

    // OBSERVING DATA ROOM
    // Mengambil daftar kursus dari database lokal (ROOM)
    val allKursus by kursusViewModel.kursus.collectAsState()
    val materiList by materiViewModel.getMateri(kursusId).collectAsState(initial = emptyList())
    val isMateriLoading by materiViewModel.isLoading.collectAsState()

    val kursus = allKursus.find { it.kursus_id == kursusId }

    // STATE LOKAL
    var enrollmentStatus by remember { mutableStateOf("none") }
    var showShareSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }


    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val token = tokenManager.getToken()



    // OAD DATA & SYNC
    LaunchedEffect(kursusId) {
        materiViewModel.refreshMateri(kursusId)

        // Cek Status Enrollment
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
                    }
                    override fun onFailure(call: Call<EnrollmentCheckResponse>, t: Throwable) { }
                })
        }
    }

    // Mark Notifikasi
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

    // kirim notif
    fun sendCourseShareNotif(toUser: Int) {
        val currentIdInt = tokenManager.getUserId()
        val currentUsername = tokenManager.getUserName()
        if (kursus == null) return

        if (currentIdInt == null || currentUsername == null) {
            Toast.makeText(context, "Gagal mendapatkan data pengguna", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Gagal mengirim", Toast.LENGTH_SHORT).show()
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
                override fun onResponse(call: Call<List<UserData>>, response: Response<List<UserData>>) {
                    if (response.isSuccessful) {
                        val allUsers = response.body() ?: emptyList()
                        searchResults = if (currentIdInt != null) {
                            allUsers.filter { it.user_id != currentIdInt }
                        } else {
                            allUsers
                        }
                    }
                }
                override fun onFailure(call: Call<List<UserData>>, t: Throwable) {}
            })
    }

    fun searchUser(query: String) {
        if (query.isEmpty()) {
            loadAllUsers()
            return
        }
        searchResults = searchResults.filter {
            it.username.contains(query, ignoreCase = true)
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
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF5F7))) {
            if (kursus == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = innerPadding.calculateBottomPadding() + 20.dp)) {
                    // Header Image
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current).data(kursus.thumbnail).crossfade(true).error(R.drawable.tessampul).build()
                            ),
                            contentDescription = kursus.judul,
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            contentScale = ContentScale.Crop
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    showShareSheet = true
                                    loadAllUsers()
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Share, "Share", tint = Color.White)
                            }
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
                    
                    // kursusnya
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
                            
                            if (isMateriLoading && materiList.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                            } else {
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
            }

            // Button Action
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = innerPadding.calculateBottomPadding()).padding(horizontal = 16.dp, vertical = 12.dp).align(Alignment.BottomCenter)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (enrollmentStatus == "ongoing" || enrollmentStatus == "completed") {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red,
                                containerColor = Color.White
                            ),
                            modifier = Modifier
                                .width(56.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Batalkan Kursus")
                        }
                    }

                    Button(
                        onClick = {
                            when (enrollmentStatus) {
                                "none" -> enrollToCourse(kursusId, navController)
                                "ongoing", "completed" -> navController.navigate("materi/$kursusId")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3E48)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    cancelEnrollment(kursusId, navController)
                }) {
                    Text("Ya, Hapus", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            },
            title = { Text("Batalkan Kursus?") },
            text = {
                Text("Progress belajar akan dihapus dan tidak bisa dikembalikan.")
            }
        )
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

fun cancelEnrollment(kursusId: Int, navController: NavController) {
    val context = navController.context
    val token = LoginTokenManager(context).getToken() ?: return

    RetrofitClient.instance
        .cancelEnrollment("Bearer $token", kursusId)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Kursus dibatalkan", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Gagal membatalkan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
