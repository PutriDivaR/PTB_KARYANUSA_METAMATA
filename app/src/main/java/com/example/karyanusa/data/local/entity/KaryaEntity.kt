package com.example.karyanusa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "karya")
data class KaryaEntity(
    @PrimaryKey
    val galeri_id: Int,
    val user_id: Int,
    val judul: String,
    val caption: String,
    val gambar: String,
    val tanggal_upload: String?,
    val created_at: String?,
    val updated_at: String?,
    val views: Int = 0,
    val likes: Int = 0,
    val uploader_name: String?,
    val last_updated: Long = System.currentTimeMillis()
)
