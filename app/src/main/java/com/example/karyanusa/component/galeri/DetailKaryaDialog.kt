package com.example.karyanusa.component.galeri

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.karyanusa.network.KaryaData

@Composable
fun DetailKaryaDialog(
    karya: KaryaData,
    onDismiss: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },       // klik luar nutup
        contentAlignment = Alignment.Center
    ) {

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // =======================
                //  GAMBAR
                // =======================
                Image(
                    painter = rememberAsyncImagePainter(
                        model = "http://10.0.2.2:8000/storage/${karya.gambar}"
                    ),
                    contentDescription = karya.judul,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(16.dp))


                // =======================
                //  JUDUL
                // =======================
                Text(
                    text = karya.judul,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A0E24),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))


                // =======================
                //  DESKRIPSI
                // =======================
                Text(
                    text = karya.caption,
                    color = Color(0xFF5C5C5C),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(16.dp))


                // =======================
                //  UPLOADER
                // =======================
                Text(
                    text = "Diupload oleh ${karya.uploader_name}",
                    color = Color(0xFF7A4E5A),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            Color(0xFFFFE8EE),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )

                Spacer(Modifier.height(8.dp))


                // =======================
                //  TANGGAL UPLOAD (opsional)
                // =======================
                if (karya.tanggal_upload != null) {
                    Text(
                        text = "Tanggal: ${karya.tanggal_upload}",
                        color = Color(0xFF8C5F6E),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(12.dp))
                }


                // =======================
                //  TOMBOL TUTUP
                // =======================
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E24)),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(45.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Tutup",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
