package com.thomasalfa.photobooth.presentation.screens.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintManager
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
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.SessionWithPhotos
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.LocalShareManager
import com.thomasalfa.photobooth.utils.PhotoPrintAdapter
import com.thomasalfa.photobooth.utils.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
                if (file.name.startsWith("kubik_") || file.name.startsWith("final_")) {
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
                    // Tombol Sort
                    IconButton(onClick = { isNewestFirst = !isNewestFirst }) {
                        Icon(if (isNewestFirst) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, contentDescription = "Sort")
                    }
                    Text(if(isNewestFirst) "Newest" else "Oldest", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(16.dp))

                    // Tombol Delete All
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

        // DETAIL DIALOG (QR MUNCUL OTOMATIS DI SINI)
        if (showDetailDialog && selectedSession != null) {
            SessionDetailDialog(
                sessionData = selectedSession!!,
                onDismiss = { showDetailDialog = false },
                onPrint = { handlePrint(it) }
            )
        }

        // Delete Confirmation
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
    val finalPath = session.finalGridPath
    val fileExists = finalPath != null && File(finalPath).exists()

    // STATE QR CODE (Langsung generate saat dialog dibuka)
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isResyncing by remember { mutableStateOf(false) } // Untuk loading re-upload
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(session.sessionUuid) {
        // Generate QR Instan dari UUID
        val webLink = "https://ourbooth-space.vercel.app/?id=${session.sessionUuid}"
        qrBitmap = LocalShareManager.generateQrCode(webLink)
    }

    // Fungsi Re-Upload (Hanya dijalankan jika tombol ditekan)
    fun handleForceResync() {
        scope.launch {
            isResyncing = true
            val gifPath = session.gifPath

            withContext(Dispatchers.IO) {
                // 1. Upload GIF (Jika ada)
                var gifUrl: String? = null
                if (gifPath != null && File(gifPath).exists()) {
                    gifUrl = SupabaseManager.uploadFile(File(gifPath))
                }

                // 2. Upload Final Photo (Wajib)
                if (fileExists) {
                    val photoUrl = SupabaseManager.uploadFile(File(finalPath!!))

                    if (photoUrl != null) {
                        // 3. Insert Awal (Cek dulu)
                        SupabaseManager.insertInitialSession(session.sessionUuid, gifUrl)
                        // 4. Update Final
                        SupabaseManager.updateFinalSession(session.sessionUuid, photoUrl)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Cloud Data Refreshed! ✅", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Upload Failed ❌", Toast.LENGTH_SHORT).show() }
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
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f) // Hampir full screen
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // --- BAGIAN KIRI: PREVIEW & FOTO ---
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SESSION DETAILS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Preview Besar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fileExists) {
                            AsyncImage(
                                model = File(finalPath!!),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("File Missing", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Print
                    if (fileExists) {
                        Button(
                            onClick = { onPrint(finalPath!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RE-PRINT PHOTO")
                        }
                    }
                }

                // --- BAGIAN KANAN: QR & CLOUD ---
                // Garis Pemisah
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray))

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("DIGITAL COPY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    // QR CODE IMAGE (Generated Locally)
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .border(4.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Scan to download", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        CircularProgressIndicator()
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // TOMBOL FORCE RE-UPLOAD (Secondary)
                    if (fileExists) {
                        OutlinedButton(
                            onClick = { handleForceResync() },
                            enabled = !isResyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isResyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Force Re-Sync Cloud")
                            }
                        }
                        Text(
                            "Only use if link is broken/empty",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("CLOSE", color = Color.Gray)
                    }
                }
            }
        }
    }
}