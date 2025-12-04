package com.thomasalfa.photobooth.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val KubikColorScheme = lightColorScheme(
    primary = KubikBlue,
    onPrimary = Color.White,
    primaryContainer = KubikBlueLight.copy(alpha = 0.2f),

    secondary = KubikGold,
    onSecondary = KubikBlack, // Teks hitam di atas emas agar terbaca

    tertiary = KubikGoldDark,
    onTertiary = Color.White,

    background = KubikBg,
    onBackground = KubikBlack,

    surface = Color.White,
    onSurface = KubikBlack,

    error = KubikError
)

@Composable
fun KubikTheme(
    // Kita force Light Mode agar branding konsisten di Photobooth
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KubikColorScheme.background.toArgb()
            // Icon status bar gelap
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = KubikColorScheme,
        typography = Typography, // Pastikan file Typography.kt Anda ada
        content = content
    )
}