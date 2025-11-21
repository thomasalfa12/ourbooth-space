package com.thomasalfa.photobooth.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.presentation.components.BouncyButton
import com.thomasalfa.photobooth.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    hasPlayedIntro: Boolean, // Parameter dari MainActivity
    onIntroFinished: () -> Unit, // Callback lapor selesai
    onStartSession: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    // Logika Intro: Jika hasPlayedIntro false, mainkan animasi.
    // Jika true, langsung tampilkan konten.

    LaunchedEffect(Unit) {
        if (!hasPlayedIntro) {
            delay(3000) // Durasi Intro
            onIntroFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream) // Background Bersih
    ) {

        // --- KONTEN UTAMA (Muncul setelah intro atau jika sudah pernah intro) ---
        AnimatedVisibility(
            visible = hasPlayedIntro,
            enter = fadeIn(animationSpec = tween(800))
        ) {
            MainContent(onStartSession, onOpenAdmin)
        }

        // --- INTRO SCREEN (Hanya muncul jika hasPlayedIntro = false) ---
        AnimatedVisibility(
            visible = !hasPlayedIntro,
            exit = fadeOut(animationSpec = tween(800))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(NeoCream), // Cover full screen
                contentAlignment = Alignment.Center
            ) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.intro_animation))
                LottieAnimation(
                    composition = composition,
                    iterations = 1,
                    modifier = Modifier.size(250.dp)
                )
            }
        }
    }
}

@Composable
fun MainContent(onStartSession: () -> Unit, onOpenAdmin: () -> Unit) {
    // Animasi Denyut Halus
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing), // Lebih pelan biar elegan
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LOGO
            Image(
                painter = painterResource(id = R.drawable.logo_kubik),
                contentDescription = "Logo",
                modifier = Modifier
                    .width(500.dp) // Ukuran proporsional
                    .heightIn(max = 200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(80.dp))

            // TOMBOL MULAI (Style Pill Modern)
            Box(modifier = Modifier.scale(scale)) {
                BouncyButton(
                    text = "TAP TO START",
                    onClick = onStartSession,
                    color = NeoYellow,
                    textColor = NeoBlack,
                    modifier = Modifier
                        .width(280.dp)
                        .height(75.dp)
                        // Shadow yang lembut (Soft Shadow)
                        .shadow(12.dp, RoundedCornerShape(50), spotColor = NeoPurple.copy(alpha=0.5f))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Touch screen to begin",
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        // TOMBOL ADMIN (Hidden tapi area besar di pojok)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(120.dp) // Area tap besar
                .clickable { onOpenAdmin() }
        )
    }
}