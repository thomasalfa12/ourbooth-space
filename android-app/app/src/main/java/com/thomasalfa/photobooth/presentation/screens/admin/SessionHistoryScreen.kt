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

    // State QR Dialog
    var showQrDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }

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
                Toast.makeText(context, "Storage Cleared!", Toast.LENGTH_LONG).show()
                showDeleteDialog = false
            }
        }
    }

    fun handlePrint(path: String) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "KubikReprint"

        // PERBAIKAN DISINI: Menggunakan Konstanta yang benar
        val attributes = android.print.PrintAttributes.Builder()
            .setMediaSize(android.print.PrintAttributes.MediaSize.NA_INDEX_4X6)
            .setResolution(android.print.PrintAttributes.Resolution("id", "print", 300, 300))
            .setColorMode(android.print.PrintAttributes.COLOR_MODE_COLOR) // FIX ERROR
            .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, PhotoPrintAdapter(context, bitmap, jobName), attributes)
    }

    // GENERATE QR (UPLOAD KE CLOUD)
    fun handleGenerateQr(session: SessionWithPhotos) {
        val finalPath = session.session.finalGridPath ?: return
        val gifPath = session.session.gifPath
        val uuid = session.session.sessionUuid

        scope.launch {
            isUploading = true

            // 1. Upload Ulang File (Jika file masih ada di lokal)
            val photoUrl = withContext(Dispatchers.IO) {
                if (File(finalPath).exists()) SupabaseManager.uploadFile(File(finalPath)) else null
            }

            var gifUrl: String? = null
            if (gifPath != null && File(gifPath).exists()) {
                withContext(Dispatchers.IO) {
                    gifUrl = SupabaseManager.uploadFile(File(gifPath))
                }
            }

            // 2. Update/Insert Database Cloud
            if (photoUrl != null) {
                SupabaseManager.insertSession(uuid, photoUrl, gifUrl)

                // 3. Generate Link Website
                val webLink = "https://kubik-gallery.vercel.app/?id=$uuid"

                // 4. Generate QR Image
                val bmp = LocalShareManager.generateQrCode(webLink)

                qrBitmap = bmp
                isUploading = false
                showQrDialog = true
            } else {
                isUploading = false
                Toast.makeText(context, "Upload Gagal (File Hilang/No Internet)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp)) {
        Column {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SESSION HISTORY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Total: ${displayedList.size} Sessions", color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isNewestFirst = !isNewestFirst }) { Icon(if (isNewestFirst) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, contentDescription = "Sort", tint = NeoBlack) }
                    Text(if(isNewestFirst) "Newest" else "Oldest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { showDeleteDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("CLEAR ALL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("BACK") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (displayedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("No history found.", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayedList) { sessionData ->
                        SessionItemCard(data = sessionData, onClick = { selectedSession = sessionData; showDetailDialog = true })
                    }
                }
            }
        }

        // DETAIL DIALOG
        if (showDetailDialog && selectedSession != null) {
            val session = selectedSession!!.session
            val photos = selectedSession!!.photos
            val finalPath = session.finalGridPath
            val fileExists = finalPath != null && File(finalPath).exists()

            Dialog(onDismissRequest = { showDetailDialog = false }) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Session Detail", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showDetailDialog = false }) { Text("X", fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Preview Final
                        Text("Final Result", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.LightGray, RoundedCornerShape(8.dp)).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            if (fileExists) {
                                Image(painter = rememberAsyncImagePainter(File(finalPath!!)), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            } else {
                                Text("File Not Found", color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Raw Photos List
                        Text("Raw Photos (${photos.size})", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            items(photos) { photo ->
                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(File(photo.filePath)).build(), contentDescription = null, modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            if (fileExists) {
                                // Tombol QR Upload Ulang
                                Button(
                                    onClick = { handleGenerateQr(selectedSession!!) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                    enabled = !isUploading
                                ) {
                                    if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    else Icon(Icons.Default.Share, null, tint = Color.White)

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isUploading) "UPLOADING..." else "RE-UPLOAD QR")
                                }

                                Button(onClick = { handlePrint(finalPath!!) }, colors = ButtonDefaults.buttonColors(containerColor = NeoYellow)) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = NeoBlack)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("RE-PRINT", color = NeoBlack)
                                }
                            }
                        }
                    }
                }
            }
        }

        // QR DIALOG
        if (showQrDialog && qrBitmap != null) {
            Dialog(onDismissRequest = { showQrDialog = false }) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ONLINE QR CODE", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NeoPurple)
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(bitmap = qrBitmap!!.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(250.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showQrDialog = false }) { Text("CLOSE") }
                    }
                }
            }
        }

        // Delete Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
                title = { Text("Delete All History?") },
                text = { Text("Permanently delete all photos?") },
                confirmButton = { Button(onClick = { clearAllData() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("YES") } },
                dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun SessionItemCard(data: SessionWithPhotos, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    val thumbPath = data.session.finalGridPath ?: data.photos.firstOrNull()?.filePath

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (thumbPath != null && File(thumbPath).exists()) {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(File(thumbPath)).build(), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(60.dp).background(Color.LightGray), contentAlignment = Alignment.Center) { Text("?", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Session: ${data.session.sessionUuid.take(8).uppercase()}", fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(data.session.timestamp)), color = Color.Gray, fontSize = 12.sp)
            }
            if (data.session.isPrinted) Surface(color = NeoGreen.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) { Text("PRINTED", fontSize = 10.sp, color = Color.Black, modifier = Modifier.padding(4.dp)) }
        }
    }
}