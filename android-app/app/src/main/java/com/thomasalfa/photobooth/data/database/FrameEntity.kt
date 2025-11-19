package com.thomasalfa.photobooth.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frames")
data class FrameEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val displayName: String,    // Nama Frame (misal: "Natal 2025")
    val imagePath: String,      // Lokasi file PNG di HP
    val layoutType: String,     // "GRID" atau "STRIP"
    val category: String        // "Festival", "Wedding", "Event"
)