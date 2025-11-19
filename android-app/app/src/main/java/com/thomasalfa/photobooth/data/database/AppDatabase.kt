package com.thomasalfa.photobooth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FrameEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun frameDao(): FrameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kubik_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}