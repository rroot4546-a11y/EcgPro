package com.roox.ecgpro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.roox.ecgpro.data.dao.EcgDao
import com.roox.ecgpro.data.model.EcgRecord
import com.roox.ecgpro.data.model.ChatMessage
import com.roox.ecgpro.data.model.TrainingRecord

@Database(entities = [EcgRecord::class, ChatMessage::class, TrainingRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ecgDao(): EcgDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ecgpro_db")
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
