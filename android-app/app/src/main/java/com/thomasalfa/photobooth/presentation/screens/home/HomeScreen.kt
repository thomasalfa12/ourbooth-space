package com.thomasalfa.photobooth.presentation.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.presentation.components.BouncyButton
import com.thomasalfa.photobooth.ui.theme.*

@Composable
fun HomeScreen(
    onStartSession: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    // Animasi Rotasi untuk hiasan background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream) // Background Terang
    ) {
        // --- LAYER 1: BACKGROUND PATTERN (Polka Dot Dinamis) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCirclePattern(NeoPurple.copy(alpha = 0.05f), radius = 40f, spacing = 100f)
        }

        // Hiasan Lingkaran Besar di pojok (Visual Interest)
        Box(modifier = Modifier
            .offset(x = (-100).dp, y = (-100).dp)
            .size(400.dp)
            .rotate(rotation) // Muter pelan
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(NeoYellow.copy(alpha = 0.4f), radius = size.minDimension / 2)
                drawCircle(NeoPink.copy(alpha = 0.4f), radius = size.minDimension / 3)
            }
        }

        // --- LAYER 2: CONTENT ---
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // JUDUL BESAR & FUN
            Text(
                text = "KUBIK",
                style = MaterialTheme.typography.displayLarge,
                color = NeoPurple,
                fontWeight = FontWeight.Black,
                fontSize = 100.sp,
                letterSpacing = 5.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PHOTO",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeoBlack,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BOOTH",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeoPink, // Beda warna biar pop
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // CALL TO ACTION (Bouncy Button Besar)
            BouncyButton(
                text = "TAP TO START",
                onClick = onStartSession,
                color = NeoYellow,
                modifier = Modifier.width(250.dp).height(70.dp)
            )
        }

        // COPYRIGHT
        Text(
            text = "Designed for Fun Moments ✨",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
        )

        // HIDDEN ADMIN (Pojok Kanan Atas - Transparan)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(80.dp)
                .clickable { onOpenAdmin() }
        )
    }
}

// Helper untuk gambar pola background
fun DrawScope.drawCirclePattern(color: Color, radius: Float, spacing: Float) {
    var x = 0f
    while (x < size.width) {
        var y = 0f
        while (y < size.height) {
            drawCircle(color, radius, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}