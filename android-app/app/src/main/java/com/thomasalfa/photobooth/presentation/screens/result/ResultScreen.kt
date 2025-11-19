package com.thomasalfa.photobooth.presentation.screens.result

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send // Fix Deprecated
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.print.PrintHelper
import coil.compose.rememberAsyncImagePainter
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.LayoutProcessor
import com.thomasalfa.photobooth.utils.LocalShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultScreen(
    photoPaths: List<String>,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- DATABASE ---
    // Ambil list frame yang tersedia dari database
    val db = remember { AppDatabase.getDatabase(context) }
    val availableFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    // --- STATE ---
    // Default pilih frame pertama jika ada
    var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) }

    // Jika data frame sudah load dan belum ada yang dipilih, set default ke index 0
    LaunchedEffect(availableFrames) {
        if (availableFrames.isNotEmpty() && selectedFrame == null) {
            selectedFrame = availableFrames[0]
        }
    }

    var finalLayoutPath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) } // Ubah default jadi false dulu

    // QR State
    var showQrDialog by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // --- LOGIC PROCESSOR ---
    // Jalankan setiap kali `selectedFrame` atau `photoPaths` berubah
    LaunchedEffect(photoPaths, selectedFrame) {
        if (selectedFrame != null) {
            isProcessing = true
            withContext(Dispatchers.Default) {
                try {
                    val photoBitmaps = photoPaths.map { BitmapFactory.decodeFile(it) }
                    val frameBitmap = BitmapFactory.decodeFile(selectedFrame!!.imagePath)

                    // Gunakan layout type dari Frame Entity ("GRID" atau "STRIP")
                    val resultBitmap = LayoutProcessor.processLayout(
                        photos = photoBitmaps,
                        layoutType = selectedFrame!!.layoutType,
                        frameBitmap = frameBitmap
                    )

                    val file = File(context.cacheDir, "kubik_final_${System.currentTimeMillis()}.jpg")
                    val stream = FileOutputStream(file)
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    stream.flush()
                    stream.close()

                    finalLayoutPath = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isProcessing = false
        }
    }

    DisposableEffect(Unit) { onDispose { LocalShareManager.stopServer() } }

    // --- ACTIONS ---
    fun printPhoto() {
        finalLayoutPath?.let { path ->
            val printHelper = PrintHelper(context)
            printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
            printHelper.printBitmap("KubikCam_Print", BitmapFactory.decodeFile(path))
        }
    }

    fun startSharing() {
        finalLayoutPath?.let { path ->
            val url = LocalShareManager.startServer(path)
            if (url != null) {
                scope.launch {
                    qrBitmap = LocalShareManager.generateQrCode(url)
                    showQrDialog = true
                }
            }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // LEFT PANEL: PREVIEW
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 24.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(4.dp, NeoBlack, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = NeoPurple)
                } else {
                    finalLayoutPath?.let { path ->
                        Image(
                            painter = rememberAsyncImagePainter(File(path)),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Text("Select a frame to see preview")
                }
            }

            // RIGHT PANEL: CONTROLS & FRAME SELECTOR
            Column(
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column {
                    Text("CUSTOMIZE", style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Pick Your Frame", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }

                // FRAME SELECTOR (Horizontal List)
                if (availableFrames.isEmpty()) {
                    Text("No frames available via Admin", color = Color.Red)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        items(availableFrames) { frame ->
                            val isSelected = selectedFrame?.id == frame.id

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedFrame = frame }
                            ) {
                                // Thumbnail Frame
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(if(isSelected) 4.dp else 1.dp, if(isSelected) NeoPurple else Color.Gray, RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(frame.imagePath)),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = frame.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if(isSelected) NeoPurple else Color.Black
                                )
                            }
                        }
                    }
                }

                // ACTION BUTTONS
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { startSharing() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBlack),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GET QR CODE", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { printPhoto() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBlack),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = NeoBlack) // FIXED ICON
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PRINT", fontWeight = FontWeight.Bold, color = NeoBlack)
                    }

                    TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
                        Text("Start Over", color = Color.Gray)
                    }
                }
            }
        }

        // QR DIALOG (Sama seperti sebelumnya)
        if (showQrDialog && qrBitmap != null) {
            Dialog(onDismissRequest = { showQrDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.border(2.dp, NeoBlack, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showQrDialog = false }) { Text("CLOSE") }
                    }
                }
            }
        }
    }
}