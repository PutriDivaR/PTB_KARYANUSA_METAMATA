package com.example.karyanusa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.karyanusa.data.local.dao.KursusDao
import com.example.karyanusa.data.local.entity.KursusEntity

@Database(
    entities = [KursusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kursusDao(): KursusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karyanusa_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
