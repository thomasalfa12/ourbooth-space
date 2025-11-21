package com.thomasalfa.photobooth.presentation.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.ui.theme.NeoCream
import com.thomasalfa.photobooth.ui.theme.NeoPurple
import com.thomasalfa.photobooth.utils.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun UploadProgressScreen(
    photoPath: String?,
    gifPath: String?,
    onUploadSuccess: (String) -> Unit, // String ini adalah Link Website untuk QR
    onUploadFailed: () -> Unit
) {
    // Animasi Pesawat
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.paper_plane))
    var statusText by remember { mutableStateOf("Connecting to Cloud...") }

    LaunchedEffect(Unit) {
        delay(1000) // Animasi intro sebentar

        if (photoPath == null) {
            onUploadFailed()
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                // 1. UPLOAD FOTO
                statusText = "Uploading Photo..."
                val photoFile = File(photoPath)
                val photoUrl = SupabaseManager.uploadFile(photoFile)

                if (photoUrl == null) throw Exception("Gagal upload foto")

                // 2. UPLOAD GIF (Jika Ada)
                var gifUrl: String? = null
                if (gifPath != null) {
                    statusText = "Uploading Live Photo..."
                    val gifFile = File(gifPath)
                    gifUrl = SupabaseManager.uploadFile(gifFile)
                }

                // 3. SIMPAN KE DATABASE (SESSION)
                statusText = "Finalizing Session..."

                // Generate ID Unik untuk Web
                val sessionUuid = UUID.randomUUID().toString()

                val dbSuccess = SupabaseManager.insertSession(
                    uuid = sessionUuid,
                    photoUrl = photoUrl,
                    gifUrl = gifUrl
                )

                if (dbSuccess) {
                    withContext(Dispatchers.Main) {
                        statusText = "Sent!"
                        delay(500)

                        // --- INI KUNCINYA ---
                        // Link yang dikirim ke QR Code bukan link file JPG,
                        // tapi link WEBSITE Anda dengan parameter ID.
                        val webLink = "https://kubik-gallery.vercel.app/?id=$sessionUuid"

                        onUploadSuccess(webLink)
                    }
                } else {
                    throw Exception("Gagal simpan database")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onUploadFailed()
                }
            }
        }
    }

    // UI
    Box(
        modifier = Modifier.fillMaxSize().background(NeoCream),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(300.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(statusText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeoPurple)
        }
    }
}