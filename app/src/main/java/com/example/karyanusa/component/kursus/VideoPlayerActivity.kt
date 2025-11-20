package com.example.karyanusa.component.kursus

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUrl = intent.getStringExtra("video_url") ?: return

        setContent {
            val context = this
            var videoCompleted by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                val player = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                        prepare()
                        playWhenReady = true

                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_ENDED) {
                                    videoCompleted = true
                                }
                            }
                        })
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        // Kirim hasil ke MateriPage saat ditutup
                        val resultIntent = intent.apply {
                            putExtra("video_completed", videoCompleted)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        player.release()
                    }
                }

                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // keluar fullscren
                IconButton(
                    onClick = { finish() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color(0x66000000), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
