package com.thomasalfa.photobooth.presentation.screens.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.PhotoPrintAdapter
import com.thomasalfa.photobooth.utils.layout.LayoutProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultScreen(
    photoPaths: List<String>,
    // parameter boomerangPaths SUDAH DIHAPUS (Tidak butuh)
    onRetake: () -> Unit,
    onFinishClicked: (String) -> Unit // Callback cuma butuh path foto final
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val settingsManager = remember { SettingsManager(context) }

    // Load Data
    val activeEvent by settingsManager.activeEventFlow.collectAsState(initial = "ALL")
    val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    // Filter Logic
    val eventFilteredFrames = remember(allFrames, activeEvent) {
        when (activeEvent) {
            "ALL" -> allFrames
            "Default Only" -> allFrames.filter { it.category == "Default" }
            else -> allFrames.filter { it.category == "Default" || it.category == activeEvent }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All Frames", "Grid 2x3", "Strip Cut")

    val finalFilteredFrames = remember(eventFilteredFrames, selectedTabIndex) {
        when (selectedTabIndex) {
            1 -> eventFilteredFrames.filter { it.layoutType == "GRID" }
            2 -> eventFilteredFrames.filter { it.layoutType == "STRIP" }
            else -> eventFilteredFrames
        }
    }

    var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) }
    var finalLayoutPath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(finalFilteredFrames) {
        if (finalFilteredFrames.isNotEmpty() && (selectedFrame == null || !finalFilteredFrames.contains(selectedFrame))) {
            selectedFrame = finalFilteredFrames[0]
        }
    }

    // --- PROCESSOR (Hanya Foto, Tanpa GIF) ---
    LaunchedEffect(photoPaths, selectedFrame) {
        if (selectedFrame != null && photoPaths.isNotEmpty()) {
            isProcessing = true
            errorMessage = null
            // Delay kecil biar transisi UI smooth, bukan karena berat proses
            delay(100)

            // Pakai Dispatchers.Default untuk proses CPU berat
            val job = launch(Dispatchers.Default) {
                try {
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val photoBitmaps = photoPaths.mapNotNull { path -> BitmapFactory.decodeFile(path, options) }

                    if (photoBitmaps.isNotEmpty()) {
                        val frameBitmap = BitmapFactory.decodeFile(selectedFrame!!.imagePath)
                        if (frameBitmap != null) {
                            // Proses Layout (Cepat karena pakai Rect)
                            val resultBitmap = LayoutProcessor.processLayout(
                                photos = photoBitmaps,
                                layoutType = selectedFrame!!.layoutType,
                                frameBitmap = frameBitmap
                            )

                            val file = File(context.cacheDir, "final_${System.currentTimeMillis()}.jpg")
                            val stream = FileOutputStream(file)
                            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            stream.flush(); stream.close()

                            // Cleanup
                            photoBitmaps.forEach { it.recycle() }
                            frameBitmap.recycle()

                            // Update UI
                            finalLayoutPath = file.absolutePath
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    errorMessage = "Ouch! ${t.message}"
                }
            }

            job.join() // Tunggu layout selesai
            isProcessing = false
        }
    }

    fun handlePrint() {
        finalLayoutPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path) ?: return
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "KubikCam_${System.currentTimeMillis()}"
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_INDEX_4X6)
                .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, PhotoPrintAdapter(context, bitmap, jobName), attributes)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {

            // LEFT: PREVIEW
            Card(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isProcessing) {
                        ProcessingState()
                    } else if (errorMessage != null) {
                        ErrorState(errorMessage!!)
                    } else {
                        finalLayoutPath?.let { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = "Final Result",
                                modifier = Modifier.fillMaxSize().padding(24.dp).clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Select a frame to start", color = Color.Gray)
                    }
                }
            }

            // RIGHT: CONTROLS
            Column(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                Text("FINALIZE", style = MaterialTheme.typography.labelLarge, color = Color.Gray, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Make it Yours!", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Black)

                Spacer(modifier = Modifier.height(24.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]).height(3.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary)
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title, fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (finalFilteredFrames.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().height(130.dp)) {
                        items(finalFilteredFrames) { frame ->
                            FrameSelectionCard(frame, isSelected = selectedFrame?.id == frame.id, onClick = { selectedFrame = frame })
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) { Text("No frames available", color = Color.Gray) }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { handlePrint() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PRINT COPY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { if(finalLayoutPath != null) onFinishClicked(finalLayoutPath!!) },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("FINISH & GET QR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("Upload to cloud & share", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
                        Text("Start Over / Retake", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun FrameSelectionCard(frame: FrameEntity, isSelected: Boolean, onClick: () -> Unit) {
    val borderWidth by animateDpAsState(if (isSelected) 4.dp else 0.dp, label = "border")
    val scale by animateDpAsState(if (isSelected) 110.dp else 100.dp, label = "size")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(scale).clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            border = if (isSelected) BorderStroke(borderWidth, MaterialTheme.colorScheme.primary) else null,
            elevation = CardDefaults.cardElevation(if(isSelected) 8.dp else 2.dp)
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(File(frame.imagePath)), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                if (isSelected) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(4.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = frame.displayName, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, maxLines = 1)
    }
}

@Composable
fun ProcessingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_anim))
        LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(180.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Mixing Pixels...", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ErrorState(msg: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
    }
}