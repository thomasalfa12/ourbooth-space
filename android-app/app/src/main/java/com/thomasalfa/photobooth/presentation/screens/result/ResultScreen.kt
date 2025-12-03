package com.thomasalfa.photobooth.presentation.screens.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.utils.PhotoPrintAdapter
import com.thomasalfa.photobooth.utils.SupabaseManager
import com.thomasalfa.photobooth.utils.layout.LayoutProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultScreen(
    photoPaths: List<String>,   // Foto yang sudah diurutkan dari Editor
    selectedFrame: FrameEntity, // Frame yang sudah dipilih di awal (FIXED)
    sessionUuid: String,
    onRetake: () -> Unit,
    onFinishClicked: (String) -> Unit // Mengirim URL QR Final
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var finalLayoutPath by remember { mutableStateOf<String?>(null) }

    // UI States
    var isGeneratingLayout by remember { mutableStateOf(true) } // Default true karena langsung proses pas masuk
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UPLOAD STATES (Smart Background Process)
    var uploadedUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showFinalizingOverlay by remember { mutableStateOf(false) }

    // --- 1. OTOMATIS GENERATE LAYOUT & UPLOAD SAAT MASUK ---
    LaunchedEffect(Unit) { // Run ONCE saat layar dibuka
        isGeneratingLayout = true
        isUploading = true
        errorMessage = null

        val job = launch(Dispatchers.Default) {
            var photoBitmaps: List<Bitmap> = emptyList()
            var frameBitmap: Bitmap? = null

            try {
                // A. GENERATE HIGH-RES LAYOUT
                val options = BitmapFactory.Options().apply { inSampleSize = 2 } // Optimasi memori sedikit
                photoBitmaps = photoPaths.mapNotNull { path -> BitmapFactory.decodeFile(path, options) }

                if (photoBitmaps.isNotEmpty()) {
                    frameBitmap = BitmapFactory.decodeFile(selectedFrame.imagePath)

                    if (frameBitmap != null) {
                        val resultBitmap = LayoutProcessor.processLayout(
                            photos = photoBitmaps,
                            layoutType = selectedFrame.layoutType,
                            frameBitmap = frameBitmap
                        )

                        // Save Final File
                        val file = File(context.cacheDir, "final_${System.currentTimeMillis()}.jpg")
                        val stream = FileOutputStream(file)
                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        stream.flush()
                        stream.close()

                        // Update UI: Layout Ready untuk Preview & Print
                        withContext(Dispatchers.Main) {
                            finalLayoutPath = file.absolutePath
                            isGeneratingLayout = false
                        }

                        // B. BACKGROUND UPLOAD (Fire & Forget)
                        // Langsung upload file yang baru saja digenerate
                        val uploadedLink = SupabaseManager.uploadFile(file)

                        if (uploadedLink != null) {
                            SupabaseManager.updateFinalSession(sessionUuid, uploadedLink)
                            withContext(Dispatchers.Main) {
                                uploadedUrl = uploadedLink
                                isUploading = false // Upload Selesai!
                            }
                        } else {
                            withContext(Dispatchers.Main) { isUploading = false } // Gagal diam-diam (retry nanti)
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "Error: ${t.message}"
                    isGeneratingLayout = false
                    isUploading = false
                }
            } finally {
                photoBitmaps.forEach { it.recycle() }
                frameBitmap?.recycle()
            }
        }
    }

    // --- FUNCTION: HANDLE FINISH ---
    fun executeFinish() {
        if (uploadedUrl != null) {
            // BEST CASE: Upload sudah selesai duluan di background
            val finalQrUrl = "https://ourbooth-space.vercel.app/?id=$sessionUuid"
            onFinishClicked(finalQrUrl)
        } else {
            // WORST CASE: Internet lambat, upload belum beres
            showFinalizingOverlay = true

            scope.launch(Dispatchers.IO) {
                // Tunggu sebentar (polling)
                var attempt = 0
                while (uploadedUrl == null && attempt < 20) { // Max 10 detik
                    delay(500)
                    if (uploadedUrl != null) break
                    attempt++
                }

                // Jika masih gagal background, coba paksa upload ulang
                if (uploadedUrl == null && finalLayoutPath != null) {
                    try {
                        val file = File(finalLayoutPath!!)
                        val link = SupabaseManager.uploadFile(file)
                        if (link != null) {
                            SupabaseManager.updateFinalSession(sessionUuid, link)
                            uploadedUrl = link
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    showFinalizingOverlay = false
                    if (uploadedUrl != null) {
                        val finalQrUrl = "https://ourbooth-space.vercel.app/?id=$sessionUuid"
                        onFinishClicked(finalQrUrl)
                    } else {
                        errorMessage = "Upload Failed. Check Internet."
                    }
                }
            }
        }
    }

    // --- FUNCTION: PRINT ---
    fun handlePrint() {
        finalLayoutPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path) ?: return
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Kubik_Print_${System.currentTimeMillis()}"
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_4X6)
                .setResolution(PrintAttributes.Resolution("id", "Epson", 300, 300))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, PhotoPrintAdapter(context, bitmap, jobName), attributes)
        }
    }

    // --- UI LAYOUT ---
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // KIRI: PREVIEW FINAL (Besar & Jelas)
                Card(
                    modifier = Modifier.weight(1.3f).fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isGeneratingLayout) {
                            ProcessingState() // Loading Animation
                        } else if (errorMessage != null) {
                            ErrorState(errorMessage!!)
                        } else {
                            finalLayoutPath?.let { path ->
                                Box {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "Final Result",
                                        modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    // Indikator Upload Kecil (UX: Biar user tau sistem sedang bekerja)
                                    if (isUploading) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            shadowElevation = 4.dp
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Cloud Sync...", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // KANAN: ACTION PANEL (Simpel, Tanpa Frame Selector)
                Column(
                    modifier = Modifier.weight(0.7f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("MEMORIES READY!", style = MaterialTheme.typography.labelLarge, color = Color.Gray, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Print or Share?", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.height(32.dp))

                    // ACTION BUTTONS
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // 1. PRINT BUTTON
                        OutlinedButton(
                            onClick = { handlePrint() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = finalLayoutPath != null && !isGeneratingLayout
                        ) {
                            Icon(Icons.Filled.Print, null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("PRINT PHOTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. FINISH BUTTON (Primary)
                        Button(
                            onClick = { executeFinish() },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = finalLayoutPath != null && !isGeneratingLayout
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("GET QR CODE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                    val statusText = if (uploadedUrl != null) "Ready to Scan" else if (isUploading) "Uploading..." else "Process & Finish"
                                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. RETAKE BUTTON
                        TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
                            Text("Start Over (Delete All)", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // --- FINALIZING OVERLAY (Blocker) ---
        AnimatedVisibility(
            visible = showFinalizingOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(60.dp), strokeWidth = 5.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Finalizing...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Syncing high-res photo to cloud", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---
@Composable
fun ProcessingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_anim)) // Pastikan ada animasinya
        LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(180.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Developing Photo...", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ErrorState(msg: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Oops!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(msg, color = MaterialTheme.colorScheme.error)
    }
}