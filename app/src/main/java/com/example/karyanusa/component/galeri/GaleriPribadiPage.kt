package com.example.karyanusa.component.galeri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.data.viewmodel.GaleriViewModel

@Composable
fun GaleriPribadiPage(
    navController: NavController,
    viewModel: GaleriViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = LoginTokenManager(context)
    val token = tokenManager.getBearerToken()
    val userId = tokenManager.getUserId()

    val karyaList by viewModel.myKaryaList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var karyaDihapus by remember { mutableStateOf<KaryaEntity?>(null) }

    val pinkTua = Color(0xFF4A0E24)
    val background = Color(0xFFFFF5F7)

    if (token == null || userId == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
        return
    }
    LaunchedEffect(Unit) {
        viewModel.loadMyKarya(token, userId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            Toast.makeText(context, "Karya berhasil dihapus", Toast.LENGTH_SHORT).show()
            showDialog = false
            karyaDihapus = null
            viewModel.resetDeleteSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Button(
            onClick = { navController.navigate("upload") },
            colors = ButtonDefaults.buttonColors(containerColor = pinkTua),
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = "Upload", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Upload Karya Baru", color = Color.White)
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = pinkTua)
                }
            }

            karyaList.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Belum ada karya Anda",
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mulai unggah karya pertama Anda!",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(karyaList, key = { it.galeri_id }) { karya ->
                        KaryaCard(
                            karya = karya,
                            pinkTua = pinkTua,
                            onEdit = {
                                navController.navigate("edit/${karya.galeri_id}")
                            },
                            onDelete = {
                                karyaDihapus = karya
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog && karyaDihapus != null) {
        DeleteConfirmationDialog(
            karya = karyaDihapus!!,
            isDeleting = isDeleting,
            pinkTua = pinkTua,
            onConfirm = {
                viewModel.deleteKarya(token, karyaDihapus!!.galeri_id, userId)
            },
            onDismiss = {
                if (!isDeleting) {
                    showDialog = false
                    karyaDihapus = null
                }
            }
        )
    }
}

@Composable
private fun KaryaCard(
    karya: KaryaEntity,
    pinkTua: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = karya.judul,
                    fontWeight = FontWeight.Bold,
                    color = pinkTua
                )
                Text(
                    text = karya.caption,
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = Color(0xFF7A4E5A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${karya.views} views",
                        color = Color(0xFF7A4E5A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "❤️ ${karya.likes}",
                        color = Color(0xFF7A4E5A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = pinkTua
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    karya: KaryaEntity,
    isDeleting: Boolean,
    pinkTua: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Ya, Hapus", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Batal", color = Color.Gray)
            }
        },
        title = {
            Text(
                "Konfirmasi Hapus",
                fontWeight = FontWeight.Bold,
                color = pinkTua
            )
        },
        text = {
            Column {
                Text("Apakah Anda yakin ingin menghapus karya ini?")
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"${karya.judul}\"",
                    fontWeight = FontWeight.SemiBold,
                    color = pinkTua
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tindakan ini tidak dapat dibatalkan.",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
        },
        containerColor = Color(0xFFFFE4EC),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    )
}