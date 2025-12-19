package com.example.karyanusa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enrollment")
data class EnrollmentEntity(
    @PrimaryKey
    val enrollment_id: Int,
    val user_id: Int,
    val kursus_id: Int,
    val progress: Int,
    val status: String,
    val last_updated: Long = System.currentTimeMillis()
)