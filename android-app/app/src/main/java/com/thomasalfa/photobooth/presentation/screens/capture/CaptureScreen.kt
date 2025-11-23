package com.thomasalfa.photobooth.presentation.screens.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.usb.UsbManager
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.ToneGenerator
import android.util.Log
import android.view.KeyEvent
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
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
import com.thomasalfa.photobooth.utils.camera.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// --- 1. DEFINISI STATE ---
enum class CameraState {
    NO_DEVICE,      // Tidak ada kabel USB
    DEVICE_FOUND,   // Kabel terdeteksi, siap inisialisasi
    ENGINE_READY,   // CameraClient sudah dibuat
    PREVIEWING,     // Preview sedang jalan
    ERROR           // Terjadi kesalahan fatal
}

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

    // Resolusi saat ini
    val currentRes = availableResolutions[selectedResIndex]

    // --- SINGLE SOURCE OF TRUTH (State Machine) ---
    // State ini akan kita mainkan saat tombol Clear ditekan
    var currentState by remember { mutableStateOf(CameraState.NO_DEVICE) }

    // Objek Kamera & Texture
    var cameraClient by remember { mutableStateOf<CameraClient?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    // Helper UI States
    var isAppLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }

    // Session States
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

    // Focus Requester untuk tombol fisik
    val focusRequester = remember { FocusRequester() }

    // ==========================================================================================
    // 1. PERMISSION & LOADING INIT
    // ==========================================================================================
    LaunchedEffect(Unit) {
        delay(1000)
        isAppLoading = false
        focusRequester.requestFocus()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) hasPermission = true
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            hasPermission = true
        }
    }

    // Restart Engine jika Resolusi Berubah
    LaunchedEffect(currentRes) {
        if (currentState == CameraState.PREVIEWING || currentState == CameraState.ENGINE_READY) {
            Log.d("KUBIKCAM", "Resolution Changed. Restarting Engine...")
            try { cameraClient?.closeCamera() } catch(_:Exception){}
            cameraClient = null
            currentState = CameraState.DEVICE_FOUND
        }
    }

    // ==========================================================================================
    // 2. STATE MACHINE DRIVER (JANTUNG APLIKASI)
    // ==========================================================================================

    // A. Monitor Fisik USB -> Mengubah State Dasar
    LaunchedEffect(Unit) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        while (isActive) {
            val deviceList = usbManager.deviceList
            val isConnected = deviceList.isNotEmpty()

            if (isConnected) {
                // Jika baru colok, atau sebelumnya error -> Masuk ke DEVICE_FOUND
                if (currentState == CameraState.NO_DEVICE || currentState == CameraState.ERROR) {
                    currentState = CameraState.DEVICE_FOUND
                    Log.d("KUBIKCAM", "State -> DEVICE_FOUND")
                }
            } else {
                // Jika cabut -> Reset ke NO_DEVICE
                if (currentState != CameraState.NO_DEVICE) {
                    currentState = CameraState.NO_DEVICE
                    Log.d("KUBIKCAM", "State -> NO_DEVICE")
                }
            }
            delay(1500) // Polling interval
        }
    }

    // B. Reaktor State -> Menangani Logika Kamera
    LaunchedEffect(currentState, currentRes, hasPermission, textureViewRef) {
        if (!hasPermission || textureViewRef == null) return@LaunchedEffect

        when (currentState) {
            CameraState.NO_DEVICE -> {
                // CLEANUP TOTAL
                if (cameraClient != null) {
                    try { cameraClient?.closeCamera() } catch (_: Exception) {}
                    cameraClient = null
                    Log.d("KUBIKCAM", "Cleanup Complete")
                }
            }

            CameraState.DEVICE_FOUND -> {
                // BUILD ENGINE
                if (cameraClient == null) {
                    Log.d("KUBIKCAM", "Building Engine...")
                    delay(800)
                    try {
                        val client = CameraClient.newBuilder(context)
                            .setEnableGLES(true)
                            .setRawImage(false)
                            .setCameraStrategy(CameraUvcStrategy(context))
                            .setCameraRequest(
                                CameraRequest.Builder()
                                    .setPreviewWidth(currentRes.width)
                                    .setPreviewHeight(currentRes.height)
                                    .setFrontCamera(false)
                                    .setContinuousAFModel(true)
                                    .create()
                            )
                            .openDebug(true)
                            .build()

                        cameraClient = client
                        currentState = CameraState.ENGINE_READY
                        Log.d("KUBIKCAM", "State -> ENGINE_READY")
                    } catch (e: Exception) {
                        Log.e("KUBIKCAM", "Build Error: ${e.message}")
                        currentState = CameraState.ERROR
                    }
                } else {
                    currentState = CameraState.ENGINE_READY
                }
            }

            CameraState.ENGINE_READY -> {
                // START PREVIEW
                val client = cameraClient
                if (client != null) {
                    delay(500)
                    try {
                        if (client.isCameraOpened() != true) {
                            Log.d("KUBIKCAM", "Opening Camera...")
                            client.openCamera(textureViewRef as IAspectRatio)
                        }
                        currentState = CameraState.PREVIEWING
                        Log.d("KUBIKCAM", "State -> PREVIEWING")
                    } catch (e: Exception) {
                        Log.e("KUBIKCAM", "Open Error: ${e.message}")
                        delay(1000)
                        currentState = CameraState.DEVICE_FOUND
                    }
                }
            }

            CameraState.PREVIEWING -> { /* Monitor stabil */ }

            CameraState.ERROR -> {
                delay(3000)
                currentState = CameraState.NO_DEVICE
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            try { cameraClient?.closeCamera() } catch (_:Exception) {}
            cameraClient = null
        }
    }

    // ==========================================================================================
    // LOGIC CAPTURE & BOOMERANG
    // ==========================================================================================

    suspend fun performSingleCapture() {
        isSessionRunning = true
        statusMessage = "Get Ready!"
        for (i in settingTimer downTo 1) {
            countdownValue = i
            try { toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150) } catch (_: Exception) {}
            delay(1000)
        }
        countdownValue = 0

        showFlash = true
        try { shutterSound.play(MediaActionSound.SHUTTER_CLICK) } catch (_: Exception) {}
        statusMessage = "Capturing..."

        val currentView = textureViewRef
        if (currentView != null && currentView.isAvailable && currentState == CameraState.PREVIEWING) {
            try {
                val bitmap = currentView.getBitmap(currentRes.width, currentRes.height)
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
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) { showFlash = false }
                        }
                    }
                } else { showFlash = false }
            } catch (_: Exception) { showFlash = false }
        } else {
            delay(500)
            showFlash = false
        }
        isSessionRunning = false
    }

    suspend fun performBoomerangCapture() {
        isRecordingBoomerang = true
        statusMessage = "BOOMERANG!"

        for (i in 3 downTo 1) {
            countdownValue = i
            try { toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150) } catch (_: Exception) {}
            delay(1000)
        }
        countdownValue = 0
        statusMessage = "MOVE NOW!"

        val currentView = textureViewRef
        if (currentView != null && currentView.isAvailable && currentState == CameraState.PREVIEWING) {
            withContext(Dispatchers.IO) {
                for (i in 1..15) {
                    try {
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
                            withContext(Dispatchers.Main) {
                                boomerangFrames.add(saveFile.absolutePath)
                            }
                            smallBitmap.recycle()
                        }
                    } catch (_: Exception) {}
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
                    if (capturedPhotos.isNotEmpty()) {
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

    DisposableEffect(Unit) {
        try { shutterSound.load(MediaActionSound.SHUTTER_CLICK) } catch (_: Exception) {}
        onDispose {
            try { shutterSound.release() } catch (_: Exception) {}
            try { toneGenerator.release() } catch (_: Exception) {}
        }
    }

    // ==========================================================================================
    // UI LAYOUT + KEY EVENT LISTENER
    // ==========================================================================================
    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.nativeKeyEvent.keyCode) {
                    // [VOLUME SHUTTER / REMOTE TRIGGER]
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_HEADSETHOOK -> {
                        if (!isSessionRunning && !isAutoLoopActive && capturedPhotos.size < settingPhotoCount) {
                            handleShutterClick()
                            return@onKeyEvent true
                        }
                    }
                }
            }
            false
        }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    cameraClient = cameraClient
                ) { view ->
                    if (view is TextureView) textureViewRef = view
                }

                // UI Feedback Berdasarkan State
                if (currentState != CameraState.PREVIEWING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))

                        val msg = when(currentState) {
                            CameraState.NO_DEVICE -> "Cek Kabel Kamera USB..." // Bisa juga muncul saat sedang Reset
                            CameraState.DEVICE_FOUND -> "Kamera Ditemukan..."
                            CameraState.ENGINE_READY -> "Menyiapkan Preview..."
                            CameraState.ERROR -> "Error, mencoba ulang..."
                            else -> "Loading..."
                        }
                        Text(msg, color = Color.White, fontWeight = FontWeight.Bold)
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
                            color = MaterialTheme.colorScheme.tertiary,
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
                    val formattedSession = String.format(Locale.US, "%03d", sessionNumber)
                    Text("SESSION #$formattedSession", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("Res: ${currentRes.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                                model = ImageRequest.Builder(context).data(File(photoPath)).crossfade(true).build(),
                                contentDescription = "Result",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)),
                                error = { Box(Modifier.fillMaxSize().background(Color.LightGray)) }
                            )
                        }
                    }
                }

                // [MODIFIKASI DI SINI: TOMBOL SMART RESET]
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (!isAutoLoopActive && capturedPhotos.isEmpty()) {
                        ControlButton(Icons.Default.Settings, "Set", { showResDialog = true })

                        // Tombol "Clear" sekarang jadi "Reset"
                        ControlButton(Icons.Default.Refresh, "Reset", {
                            // 1. Bersihkan data foto
                            capturedPhotos.clear()
                            boomerangFrames.clear()

                            // 2. SMART RESET: Matikan mesin secara paksa
                            Log.d("KUBIKCAM", "Smart Reset Triggered by User")
                            try { cameraClient?.closeCamera() } catch (_: Exception) {}
                            cameraClient = null

                            // 3. Ubah state ke NO_DEVICE
                            // Ini akan memancing Loop "Monitor Fisik USB" (baris 196) untuk mendeteksi ulang
                            // dan membangun mesin kamera dari awal (seolah-olah restart app).
                            currentState = CameraState.NO_DEVICE
                        })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ShutterButton(
                    isEnabled = !isAutoLoopActive && !isSessionRunning && capturedPhotos.size < settingPhotoCount,
                    onClick = { handleShutterClick() }
                )
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
    val dots = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        dots.forEachIndexed { index, color ->
            val scale by transition.animateFloat(initialValue = 0.5f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = index * 150, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "dot")
            Box(modifier = Modifier.size(30.dp).scale(scale).clip(CircleShape).background(color))
        }
    }
}