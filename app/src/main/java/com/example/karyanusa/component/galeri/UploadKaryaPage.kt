package com.example.karyanusa.component.galeri

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadKaryaPage(navController: NavController) {
    var namaKarya by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()

    // buka galeri
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Upload Karya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF4A0E24))
            )
        },
        containerColor = Color(0xFFF7F7F7)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Tambahkan hasil karyamu!",
                color = Color(0xFF4A0E24),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // 🔹 Upload foto dari galeri
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFFFE4EC), shape = MaterialTheme.shapes.medium)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Preview Gambar Karya",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Klik untuk unggah foto dari galeri",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = namaKarya,
                onValueChange = { namaKarya = it },
                label = { Text("Nama Karya") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (namaKarya.isNotBlank() && deskripsi.isNotBlank() && imageUri != null) {
                        val karyaBaru = Karya(
                            id = KaryaRepository.daftarKarya.size + 1,
                            nama = namaKarya,
                            deskripsi = deskripsi,
                            gambarUri = imageUri
                        )
                        KaryaRepository.daftarKarya.add(karyaBaru)
                        pesan = "✅ Karya berhasil diunggah!"

                        scope.launch {
                            delay(1000)
                            navController.navigate("galeri") {
                                popUpTo("upload") { inclusive = true }
                            }
                        }
                    } else {
                        pesan = "⚠️ Semua field dan foto harus diisi!"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24))
            ) {
                Text("Upload", color = Color.White, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (pesan.isNotBlank()) {
                Text(
                    text = pesan,
                    color = if (pesan.contains("berhasil")) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = {
                    navController.navigate("galeri") {
                        popUpTo("upload") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("⬅ Kembali ke Galeri", color = Color(0xFF4A0E24))
            }
        }
    }
}
