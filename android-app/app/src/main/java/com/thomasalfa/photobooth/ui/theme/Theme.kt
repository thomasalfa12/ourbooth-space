package com.thomasalfa.photobooth.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Skema Warna Neo-Pop (Cerah & Fun)
private val NeoColorScheme = lightColorScheme(
    primary = NeoPurple,
    secondary = NeoPink,
    tertiary = NeoYellow,
    background = NeoCream,
    surface = Color.White,

    // Warna tulisan di atas warna-warna tersebut
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = NeoBlack,
    onBackground = NeoBlack,
    onSurface = NeoBlack,
)

@Composable
fun KubikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Kita set false agar warna brand TETAP MUNCUL (tidak ketimpa warna wallpaper HP user)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Untuk Photobooth dengan tema Neo-Pop, kita paksa pakai Skema yang Cerah (NeoColorScheme)
    // Tidak peduli HP-nya lagi mode gelap atau terang, biar UI konsisten ceria.
    val colorScheme = NeoColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar ikut warna background (Cream)
            window.statusBarColor = colorScheme.background.toArgb()
            // Icon status bar jadi gelap (biar kelihatan di background terang)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}