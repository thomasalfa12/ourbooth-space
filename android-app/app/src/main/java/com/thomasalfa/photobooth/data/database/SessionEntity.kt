package com.thomasalfa.photobooth.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionUuid: String,       // Kode unik (misal: UUID random)
    val timestamp: Long,           // Waktu foto diambil
    val totalPhotos: Int,          // Jumlah foto mentah
    val finalGridPath: String?,    // Path hasil grid final (setelah edit)
    val gifPath: String?,
    val isPrinted: Boolean = false // Status apakah dicetak
)