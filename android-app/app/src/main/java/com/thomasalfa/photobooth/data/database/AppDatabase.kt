package com.thomasalfa.photobooth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Update version ke 2 karena ada perubahan struktur
// Tambahkan SessionEntity dan SessionPhotoEntity ke daftar entities
@Database(
    entities = [FrameEntity::class, SessionEntity::class, SessionPhotoEntity::class],
    version = 2, // NAIKKAN VERSI
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun frameDao(): FrameDao
    abstract fun sessionDao(): SessionDao // Tambahkan ini

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kubik_database"
                )
                    // PENTING: Karena kita ubah struktur saat develop, pakai ini biar gak crash.
                    // (Data lama akan hilang saat update aplikasi, tapi aman buat fase dev)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}