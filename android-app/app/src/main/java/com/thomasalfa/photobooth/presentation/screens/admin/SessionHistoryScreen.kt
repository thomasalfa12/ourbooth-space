package com.thomasalfa.photobooth.presentation.screens.admin

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.SessionWithPhotos
import com.thomasalfa.photobooth.utils.LocalShareManager
import com.thomasalfa.photobooth.utils.PhotoPrintAdapter
import com.thomasalfa.photobooth.utils.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val rawSessionsList by db.sessionDao().getAllSessions().collectAsState(initial = emptyList())

    var isNewestFirst by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var selectedSession by remember { mutableStateOf<SessionWithPhotos?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    // Sort Logic
    val displayedList = remember(rawSessionsList, isNewestFirst) {
        if (isNewestFirst) rawSessionsList.sortedByDescending { it.session.timestamp }
        else rawSessionsList.sortedBy { it.session.timestamp }
    }

    // --- ACTIONS ---
    fun clearAllData() {
        scope.launch(Dispatchers.IO) {
            val dir = context.externalCacheDir
            dir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("kubik_") || file.name.startsWith("final_") || file.name.startsWith("boom_")) {
                    file.delete()
                }
            }
            db.sessionDao().clearAllData()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "All History Cleared!", Toast.LENGTH_LONG).show()
                showDeleteDialog = false
            }
        }
    }

    fun handlePrint(path: String) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "KubikReprint"
        val attributes = android.print.PrintAttributes.Builder()
            .setMediaSize(android.print.PrintAttributes.MediaSize.NA_INDEX_4X6)
            .setResolution(android.print.PrintAttributes.Resolution("id", "print", 300, 300))
            .setColorMode(android.print.PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, PhotoPrintAdapter(context, bitmap, jobName), attributes)
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Column {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SESSION HISTORY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Text("Total: ${displayedList.size} Sessions", color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isNewestFirst = !isNewestFirst }) {
                        Icon(if (isNewestFirst) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, contentDescription = "Sort")
                    }
                    Text(if(isNewestFirst) "Newest" else "Oldest", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLEAR ALL", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("BACK") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (displayedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No history found.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayedList) { sessionData ->
                        SessionItemCard(data = sessionData, onClick = {
                            selectedSession = sessionData
                            showDetailDialog = true
                        })
                    }
                }
            }
        }

        // DETAIL DIALOG
        if (showDetailDialog && selectedSession != null) {
            SessionDetailDialog(
                sessionData = selectedSession!!,
                onDismiss = { showDetailDialog = false },
                onPrint = { handlePrint(it) }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Delete All History?") },
                text = { Text("Permanently delete all photos from device?") },
                confirmButton = {
                    Button(onClick = { clearAllData() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("YES, DELETE") }
                },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// --- FUNGSI HELPER: SAVE KE GALLERY ---
private fun saveImageToGallery(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) return

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "KUBIK_RAW_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KubikBoothRaw")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it).use { out ->
                FileInputStream(file).copyTo(out!!)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            Toast.makeText(context, "Saved to Tablet Gallery! ✅", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Save Failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- FUNGSI HELPER: PLAY VIDEO EXTERNALLY ---
private fun playVideoExternal(context: Context, videoPath: String) {
    try {
        val file = File(videoPath)
        if (!file.exists()) {
            Toast.makeText(context, "Video file not found!", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot play video: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- COMPONENTS ---

@Composable
fun SessionItemCard(data: SessionWithPhotos, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    val thumbPath = data.session.finalGridPath ?: data.photos.firstOrNull()?.filePath

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (thumbPath != null && File(thumbPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(thumbPath)).build(),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(60.dp).background(Color.LightGray), contentAlignment = Alignment.Center) { Text("?", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Session: ${data.session.sessionUuid.take(8).uppercase()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(dateFormat.format(Date(data.session.timestamp)), color = Color.Gray, fontSize = 12.sp)
            }
            if (data.session.isPrinted) {
                Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text("PRINTED", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(
    sessionData: SessionWithPhotos,
    onDismiss: () -> Unit,
    onPrint: (String) -> Unit
) {
    val session = sessionData.session
    val rawPhotos = sessionData.photos
    val finalPath = session.finalGridPath
    val videoPath = session.gifPath // Ini sekarang isinya Video MP4 Path
    val fileExists = finalPath != null && File(finalPath).exists()
    val videoExists = videoPath != null && File(videoPath).exists()

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isResyncing by remember { mutableStateOf(false) }

    // State Display: 'LAYOUT', 'VIDEO', atau Path Raw Photo
    var selectedMediaType by remember { mutableStateOf("LAYOUT") }
    var selectedRawPhoto by remember { mutableStateOf<String?>(null) } // Jika mode RAW

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(session.sessionUuid) {
        val webLink = "https://ourbooth-space.vercel.app/?id=${session.sessionUuid}"
        qrBitmap = LocalShareManager.generateQrCode(webLink)
    }

    fun handleForceResync() {
        scope.launch {
            isResyncing = true
            withContext(Dispatchers.IO) {
                // 1. Upload VIDEO
                var videoUrl: String? = null
                if (videoExists) {
                    videoUrl = SupabaseManager.uploadFile(File(videoPath!!))
                } else {
                    // Log Error kalau file video lokal hilang
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Local Video File Missing!", Toast.LENGTH_SHORT).show() }
                }

                // 2. Upload Final Photo
                if (fileExists) {
                    val photoUrl = SupabaseManager.uploadFile(File(finalPath!!))

                    if (photoUrl != null) {
                        SupabaseManager.updateFinalSession(session.sessionUuid, photoUrl)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Cloud Data Refreshed! ✅", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Photo Upload Failed ❌", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
            isResyncing = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // --- KIRI: PREVIEW ---
                Column(
                    modifier = Modifier.weight(1.3f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SESSION DETAILS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // MAIN PREVIEW BOX
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedMediaType) {
                            "LAYOUT" -> {
                                if (fileExists) {
                                    AsyncImage(model = File(finalPath!!), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                } else Text("Layout File Missing", color = Color.Gray)
                            }
                            "VIDEO" -> {
                                if (videoExists) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Motion Video Ready", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(File(videoPath!!).name, color = Color.Gray, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(onClick = { playVideoExternal(context, videoPath) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                            Text("PLAY VIDEO")
                                        }
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                        Text("Video File Missing/Corrupt", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            "RAW" -> {
                                if (selectedRawPhoto != null && File(selectedRawPhoto!!).exists()) {
                                    AsyncImage(model = File(selectedRawPhoto!!), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                } else Text("Raw File Missing", color = Color.Gray)
                            }
                        }

                        // Label Overlay
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(4.dp)).padding(horizontal=8.dp, vertical=4.dp)) {
                            Text(text = selectedMediaType, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- GALLERY SELECTOR ---
                    Text("Media Gallery", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    ) {
                        // 1. Tombol Layout
                        item {
                            GalleryThumbnail(
                                isSelected = selectedMediaType == "LAYOUT",
                                label = "Layout",
                                icon = Icons.AutoMirrored.Filled.Send,
                                onClick = { selectedMediaType = "LAYOUT" }
                            )
                        }

                        // 2. Tombol Video
                        item {
                            GalleryThumbnail(
                                isSelected = selectedMediaType == "VIDEO",
                                label = "Motion",
                                icon = Icons.Default.PlayArrow,
                                colorOverride = if(videoExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                onClick = { selectedMediaType = "VIDEO" }
                            )
                        }

                        // 3. Raw Photos
                        items(rawPhotos) { photo ->
                            val isSelected = selectedMediaType == "RAW" && selectedRawPhoto == photo.filePath
                            AsyncImage(
                                model = File(photo.filePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(if(isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMediaType = "RAW"
                                        selectedRawPhoto = photo.filePath
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION BUTTON
                    if (selectedMediaType == "LAYOUT" && fileExists) {
                        Button(onClick = { onPrint(finalPath!!) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Filled.Send, null); Spacer(modifier = Modifier.width(8.dp)); Text("RE-PRINT LAYOUT")
                        }
                    } else if (selectedMediaType == "RAW" && selectedRawPhoto != null) {
                        Button(onClick = { saveImageToGallery(context, selectedRawPhoto!!) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Share, null); Spacer(modifier = Modifier.width(8.dp)); Text("SAVE RAW TO TABLET")
                        }
                    }
                }

                // --- KANAN: QR & CLOUD ---
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray))
                Column(modifier = Modifier.weight(0.7f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("DIGITAL COPY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp).border(4.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)).padding(8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Scan for Video & Photo", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (fileExists) {
                        OutlinedButton(onClick = { handleForceResync() }, enabled = !isResyncing, modifier = Modifier.fillMaxWidth()) {
                            if (isResyncing) Text("Uploading...") else Text("Force Resync Cloud")
                        }
                        Text("Use if QR link is empty", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) { Text("CLOSE", color = Color.Gray) }
                }
            }
        }
    }
}

@Composable
fun GalleryThumbnail(
    isSelected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorOverride: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(if (isSelected) 2.dp else 0.dp, colorOverride, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = colorOverride)
        }
        Text(label, fontSize = 10.sp)
    }
}