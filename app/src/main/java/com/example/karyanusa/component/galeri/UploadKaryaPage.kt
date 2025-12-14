package com.example.karyanusa.component.galeri

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val pinkTua = Color(0xFF4A0E24)
    val background = Color(0xFFFFF5F7)
    val accent = Color(0xFFFFE4EC)

    // Launcher untuk ambil dari galeri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        capturedBitmap = null // reset jika sebelumnya ambil dari kamera
    }

    // Launcher untuk ambil foto dari kamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        capturedBitmap = bitmap
        imageUri = null // reset jika sebelumnya ambil dari galeri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Upload Karya", color = pinkTua, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "kembali", tint = pinkTua)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = accent)
            )
        },
        containerColor = background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Tambahkan hasil karyamu!",
                    color = pinkTua,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Preview gambar atau kotak upload
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(accent, shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        capturedBitmap != null -> {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto Kamera",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        imageUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUri),
                                contentDescription = "Preview Gambar Karya",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Text(
                                text = "Belum ada gambar. Pilih dari galeri atau ambil foto.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Tombol pilih sumber gambar
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = pinkTua)
                        Spacer(Modifier.width(8.dp))
                        Text("Galeri", color = pinkTua)
                    }
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = pinkTua)
                        Spacer(Modifier.width(8.dp))
                        Text("Kamera", color = pinkTua)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Input Nama Karya (max 30 karakter)
                OutlinedTextField(
                    value = namaKarya,
                    onValueChange = {
                        if (it.length <= 30) namaKarya = it
                    },
                    label = { Text("Nama Karya (max 30 karakter)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading,
                    singleLine = true
                )
                Text(
                    text = "${namaKarya.length}/30 karakter",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp, end = 4.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Input Deskripsi (max 200 karakter)
                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = {
                        if (it.length <= 200) deskripsi = it
                    },
                    label = { Text("Deskripsi (max 200 karakter)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    enabled = !isUploading
                )
                Text(
                    text = "${deskripsi.length}/200 karakter",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp, end = 4.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Tombol Upload
                Button(
                    onClick = {
                        if (isUploading) return@Button
                        if (namaKarya.isNotBlank() && deskripsi.isNotBlank() && (imageUri != null || capturedBitmap != null)) {
                            isUploading = true

                            val karyaBaru = Karya(
                                id = KaryaRepository.daftarKarya.size + 1,
                                nama = namaKarya,
                                deskripsi = deskripsi,
                                gambarUri = imageUri,
                                uploader = "Vania Zhafira",
                                gambarBitmap = capturedBitmap
                            )
                            KaryaRepository.daftarKarya.add(karyaBaru)


                            snackbarMessage = "Karya '$namaKarya' berhasil diunggah!"
                            showSnackbar = true

                            scope.launch {
                                delay(1500)
                                isUploading = false
                                navController.navigate("galeri") {
                                    popUpTo("upload") { inclusive = true }
                                }
                            }
                        } else {
                            snackbarMessage = "⚠️ Semua field dan foto harus diisi!"
                            showSnackbar = true
                        }
                    },
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pinkTua)
                ) {
                    Text(
                        if (isUploading) "Mengunggah..." else "Upload",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            // Snackbar cantik dari atas
            AnimatedVisibility(
                visible = showSnackbar,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                CustomTopSnackbar(
                    message = snackbarMessage,
                    modifier = Modifier.padding(top = 80.dp),
                    backgroundColor = accent,
                    textColor = pinkTua
                )
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSnackbar = false
                }
            }
        }
    }
}

// Snackbar custom seragam warna
@Composable
fun CustomTopSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFFE4EC),
    textColor: Color = Color(0xFF4A0E24)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = backgroundColor,
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = textColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
