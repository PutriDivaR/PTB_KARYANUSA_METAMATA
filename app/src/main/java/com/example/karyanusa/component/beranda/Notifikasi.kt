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

    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }
    val bearerToken = tokenManager.getBearerToken()

    var notifikasiList by remember { mutableStateOf<List<Notifikasi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bearerToken) {
        if (bearerToken == null) {
            errorMsg = "Silakan login terlebih dahulu"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        RetrofitClient.instance
            .getNotifications(bearerToken)
            .enqueue(object : Callback<List<Notifikasi>> {

                override fun onResponse(
                    call: Call<List<Notifikasi>>,
                    response: Response<List<Notifikasi>>
                ) {
                    if (response.isSuccessful) {
                        notifikasiList = response.body() ?: emptyList()
                    } else {
                        errorMsg = "Error ${response.code()}"
                    }
                    isLoading = false
                }

                override fun onFailure(call: Call<List<Notifikasi>>, t: Throwable) {
                    errorMsg = t.message
                    isLoading = false
                }
            })
    }

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
                    IconButton(onClick = { navController.navigate("forum") }) {
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = pinkTua)
                        Spacer(Modifier.height(8.dp))
                        Text("Memuat notifikasi...", color = Color.Gray)
                    }
                }

                errorMsg != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMsg = null
                                navController.navigate("notifikasi") {
                                    popUpTo("notifikasi") { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = pinkTua)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                notifikasiList.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada notifikasi",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Notifikasi akan muncul di sini",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
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
                                        notifikasiList = notifikasiList.map {
                                            if (it.notif_id == notif.notif_id)
                                                it.copy(is_read = 1)
                                            else it
                                        }

                                        bearerToken?.let { token ->
                                            markAsRead(
                                                token = token,
                                                notifId = notif.notif_id
                                            ) {}
                                        }

                                        when (notif.type) {
                                            "share_kursus" -> {
                                                notif.related_id?.let { id ->
                                                    navController.navigate("detail_kursus/$id")
                                                }
                                            }

                                            "like" -> {
                                                navController.navigate("galeri?initialTab=pribadi")
                                            }

                                            "view_milestone" -> {
                                                navController.navigate("galeri?initialTab=pribadi")
                                            }

                                            "reply_forum" -> {
                                                notif.related_id?.let { pertanyaanId ->
                                                    navController.navigate("forumDetail/$pertanyaanId")
                                                }
                                            }
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
                Log.d("NOTIF_READ", "✅ Mark as read success: ${response.code()}")
                onDone()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("NOTIF_READ", "❌ Gagal update is_read: ${t.message}")
                onDone()
            }
        })
}