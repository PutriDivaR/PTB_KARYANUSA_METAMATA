package com.example.karyanusa.component.beranda

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.network.Notifikasi
import com.example.karyanusa.network.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifikasiPage(navController: NavController) {

    // ===== TOKEN =====
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val bearerToken = tokenManager.getBearerToken()

    // ===== STATE =====
    var notifikasiList by remember { mutableStateOf<List<Notifikasi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ===== LOAD DATA =====
    LaunchedEffect(bearerToken) {
        if (bearerToken == null) {
            Log.e("NOTIF_ANDROID", "Bearer token NULL")
            isLoading = false
            return@LaunchedEffect
        }

        Log.d("NOTIF_ANDROID", "Request with token: $bearerToken")

        RetrofitClient.instance
            .getNotifications(bearerToken)
            .enqueue(object : Callback<List<Notifikasi>> {

                override fun onResponse(
                    call: Call<List<Notifikasi>>,
                    response: Response<List<Notifikasi>>
                ) {
                    Log.d("NOTIF_ANDROID", "Code: ${response.code()}")
                    Log.d("NOTIF_ANDROID", "Body: ${response.body()}")

                    if (response.isSuccessful) {
                        notifikasiList = response.body() ?: emptyList()
                    }

                    isLoading = false
                }

                override fun onFailure(call: Call<List<Notifikasi>>, t: Throwable) {
                    Log.e("NOTIF_ANDROID", "Error", t)
                    isLoading = false
                }
            })
    }

    // ===== WARNA =====
    val pinkMuda = Color(0xFFFFE4EC)
    val pinkTua = Color(0xFF4A0E24)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifikasi",
                        color = pinkTua,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = pinkTua
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pinkMuda
                )
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = pinkMuda) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.navigate("beranda") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("chat") }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("kursus") }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Kursus", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("galeri") }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Galeri", tint = pinkTua)
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = pinkTua)
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF5F7))
                .padding(innerPadding)
        ) {

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                notifikasiList.isEmpty() -> {
                    Text(
                        text = "Belum ada notifikasi",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(notifikasiList) { notif ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {

                                        // === 1. UPDATE UI LOKAL ===
                                        notifikasiList = notifikasiList.map {
                                            if (it.notif_id == notif.notif_id)
                                                it.copy(is_read = 1)
                                            else it
                                        }

                                        // === 2. UPDATE BACKEND ===
                                        bearerToken?.let { token ->
                                            markAsRead(
                                                token = token,
                                                notifId = notif.notif_id
                                            ) {}
                                        }

                                        // === 3. NAVIGASI SESUAI TIPE ===
                                        when (notif.type) {
                                            "share_kursus" -> {
                                                notif.related_id?.let { id ->
                                                    navController.navigate("detail_kursus/$id") // Ini sudah benar
                                                }
                                            }

                                            "like" -> {
                                                // PERUBAHAN: Panggil rute 'galeri' dengan argumen
                                                navController.navigate("galeri?initialTab=pribadi")
                                            }

                                            "view_milestone" -> {
                                                navController.navigate("galeri?initialTab=pribadi")
                                            }

                                            //untuk wanda hapus aja '//' nya
                                            //"forum" -> {
                                            //navController.navigate("galeri?initialTab=pribadi") //ganti navigasi nya nanti
                                            // }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (notif.is_read == 1)
                                            Color.White
                                        else
                                            Color(0xFFFFEEF3)
                                ),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {

                                Column(Modifier.padding(16.dp)) {

                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        color = pinkTua
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = notif.message,
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = notif.created_at,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

fun markAsRead(
    token: String,
    notifId: Int,
    onDone: () -> Unit
) {
    RetrofitClient.instance
        .markNotifRead(token, notifId)
        .enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                onDone()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("NOTIF_READ", "Gagal update is_read")
                onDone()
            }
        })
}

