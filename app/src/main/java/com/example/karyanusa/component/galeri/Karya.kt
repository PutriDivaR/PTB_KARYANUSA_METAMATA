package com.example.karyanusa.component.galeri


import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

data class Karya(
    val id: Int,
    var nama: String,
    var deskripsi: String,
    val gambarUri: Uri? = null,
    val gambarBitmap: Bitmap? = null,
    val uploader: String = "Anonim"
)

object KaryaRepository {
    val daftarKarya = mutableStateListOf<Karya>()
}



