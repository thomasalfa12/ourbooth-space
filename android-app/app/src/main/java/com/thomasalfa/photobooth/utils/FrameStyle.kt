package com.thomasalfa.photobooth.utils

import android.graphics.Color

enum class LayoutType { GRID_2X3, STRIP_2X1 }

enum class FrameStyle(
    val label: String,
    val layoutType: LayoutType,
    val baseColor: Int // Warna dasar background untuk fallback
) {
    // Desain 1: Grid Biasa (Natal)
    CHRISTMAS("Merry Christmas", LayoutType.GRID_2X3, Color.parseColor("#C62828")), // Merah Natal

    // Desain 2: Strip Potong 2 (Tahun Baru)
    NEW_YEAR("Happy New Year", LayoutType.STRIP_2X1, Color.parseColor("#212121"))   // Hitam Elegan
}