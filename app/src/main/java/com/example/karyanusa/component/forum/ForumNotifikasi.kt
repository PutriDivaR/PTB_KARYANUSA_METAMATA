package com.example.karyanusa.component.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumNotifikasi(navController: NavController) {

    var notifications by remember {
        mutableStateOf(
            listOf(
                "Seseorang membalas forum kamu",
                "Forum kamu mencapai 10 balasan",
                "Forum kamu mencapai 50 balasan"
            )
        )
    }

    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifikasi",
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF4A0E24)
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { showClearAllDialog = true }) {
                            Text(
                                "Hapus Semua",
                                color = Color(0xFF4A0E24),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFE4EC)
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF8FA))
                .padding(padding)
        ) {

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada notifikasi",
                        color = Color(0xFF4A0E24),
                        fontWeight = FontWeight.Light
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(notifications) { notif ->
                        NotificationItem(
                            text = notif,
                            onRemove = {
                                notifications = notifications.minus(notif)
                            }
                        )
                    }
                }
            }
        }

        // Dialog hapus semua
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = {
                    Text(
                        "Hapus Semua Notifikasi?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A0E24)
                    )
                },
                text = {
                    Text(
                        "Semua notifikasi akan dihapus. Yakin ingin melanjutkan?",
                        color = Color.DarkGray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            notifications = emptyList()
                            showClearAllDialog = false
                        }
                    ) {
                        Text(
                            "Ya, Hapus Semua",
                            color = Color(0xFFB00020),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text("Batal", color = Color(0xFF4A0E24))
                    }
                },
                containerColor = Color.White
            )
        }
    }
}



@Composable
fun NotificationItem(
    text: String,
    onRemove: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "Hapus Notifikasi?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A0E24)
                )
            },
            text = {
                Text(
                    "Apakah kamu yakin ingin menghapus notifikasi ini?",
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onRemove()
                    }
                ) {
                    Text(
                        "Ya, Hapus",
                        color = Color(0xFFB00020),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal", color = Color(0xFF4A0E24))
                }
            },
            containerColor = Color.White
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE4EC)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text,
                color = Color(0xFF4A0E24),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus",
                    tint = Color(0xFF4A0E24)
                )
            }
        }
    }
}
