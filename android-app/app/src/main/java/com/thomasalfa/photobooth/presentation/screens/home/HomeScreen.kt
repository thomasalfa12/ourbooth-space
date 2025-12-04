package com.thomasalfa.photobooth.presentation.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.ui.theme.*

@Composable
fun HomeScreen(
    // Parameter intro dihapus karena sudah tidak dipakai
    onStartSession: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- MAIN LAYOUT (GOLDEN RATIO SPLIT) ---
            Row(modifier = Modifier.fillMaxSize()) {

                // 1. LEFT SIDE (62% - Hero Image)
                // Full Screen Image untuk impresi visual yang kuat
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {
                    HeroVisualSection()
                }

                // 2. RIGHT SIDE (38% - Interaction)
                // Area kontrol berwarna putih bersih
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(Color.White)
                ) {
                    ControlSection(onStartSession)
                }
            }

            // --- HIDDEN ADMIN BUTTON (Pojok Kanan Atas) ---
            // Area transparan untuk akses teknisi
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(80.dp) // Area sentuh cukup besar
                    .clip(CircleShape)
                    .clickable(onClick = onOpenAdmin)
            )
        }
    }
}

@Composable
fun HeroVisualSection() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. HERO IMAGE (GANTI 'R.drawable.hero_image' DENGAN FOTO TERBAIK ANDA)
        // Jika belum ada, sementara pakai logo atau warna solid
        Image(
            painter = painterResource(id = R.drawable.hero_image), // Pastikan file ini ada di drawable!
            contentDescription = "Hero Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. GRADIENT OVERLAY (PENTING!)
        // Supaya teks putih tetap terbaca walau background gambarnya terang
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f) // Gelap di bawah
                        )
                    )
                )
        )

        // 3. TEXT COPYWRITING
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(40.dp)
        ) {
            Text(
                text = "CAPTURE\nTHE MOMENT",
                style = MaterialTheme.typography.displayLarge, // Font sangat besar
                fontWeight = FontWeight.Black,
                color = KubikBlue,
                lineHeight = 60.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Make every smile count.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun ControlSection(onStartSession: () -> Unit) {
    // Animasi Pulse Halus untuk Tombol Start
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // KUNCI: Memisahkan Atas dan Bawah
    ) {
        // --- BAGIAN ATAS: LOGO & SALAM ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Brand
            Image(
                painter = painterResource(id = R.drawable.logo_kubik),
                contentDescription = "Kubik Booth",
                modifier = Modifier
                    .width(200.dp)
                    .heightIn(max = 100.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Text Sambutan
            Text(
                text = "Ready to Shine?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = KubikBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Touch the button below to start.",
                style = MaterialTheme.typography.bodyLarge,
                color = KubikGrey,
                textAlign = TextAlign.Center
            )
        }

        // --- BAGIAN BAWAH: TOMBOL START & FOOTER ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // TOMBOL START (The Main Action)
            Box(
                modifier = Modifier
                    .scale(scale)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = KubikGold.copy(alpha = 0.6f)
                    )
            ) {
                Button(
                    onClick = onStartSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KubikGold, // Warna Kuning Emas
                        contentColor = KubikBlack   // Teks Hitam
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth() // Lebar penuh
                        .height(80.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Camera, // Icon Kamera Standar
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "START SESSION",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Branding
            Text(
                text = "Powered by KubikBooth v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}