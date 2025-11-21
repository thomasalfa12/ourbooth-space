package com.thomasalfa.photobooth.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_photos",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE // Kalau Sesi dihapus, foto2nya ikut kehapus di DB
        )
    ]
)
data class SessionPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,     // Relasi ke tabel sessions
    val filePath: String,    // Lokasi file di HP
    val isFinalResult: Boolean = false // True jika ini foto hasil jadi, False jika foto mentah
)