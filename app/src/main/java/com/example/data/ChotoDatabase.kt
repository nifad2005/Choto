package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CompressionHistoryEntity::class], version = 1, exportSchema = false)
abstract class ChotoDatabase : RoomDatabase() {
    abstract fun historyDao(): CompressionHistoryDao

    companion object {
        @Volatile
        private var instance: ChotoDatabase? = null

        fun getInstance(context: Context): ChotoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChotoDatabase::class.java,
                    "choto_database",
                ).build().also { instance = it }
            }
        }
    }
}
