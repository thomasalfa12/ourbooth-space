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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    var statusText by remember { mutableStateOf("Preparing...") }

    LaunchedEffect(Unit) {
        if (photoPath == null) {
            onUploadFailed()
            return@LaunchedEffect
        }

        try {
            // 1. Cek Kesiapan DB (Sekarang proses ini INSTAN)
            // Kita pakai snapshotFlow cuma buat safety kalau internet putus di awal
            if (!isBackgroundUploadDone) {
                statusText = "Initializing..."
                androidx.compose.runtime.snapshotFlow { isBackgroundUploadDone }
                    .filter { it }
                    .first()
            }

            // Jika ada error fatal di DB Init (bukan video), baru error
            if (backgroundError != null) {
                // Opsional: Abaikan error video, fokus ke DB
                // throw Exception(backgroundError)
            }

            // 2. Upload Foto Final
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) { statusText = "Uploading Photo..." }

                val photoFile = File(photoPath)
                if (!photoFile.exists()) throw Exception("Photo file not found")

                val finalPhotoUrl = SupabaseManager.uploadFile(photoFile)
                    ?: throw Exception("Upload Failed")

                // 3. Update DB (Foto Only)
                withContext(Dispatchers.Main) { statusText = "Finalizing..." }

                val updateSuccess = SupabaseManager.updateFinalSession(sessionUuid, finalPhotoUrl)

                if (updateSuccess) {
                    withContext(Dispatchers.Main) {
                        statusText = "Done!"
                        delay(500)
                        onUploadSuccess("https://ourbooth-space.vercel.app/?id=$sessionUuid")
                    }
                } else {
                    throw Exception("Connection Error")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onUploadFailed()
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