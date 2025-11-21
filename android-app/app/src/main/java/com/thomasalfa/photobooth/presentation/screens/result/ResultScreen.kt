package com.thomasalfa.photobooth.presentation.screens.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.GifProcessor
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
    boomerangPaths: List<String>,
    onRetake: () -> Unit,
    // CALLBACK BARU: Pindah ke layar Upload (Pesawat)
    onFinishClicked: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    // --- STATE TABS ---
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("ALL", "GRID (2x3)", "STRIP (Cut 2)")
    val filteredFrames = remember(allFrames, selectedTabIndex) {
        when (selectedTabIndex) {
            1 -> allFrames.filter { it.layoutType == "GRID" }
            2 -> allFrames.filter { it.layoutType == "STRIP" }
            else -> allFrames
        }
    }

    // --- STATE UTAMA ---
    var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) }
    var finalLayoutPath by remember { mutableStateOf<String?>(null) }
    var finalGifPath by remember { mutableStateOf<String?>(null) } // GIF Path

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto Select Frame Pertama
    LaunchedEffect(filteredFrames) {
        if (filteredFrames.isNotEmpty() && selectedFrame == null) {
            selectedFrame = filteredFrames[0]
        }
    }

    // --- PROCESSOR ENGINE ---
    LaunchedEffect(photoPaths, selectedFrame, boomerangPaths) {
        if (selectedFrame != null && photoPaths.isNotEmpty()) {
            isProcessing = true
            errorMessage = null
            delay(500)

            withContext(Dispatchers.Default) {
                try {
                    // 1. PROSES GRID FOTO
                    // Gunakan inSampleSize = 2 agar RAM aman (Anti-Crash)
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val photoBitmaps = photoPaths.mapNotNull { path ->
                        BitmapFactory.decodeFile(path, options)
                    }

                    if (photoBitmaps.isEmpty()) throw Exception("Foto corrupt/hilang")

                    val frameBitmap = BitmapFactory.decodeFile(selectedFrame!!.imagePath)
                        ?: throw Exception("Frame corrupt")

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

                    finalLayoutPath = file.absolutePath

                    // 2. PROSES GIF BOOMERANG
                    if (boomerangPaths.isNotEmpty()) {
                        val gifResult = GifProcessor.generateBoomerangGif(context, boomerangPaths)
                        finalGifPath = gifResult
                    }

                } catch (t: Throwable) {
                    t.printStackTrace()
                    errorMessage = "Error: ${t.message}"
                }
            }
            isProcessing = false
        }
    }

    // --- ACTIONS ---
    fun handlePrint() {
        if (finalLayoutPath != null) {
            val bitmap = BitmapFactory.decodeFile(finalLayoutPath)
            if (bitmap != null) {
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
    }

    fun handleFinish() {
        if (finalLayoutPath != null) {
            // Panggil Callback ke MainActivity untuk pindah ke Upload Screen
            onFinishClicked(finalLayoutPath!!, finalGifPath)
        }
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxSize()) {

            // PANEL KIRI: PREVIEW
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading_anim))
                        if (composition != null) {
                            LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(150.dp))
                        } else {
                            CircularProgressIndicator(color = NeoPurple)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Creating Magic...", color = NeoPurple, fontWeight = FontWeight.Bold)
                    }
                } else if (errorMessage != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                    }
                } else {
                    // FOTO UTAMA
                    finalLayoutPath?.let { path ->
                        key(path) {
                            Image(
                                painter = rememberAsyncImagePainter(File(path)),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } ?: Text("Select Frame", color = Color.Gray)

                    // GIF OVERLAY
                    if (finalGifPath != null) {
                        val imageLoader = ImageLoader.Builder(context)
                            .components {
                                if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                            }
                            .build()

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp)
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(3.dp, NeoPink, RoundedCornerShape(12.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(File(finalGifPath!!)).build(),
                                imageLoader = imageLoader,
                                contentDescription = "Boomerang",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.align(Alignment.TopStart).background(NeoPink, RoundedCornerShape(bottomEnd=8.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // PANEL KANAN: CONTROLS
            Column(
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("FINALIZE", style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Choose Frame", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(16.dp))

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = NeoPurple,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = NeoPurple)
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) })
                        }
                    }
                }

                if (filteredFrames.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        items(filteredFrames) { frame ->
                            val isSelected = selectedFrame?.id == frame.id
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedFrame = frame }) {
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).border(if(isSelected) 4.dp else 1.dp, if(isSelected) NeoPurple else Color.Gray, RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                                    Image(painter = rememberAsyncImagePainter(File(frame.imagePath)), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(frame.displayName, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No frames", color = Color.Gray) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { handlePrint() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBlack),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = NeoBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PRINT PHOTO", fontWeight = FontWeight.Bold, color = NeoBlack)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // TOMBOL FINISH & GET QR (Pemicu Pindah Layar)
                    Button(
                        onClick = { handleFinish() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoGreen),
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(6.dp),
                        enabled = finalLayoutPath != null && !isProcessing
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FINISH & GET QR", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Upload to cloud & share", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    TextButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) { Text("No, Retake Photo", color = Color.Red) }
                }
            }
        }
    }
}