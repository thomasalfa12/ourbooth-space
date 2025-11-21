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

@Composable
fun UploadProgressScreen(
    photoPath: String?,
    sessionUuid: String,
    isBackgroundUploadDone: Boolean,
    backgroundError: String?,
    onUploadSuccess: (String) -> Unit,
    onUploadFailed: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.paper_plane))
    var statusText by remember { mutableStateOf("Finalizing...") }

    LaunchedEffect(Unit) {
        delay(500)

        if (photoPath == null) { onUploadFailed(); return@LaunchedEffect }

        withContext(Dispatchers.IO) {
            try {
                // 1. CEK STATUS BACKGROUND (Tunggu GIF & DB Init Selesai)
                while (!isBackgroundUploadDone) {
                    withContext(Dispatchers.Main) { statusText = "Syncing data..." }
                    delay(200) // Cek setiap 0.2 detik
                }

                // Jika background error, lempar exception
                if (backgroundError != null) {
                    throw Exception("Background Error: $backgroundError")
                }

                // 2. UPLOAD FOTO FINAL (Hanya ini yang ditunggu User)
                withContext(Dispatchers.Main) { statusText = "Uploading Photo..." }
                val photoFile = File(photoPath)
                val finalPhotoUrl = SupabaseManager.uploadFile(photoFile) ?: throw Exception("Upload Foto Gagal")

                // 3. UPDATE DATABASE (Patch data terakhir)
                withContext(Dispatchers.Main) { statusText = "Finishing up..." }
                val updateSuccess = SupabaseManager.updateFinalSession(sessionUuid, finalPhotoUrl)

                if (updateSuccess) {
                    withContext(Dispatchers.Main) {
                        statusText = "Done!"
                        delay(500)
                        val webLink = "https://ourbooth-space.vercel.app/?id=$sessionUuid"
                        onUploadSuccess(webLink)
                    }
                } else {
                    throw Exception("Gagal Update Database")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onUploadFailed()
                }
            }
        }
    }

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