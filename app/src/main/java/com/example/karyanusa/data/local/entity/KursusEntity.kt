package com.example.karyanusa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kursus")
data class KursusEntity(
    @PrimaryKey
    val kursus_id: Int,
    val judul: String,
    val deskripsi: String,
    val pengrajin_nama: String,
    val thumbnail: String?
)
