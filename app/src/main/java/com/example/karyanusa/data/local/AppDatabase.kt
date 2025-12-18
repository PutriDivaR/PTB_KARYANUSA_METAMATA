package com.example.karyanusa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.karyanusa.data.local.dao.KursusDao
import com.example.karyanusa.data.local.dao.MateriDao
import com.example.karyanusa.data.local.entity.KursusEntity
import com.example.karyanusa.data.local.entity.MateriEntity

@Database(
    entities = [KursusEntity::class, MateriEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kursusDao(): KursusDao
    abstract fun materiDao(): MateriDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karyanusa_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
