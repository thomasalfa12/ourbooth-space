package com.thomasalfa.photobooth.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thomasalfa.photobooth.ui.theme.*

// 1. TOMBOL YANG MEMBAL (BOUNCY)
@Composable
fun BouncyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeoYellow,
    textColor: Color = NeoBlack,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animasi Scale saat ditekan
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(60.dp)
            .clip(RoundedCornerShape(20.dp)) // Sudut membulat modern
            .background(if (enabled) color else Color.LightGray)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Matikan ripple bawaan android yg kaku
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) textColor else Color.Gray
            )
        }
    }
}

// 2. LOADING ANIMASI TITIK MENARI
@Composable
fun FunLoading(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Animasi 3 titik naik turun bergantian
    @Composable
    fun Dot(delay: Int) {
        val dy by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "dot"
        )

        Box(
            modifier = Modifier
                .offset(y = dy.dp)
                .size(15.dp)
                .clip(CircleShape)
                .background(NeoPurple)
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(0)
        Dot(150)
        Dot(300)
    }
}

// 3. PROGRESS BAR SESI (Garis Putus-putus Modern)
@Composable
fun SessionProgressIndicator(
    totalShots: Int,
    currentShot: Int, // Index foto saat ini (dimulai dari 1)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth() // Pastikan memenuhi lebar layar
            .padding(horizontal = 24.dp), // Beri jarak aman dari pinggir hp
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Jarak antar bar
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalShots) { index ->
            // Logic Status:
            // index < currentShot -> Sudah difoto (NeoGreen / Selesai)
            // index == currentShot -> Sedang difoto (NeoPink / Aktif)
            // index > currentShot -> Belum (Abu-abu)

            // Kita asumsikan 'currentShot' adalah jumlah foto yang SUDAH diambil.
            // Jadi baris ke-0 akan menyala jika currentShot >= 1
            val isTaken = index < currentShot

            // Animasi Tinggi Bar (biar ada efek 'hidup')
            // Jika ini adalah foto terakhir yang baru saja diambil, dia akan sedikit lebih tinggi
            val isLastTaken = index == currentShot - 1

            val height by animateDpAsState(
                targetValue = if (isLastTaken) 14.dp else 8.dp, // Sedikit membesar saat aktif
                label = "height",
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            val color by animateColorAsState(
                targetValue = if (isTaken) NeoPink else Color.LightGray.copy(alpha = 0.5f),
                label = "color"
            )

            Box(
                modifier = Modifier
                    .weight(1f) // KUNCI UTAMA: Membagi rata lebar berapapun jumlah fotonya
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

// 4. Shutter Button Keren
@Composable
fun ShutterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    BouncyButton(
        text = "SNAP!",
        onClick = onClick,
        color = NeoPink,
        textColor = Color.White,
        enabled = isEnabled,
        modifier = modifier.width(120.dp)
    )
}

// 5. Control Button Kecil
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = NeoBlack.copy(alpha = 0.6f))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = NeoBlack.copy(alpha = 0.6f))
    }
}