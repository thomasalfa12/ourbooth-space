package com.thomasalfa.photobooth.presentation.screens.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.ToneGenerator
import android.util.Log
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.IAspectRatio
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.presentation.components.ControlButton
import com.thomasalfa.photobooth.presentation.components.SessionProgressIndicator
import com.thomasalfa.photobooth.presentation.components.ShutterButton
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.camera.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Data Class Resolusi
data class CameraResolution(val name: String, val width: Int, val height: Int)

@Composable
fun CaptureScreen(
    modifier: Modifier = Modifier,
    onSessionComplete: (List<String>, List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val db = remember { AppDatabase.getDatabase(context) }

    // --- SETTINGS ---
    val settingPhotoCount by settingsManager.photoCountFlow.collectAsState(initial = 6)
    val settingTimer by settingsManager.timerDurationFlow.collectAsState(initial = 3)
    val settingMode by settingsManager.captureModeFlow.collectAsState(initial = "AUTO")
    val settingDelay by settingsManager.autoDelayFlow.collectAsState(initial = 2)

    // --- SESSION ID LOGIC (FIXED) ---
    // Mengambil list secara real-time agar sinkron jika data dihapus/ditambah
    val sessionList by db.sessionDao().getAllSessions().collectAsState(initial = emptyList())
    val sessionNumber = remember(sessionList) { sessionList.size + 1 }

    // --- RESOLUSI ---
    val availableResolutions = listOf(
        CameraResolution("Low (1.2MP)", 1600, 1200),
        CameraResolution("Medium (3MP)", 1920, 1440),
        CameraResolution("High (6MP)", 2880, 2160)
    )
    var selectedResIndex by remember { mutableIntStateOf(1) }
    var showResDialog by remember { mutableStateOf(false) }
    val camWidth = availableResolutions[selectedResIndex].width
    val camHeight = availableResolutions[selectedResIndex].height

    var isAppLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2500)
        isAppLoading = false
    }

    var cameraClient by remember { mutableStateOf<CameraClient?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    val capturedPhotos = remember { mutableStateListOf<String>() }
    val boomerangFrames = remember { mutableStateListOf<String>() }

    var isSessionRunning by remember { mutableStateOf(false) }
    var isAutoLoopActive by remember { mutableStateOf(false) }
    var isRecordingBoomerang by remember { mutableStateOf(false) }

    var countdownValue by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready?") }

    val shutterSound = remember { MediaActionSound() }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // --- LOGIC FOTO ---
    suspend fun performSingleCapture() {
        isSessionRunning = true
        statusMessage = "Get Ready!"
        for (i in settingTimer downTo 1) {
            countdownValue = i
            try { toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150) } catch (e: Exception) {}
            delay(1000)
        }
        countdownValue = 0

        showFlash = true
        try { shutterSound.play(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}
        statusMessage = "Capturing..."

        val currentView = textureViewRef
        if (currentView != null && currentView.isAvailable) {
            val bitmap = currentView.getBitmap(camWidth, camHeight)
            if (bitmap != null) {
                val filename = "kubik_${System.currentTimeMillis()}.jpg"
                val saveFile = File(context.externalCacheDir, filename)
                withContext(Dispatchers.IO) {
                    try {
                        val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
                        val mirrored = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                        FileOutputStream(saveFile).use { out ->
                            mirrored.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            out.flush()
                        }
                        bitmap.recycle()

                        withContext(Dispatchers.Main) {
                            if (saveFile.exists() && saveFile.length() > 0) {
                                capturedPhotos.add(saveFile.absolutePath)
                            }
                            showFlash = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { showFlash = false }
                    }
                }
            } else { showFlash = false }
        } else {
            delay(500)
            showFlash = false
        }
        isSessionRunning = false
    }

    // --- LOGIC BOOMERANG ---
    suspend fun performBoomerangCapture() {
        isRecordingBoomerang = true
        statusMessage = "BOOMERANG!"

        for (i in 3 downTo 1) {
            countdownValue = i
            try { toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150) } catch (e: Exception) {}
            delay(1000)
        }
        countdownValue = 0
        statusMessage = "MOVE NOW!"

        val currentView = textureViewRef
        if (currentView != null && currentView.isAvailable) {
            withContext(Dispatchers.IO) {
                for (i in 1..15) {
                    val smallBitmap = currentView.getBitmap(1280, 960)
                    if (smallBitmap != null) {
                        val filename = "boom_${System.currentTimeMillis()}_$i.jpg"
                        val saveFile = File(context.externalCacheDir, filename)

                        val matrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }
                        val mirrored = Bitmap.createBitmap(smallBitmap, 0, 0, smallBitmap.width, smallBitmap.height, matrix, true)

                        FileOutputStream(saveFile).use { out ->
                            mirrored.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            out.flush()
                        }
                        boomerangFrames.add(saveFile.absolutePath)
                        smallBitmap.recycle()
                    }
                    delay(100)
                }
            }
        }
        statusMessage = "Processing..."
        delay(500)
        isRecordingBoomerang = false
    }

    fun handleShutterClick() {
        scope.launch {
            if (settingMode == "AUTO") {
                if (isAutoLoopActive) return@launch
                isAutoLoopActive = true

                while (capturedPhotos.size < settingPhotoCount) {
                    if (capturedPhotos.size > 0) {
                        statusMessage = "Next Pose..."
                        delay(settingDelay * 1000L)
                    }
                    performSingleCapture()
                }

                delay(1000)
                performBoomerangCapture()

                delay(1000)
                onSessionComplete(capturedPhotos.toList(), boomerangFrames.toList())

                isAutoLoopActive = false
            } else {
                if (isSessionRunning) return@launch
                performSingleCapture()
                if (capturedPhotos.size >= settingPhotoCount) {
                    delay(1000)
                    onSessionComplete(capturedPhotos.toList(), emptyList())
                } else {
                    statusMessage = "Tap for Next!"
                }
            }
        }
    }

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) isCameraReady = true
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            isCameraReady = true
        }
    }

    DisposableEffect(Unit) {
        try { shutterSound.load(MediaActionSound.SHUTTER_CLICK) } catch (e: Exception) {}
        onDispose {
            try { shutterSound.release() } catch (e: Exception) {}
            try { toneGenerator.release() } catch (e: Exception) {}
        }
    }

    DisposableEffect(isCameraReady, camWidth, camHeight) {
        if (isCameraReady) {
            try {
                val client = CameraClient.newBuilder(context)
                    .setEnableGLES(true).setRawImage(false).setCameraStrategy(CameraUvcStrategy(context))
                    .setCameraRequest(
                        CameraRequest.Builder()
                            .setPreviewWidth(camWidth)
                            .setPreviewHeight(camHeight)
                            .setFrontCamera(false)
                            .setContinuousAFModel(true)
                            .create()
                    )
                    .openDebug(true).build()
                cameraClient = client
                if (textureViewRef != null) client.openCamera(textureViewRef as IAspectRatio)
            } catch (e: Exception) {
                Log.e("KUBIKCAM", "Init Error: ${e.message}")
            }
        }
        onDispose { cameraClient?.closeCamera() }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // PANEL KIRI (CAMERA)
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = if (isRecordingBoomerang) 8.dp else 4.dp,
                        color = if (isRecordingBoomerang) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview(modifier = Modifier.fillMaxSize(), cameraClient = cameraClient) { view ->
                    if (view is TextureView) {
                        textureViewRef = view
                        try { cameraClient?.openCamera(view) } catch (e:Exception){}
                    }
                }

                if (countdownValue > 0) {
                    val scale by animateFloatAsState(
                        targetValue = if (countdownValue % 2 == 0) 1.2f else 1f,
                        animationSpec = tween(500), label = "scale"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            countdownValue.toString(),
                            color = MaterialTheme.colorScheme.tertiary, // Kuning
                            fontSize = 180.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.scale(scale)
                        )
                    }
                }

                if (isRecordingBoomerang) {
                    val alpha by rememberInfiniteTransition(label = "rec").animateFloat(
                        initialValue = 1f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "alpha"
                    )
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("● REC", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (showFlash) Box(Modifier.fillMaxSize().background(Color.White))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // PANEL KANAN (CONTROLS)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // SESSION NUMBER UPDATE OTOMATIS
                    val formattedSession = String.format("%03d", sessionNumber)
                    Text("SESSION #$formattedSession", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                    Text("Res: ${availableResolutions[selectedResIndex].name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isRecordingBoomerang) "MOVE NOW!" else if (isAutoLoopActive || isSessionRunning) statusMessage else "Ready?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isRecordingBoomerang) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SessionProgressIndicator(totalShots = settingPhotoCount, currentShot = capturedPhotos.size)
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(capturedPhotos) { photoPath ->
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(photoPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Result",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)),
                                error = {
                                    Box(Modifier.fillMaxSize().background(Color.LightGray))
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (!isAutoLoopActive && capturedPhotos.isEmpty()) {
                        ControlButton(Icons.Default.Settings, "Set", { showResDialog = true })
                        ControlButton(Icons.Default.Refresh, "Clear", { capturedPhotos.clear() })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ShutterButton(
                    isEnabled = !isAutoLoopActive && !isSessionRunning && capturedPhotos.size < settingPhotoCount,
                    onClick = { handleShutterClick() }
                )
            }
        }

        AnimatedVisibility(visible = isAppLoading, exit = fadeOut(animationSpec = tween(500))) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CuteLoadingAnimation()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Warming Up Camera...", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showResDialog) {
            AlertDialog(
                onDismissRequest = { showResDialog = false },
                title = { Text("Select Resolution", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        availableResolutions.forEachIndexed { index, res ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedResIndex = index; showResDialog = false }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (index == selectedResIndex), onClick = { selectedResIndex = index; showResDialog = false })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(res.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text("${res.width} x ${res.height}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showResDialog = false }) { Text("Cancel") } },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun CuteLoadingAnimation() {
    val transition = rememberInfiniteTransition(label = "loading")
    // Gunakan warna tema untuk dots loading
    val dots = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        dots.forEachIndexed { index, color ->
            val scale by transition.animateFloat(initialValue = 0.5f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = index * 150, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "dot")
            Box(modifier = Modifier.size(30.dp).scale(scale).clip(CircleShape).background(color))
        }
    }
}