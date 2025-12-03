package com.thomasalfa.photobooth.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 1. NEW BRAND PALETTE (KUBIK BOOTH 2.0)
// ==========================================
// Konsep: Professional Blue & Golden Ratio Accent

val KubikBg = Color(0xFFFAFAFA)       // Background Putih Bersih
val KubikBlue = Color(0xFF2962FF)     // Primary Brand (Electric Blue)
val KubikBlueLight = Color(0xFF768FFF) // Secondary Blue
val KubikGold = Color(0xFFFFC107)     // Action Buttons (Golden Yellow)
val KubikGoldDark = Color(0xFFFFA000)
val KubikBlack = Color(0xFF1E1E2C)    // Teks Utama (Dark Blue-Grey)
val KubikGrey = Color(0xFF757575)     // Teks Sekunder
val KubikError = Color(0xFFD32F2F)    // Merah Error
val KubikSuccess = Color(0xFF388E3C)  // Hijau Sukses


// ==========================================
// 2. COMPATIBILITY LAYER (MAPPING)
// ==========================================
// Ini adalah "Jembatan" agar kode lama (CaptureScreen, ResultScreen, dll)
// yang memanggil "Neo..." tidak error, tapi otomatis memakai warna baru.

val NeoCream = KubikBg          // Background lama -> jadi Putih Bersih
val NeoPurple = KubikBlue       // Ungu lama -> jadi Biru Brand
val NeoYellow = KubikGold       // Kuning lama -> jadi Emas
val NeoPink = KubikGold         // Pink lama (biasanya tombol) -> jadi Emas (Action)
val NeoBlack = KubikBlack       // Hitam lama -> jadi Hitam Baru
val NeoGreen = KubikSuccess     // Hijau lama -> jadi Hijau Baru
val NeoBlue = KubikBlue         // Biru lama -> jadi Biru Brand