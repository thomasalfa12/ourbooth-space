package com.thomasalfa.photobooth.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    hasPlayedIntro: Boolean,
    onIntroFinished: () -> Unit,
    onStartSession: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    // --- INTRO LOGIC ---
    LaunchedEffect(Unit) {
        if (!hasPlayedIntro) {
            delay(3000) // Sesuaikan dengan durasi Lottie kamu
            onIntroFinished()
        }
    }

    // Gunakan Surface M3 agar background otomatis mengikuti tema (Cream)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- KONTEN UTAMA ---
            AnimatedVisibility(
                visible = hasPlayedIntro,
                enter = fadeIn(animationSpec = tween(1000)) // Fade in pelan elegan
            ) {
                HomeContent(onStartSession)
            }

            // --- INTRO ANIMATION ---
            AnimatedVisibility(
                visible = !hasPlayedIntro,
                exit = fadeOut(animationSpec = tween(800))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.intro_animation))
                    LottieAnimation(
                        composition = composition,
                        iterations = 1, // Main sekali saja
                        modifier = Modifier.fillMaxWidth(0.8f) // Jangan full width, kasih napas
                    )
                }
            }

            // --- HIDDEN ADMIN BUTTON (Pojok Kanan Atas) ---
            // Kita buat transparan total tapi bisa diklik
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .clip(CircleShape) // Biar ripple-nya bulat pas diklik (kalau mau debug)
                    .clickable { onOpenAdmin() }
            )
        }
    }
}

@Composable
fun HomeContent(onStartSession: () -> Unit) {
    // Animasi Pulse untuk tombol Start
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Header Text (Typography M3)
        Text(
            text = "WELCOME TO",
            style = MaterialTheme.typography.titleMedium, // Font agak kecil, caps
            color = MaterialTheme.colorScheme.primary, // Warna Ungu Brand
            letterSpacing = 4.sp // Spasi lebar biar elegan modern
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. LOGO
        Image(
            painter = painterResource(id = R.drawable.logo_kubik),
            contentDescription = "Kubik Logo",
            modifier = Modifier
                .width(400.dp)
                .heightIn(max = 180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(80.dp))

        // 3. START BUTTON (Gunakan ExtendedFloatingActionButton M3)
        // Ini adalah komponen standar M3 untuk tombol aksi utama yang mencolok
        Box(modifier = Modifier.scale(scale)) {
            ExtendedFloatingActionButton(
                onClick = onStartSession,
                containerColor = MaterialTheme.colorScheme.tertiary, // Warna Kuning/Accent
                contentColor = MaterialTheme.colorScheme.onTertiary, // Warna Teks Kontras
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                ),
                modifier = Modifier
                    .height(80.dp) // Tinggi tombol besar
                    .width(280.dp)
            ) {
                // Isi Tombol
                Text(
                    text = "TAP TO START",
                    style = MaterialTheme.typography.headlineSmall, // Font Tebal & Besar
                    fontWeight = FontWeight.Black // Extra Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Footer Text
        Text(
            text = "Touch anywhere to create memories",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Warna abu-abu standar M3
            textAlign = TextAlign.Center
        )
    }
}