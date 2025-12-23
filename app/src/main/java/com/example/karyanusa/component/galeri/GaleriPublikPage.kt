package com.example.karyanusa.component.galeri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.data.viewmodel.GaleriPublikViewModel

@Composable
fun GaleriPublikPage(
    navController: NavController,
    viewModel: GaleriPublikViewModel = viewModel()
) {
    val context = LocalContext.current

    val filteredKaryaList by viewModel.filteredKaryaList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedItem by remember { mutableStateOf<KaryaEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAllKarya()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F7))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Cari karya...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A0E24),
                    focusedLabelColor = Color(0xFF4A0E24)
                )
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4A0E24))
                            Text(
                                "Memuat data...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                filteredKaryaList.isEmpty() && searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Tidak ada hasil untuk \"$searchQuery\"",
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Coba kata kunci lain",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                filteredKaryaList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Belum ada karya tersedia",
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Coba kembali lagi nanti!",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredKaryaList, key = { it.galeri_id }) { karya ->
                            KaryaPublikCard(
                                karya = karya,
                                onClick = { selectedItem = karya }
                            )
                        }
                    }
                }
            }
        }

        if (selectedItem != null) {
            DetailKaryaDialog(
                karya = selectedItem!!,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

@Composable
private fun KaryaPublikCard(
    karya: KaryaEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {

            SubcomposeAsyncImage(
                model = "http://10.0.2.2:8000/storage/${karya.gambar}",
                contentDescription = karya.judul,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF4A0E24),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Error",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Gagal memuat gambar",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = karya.judul,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF4A0E24)
                )

                Spacer(Modifier.height(4.dp))

                karya.uploader_name?.let {
                    Text(
                        text = "Oleh: $it",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = Color(0xFF7A4E5A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${karya.views}",
                            color = Color(0xFF7A4E5A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${karya.likes}",
                            color = Color(0xFFE91E63),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}