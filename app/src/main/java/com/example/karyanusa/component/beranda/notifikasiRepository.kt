package com.example.karyanusa.component.beranda

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.*

data class Notifikasi(
    val pesan: String,
    val waktu: String
)

object NotifikasiRepository {
    val daftarNotifikasi = mutableStateListOf<Notifikasi>()

    fun tambahNotifikasi(pesan: String) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val waktuSekarang = sdf.format(Date())
        daftarNotifikasi.add(0, Notifikasi(pesan, waktuSekarang)) // tambah di atas biar yang terbaru muncul duluan
    }
}
