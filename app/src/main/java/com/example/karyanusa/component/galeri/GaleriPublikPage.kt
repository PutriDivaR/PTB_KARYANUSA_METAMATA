package com.example.karyanusa.component.galeri

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun GaleriPublikPage(
    navController: NavController,
    viewModel: GaleriPublikViewModel = viewModel()
) {
    val context = LocalContext.current

    // Collect states dari ViewModel
    val filteredKaryaList by viewModel.filteredKaryaList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedItem by remember { mutableStateOf<KaryaEntity?>(null) }

    // Load data pertama kali
    LaunchedEffect(Unit) {
        viewModel.loadAllKarya()
    }

    // Handle error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // SwipeRefresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F7))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
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

            // SwipeRefresh untuk pull to refresh
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refreshKarya() },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    // Loading pertama kali (data kosong)
                    isLoading && filteredKaryaList.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4A0E24))
                        }
                    }

                    // Data kosong setelah search
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

                    // Data kosong (belum ada karya)
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
                                    "Coba refresh atau unggah karya pertama!",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Ada data
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
        }

        // Detail Dialog
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
            // Image dengan Loading & Error State
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

            // Info
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

                // Views & Likes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Views
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

                    // Likes
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

@Composable
private fun DetailKaryaDialog(
    karya: KaryaEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A0E24)
                )
            ) {
                Text("Tutup", color = Color.White)
            }
        },
        title = {
            Text(
                text = karya.judul,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A0E24)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image dengan Loading & Error State
                SubcomposeAsyncImage(
                    model = "http://10.0.2.2:8000/storage/${karya.gambar}",
                    contentDescription = karya.judul,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
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
                                horizontalAlignment = Alignment.CenterHorizontally
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

                // Caption
                Text(
                    text = karya.caption,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                // Uploader
                karya.uploader_name?.let {
                    Text(
                        text = "Dibuat oleh: $it",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Views
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Views",
                            tint = Color(0xFF7A4E5A),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${karya.views} views",
                            color = Color(0xFF7A4E5A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Likes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${karya.likes} likes",
                            color = Color(0xFFE91E63),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Upload date
                karya.tanggal_upload?.let {
                    Text(
                        text = "Diunggah: $it",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        containerColor = Color(0xFFFFF5F7),
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.large
    )
}