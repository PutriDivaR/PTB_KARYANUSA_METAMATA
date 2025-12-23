package com.example.karyanusa.component.galeri

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.component.auth.LoginTokenManager
import com.example.karyanusa.data.local.entity.KaryaEntity
import com.example.karyanusa.network.*
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun DetailKaryaDialog(
    karya: KaryaEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { LoginTokenManager(context) }

    var viewCounted by remember { mutableStateOf(false) }
    var currentViews by remember { mutableIntStateOf(karya.views) }

    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableIntStateOf(karya.likes) }
    var isLiking by remember { mutableStateOf(false) }

    LaunchedEffect(karya.galeri_id) {
        Log.d("DetailKarya", "⏱️ Timer 5 detik dimulai untuk karya ID: ${karya.galeri_id}")
        delay(5000L)

        if (!viewCounted) {
            Log.d("DetailKarya", "🔥 5 detik berlalu, increment view...")

            RetrofitClient.instance.incrementView(karya.galeri_id)
                .enqueue(object : Callback<ViewResponse> {
                    override fun onResponse(
                        call: Call<ViewResponse>,
                        response: Response<ViewResponse>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let { body ->
                                currentViews = body.views
                                Log.d("DetailKarya", "✅ View: ${body.views}")

                                if (body.message.contains("milestone", ignoreCase = true)) {
                                    Log.d("DetailKarya", "🎉 ${body.message}")
                                }
                            }
                            viewCounted = true
                        }
                    }

                    override fun onFailure(call: Call<ViewResponse>, t: Throwable) {
                        Log.e("DetailKarya", "❌ Error: ${t.message}")
                    }
                })
        }
    }

    /**
     * Check apakah user sudah like karya ini
     */
    LaunchedEffect(karya.galeri_id) {
        val token = tokenManager.getBearerToken()
        if (token != null) {
            RetrofitClient.instance.checkLike(token, karya.galeri_id)
                .enqueue(object : Callback<LikeCheckResponse> {
                    override fun onResponse(
                        call: Call<LikeCheckResponse>,
                        response: Response<LikeCheckResponse>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                isLiked = it.is_liked
                                likesCount = it.likes
                                Log.d("CheckLike", "✅ isLiked: $isLiked, likes: $likesCount")
                            }
                        }
                    }

                    override fun onFailure(call: Call<LikeCheckResponse>, t: Throwable) {
                        Log.e("CheckLike", "❌ Error: ${t.message}")
                    }
                })
        }
    }

    /**
     * Function Toggle Like - TANPA NOTIFIKASI (Backend sudah handle)
     */
    fun toggleLike() {
        if (isLiking) return

        isLiking = true
        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            isLiking = false
            return
        }

        RetrofitClient.instance.toggleLike(token, karya.galeri_id)
            .enqueue(object : Callback<LikeResponse> {
                override fun onResponse(
                    call: Call<LikeResponse>,
                    response: Response<LikeResponse>
                ) {
                    isLiking = false

                    if (response.isSuccessful) {
                        response.body()?.let {
                            isLiked = it.is_liked
                            likesCount = it.likes

                            Log.d("ToggleLike", "✅ ${it.action}: likes = $likesCount")

                            Toast.makeText(
                                context,
                                if (it.is_liked) "❤️ Liked!" else "Unlike",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e("ToggleLike", "❌ Error: ${response.code()}")
                        Toast.makeText(context, "Gagal like karya", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    isLiking = false
                    Log.e("ToggleLike", "❌ Error: ${t.message}")
                    Toast.makeText(context, "Gagal like: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * UI DIALOG - FIXED: Clickable hierarchy agar button bisa diklik
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Block propagation to background */ }
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = "http://10.0.2.2:8000/storage/${karya.gambar}"
                    ),
                    contentDescription = karya.judul,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = karya.judul,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A0E24),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = karya.caption,
                    fontSize = 14.sp,
                    color = Color(0xFF5C5C5C),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        modifier = Modifier
                            .clickable(enabled = !isLiking) {
                                Log.d("DetailKarya", "🔘 Like button clicked!")
                                toggleLike()
                            },
                        shape = RoundedCornerShape(50),
                        color = if (isLiked) Color(0xFFFFE8EE) else Color(0xFFF0F0F0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            if (isLiking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF4A0E24)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFF4A0E24) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$likesCount",
                                fontSize = 14.sp,
                                color = if (isLiked) Color(0xFF4A0E24) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF0F0F0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "👁",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentViews",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                karya.uploader_name?.let {
                    Text(
                        text = "Diupload oleh $it",
                        fontSize = 12.sp,
                        color = Color(0xFF7A4E5A),
                        modifier = Modifier
                            .background(Color(0xFFFFE8EE), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                karya.tanggal_upload?.let {
                    Text(
                        text = "📅 $it",
                        fontSize = 12.sp,
                        color = Color(0xFF8C5F6E)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        Log.d("DetailKarya", "🔘 Close button clicked!")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A0E24)
                    )
                ) {
                    Text(
                        text = "Tutup",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}