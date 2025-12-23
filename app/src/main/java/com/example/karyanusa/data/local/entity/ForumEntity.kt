package com.example.karyanusa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forum_pertanyaan")
data class ForumEntity(
    @PrimaryKey val pertanyaan_id: Int,
    val user_id: Int,
    val isi: String,
    val tanggal: String,
    val updated_at: String?,
    val image_forum: String?, // JSON string
    val jawaban_count: Int,
    val user_nama: String?,
    val user_username: String?,
    val user_foto_profile: String?,
    val cached_at: Long
)