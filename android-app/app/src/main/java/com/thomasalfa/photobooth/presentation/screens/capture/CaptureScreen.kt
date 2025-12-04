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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.IAspectRatio
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.utils.layout.LayoutProcessor
import com.thomasalfa.photobooth.presentation.components.ControlButton
import com.thomasalfa.photobooth.presentation.components.ShutterButton
import com.thomasalfa.photobooth.ui.theme.NeoBlack
import com.thomasalfa.photobooth.ui.theme.NeoCream
import com.thomasalfa.photobooth.ui.theme.NeoGreen
import com.thomasalfa.photobooth.ui.theme.NeoPink
import com.thomasalfa.photobooth.ui.theme.NeoPurple
import com.thomasalfa.photobooth.utils.camera.CameraPreview
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.thomasalfa.photobooth.ui.theme.NeoYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

// --- 1. DEFINISI STATE ---
enum class CameraState {
    NO_DEVICE,      // Tidak ada kabel USB
    DEVICE_FOUND,   // Kabel terdeteksi
    ENGINE_READY,   // CameraClient sudah dibuat
    PREVIEWING,     // Preview sedang jalan
    ERROR           // Terjadi kesalahan fatal
}

data class CameraResolution(val name: String, val width: Int, val height: Int)

@Composable
fun CaptureScreen(
    selectedFrame: FrameEntity,
    modifier: Modifier = Modifier,
    onSessionComplete: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val settingPhotoCount by settingsManager.photoCountFlow.collectAsState(initial = 8)

    // --- SETTINGS ---
    // Note: settingPhotoCount dihapus karena sekarang pakai targetCount dari Frame
    val settingTimer by settingsManager.timerDurationFlow.collectAsState(initial = 3)
    val settingMode by settingsManager.captureModeFlow.collectAsState(initial = "AUTO")
    val settingDelay by settingsManager.autoDelayFlow.collectAsState(initial = 2)

    val targetCount = remember(selectedFrame, settingPhotoCount) {
        val requiredByFrame = LayoutProcessor.getPhotoCountForLayout(selectedFrame.layoutType)
        max(requiredByFrame, settingPhotoCount)
    }

    // --- RESOLUSI ---
    val availableResolutions = listOf(
        CameraResolution("Low (1.2MP)", 1600, 1200),
        CameraResolution("Medium (3MP)", 1920, 1440),
        CameraResolution("High (6MP)", 2880, 2160)
    )
    var selectedResIndex by remember { mutableIntStateOf(1) }
    var showResDialog by remember { mutableStateOf(false) }
    val currentRes = availableResolutions[selectedResIndex]

    // --- SINGLE SOURCE OF TRUTH (State Machine) ---
    var currentState by remember { mutableStateOf(CameraState.NO_DEVICE) }

    // Objek Kamera & Texture
    var cameraClient by remember { mutableStateOf<CameraClient?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

    // Helper UI States
    var hasPermission by remember { mutableStateOf(false) }

    // Session States
    val capturedPhotos = remember { mutableStateListOf<String>() }
    var isSessionRunning by remember { mutableStateOf(false) }
    var isAutoLoopActive by remember { mutableStateOf(false) }

    var countdownValue by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready?") }

    val shutterSound = remember { MediaActionSound() }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    val focusRequester = remember { FocusRequester() }

    // ==========================================================================================
    // 1. PERMISSION & INIT
    // ==========================================================================================
    LaunchedEffect(Unit) {
        delay(1000)
        // Request focus agar keyboard listener jalan (Volume button shutter)
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
    // A. Monitor Fisik USB
    LaunchedEffect(Unit) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        while (isActive) {
            val deviceList = usbManager.deviceList
            val isConnected = deviceList.isNotEmpty()
            if (isConnected) {
                if (currentState == CameraState.NO_DEVICE || currentState == CameraState.ERROR) {
                    currentState = CameraState.DEVICE_FOUND
                }
            } else {
                if (currentState != CameraState.NO_DEVICE) {
                    currentState = CameraState.NO_DEVICE
                }
            }
            delay(1500)
        }
    }

    // B. Reaktor State
    LaunchedEffect(currentState, currentRes, hasPermission, textureViewRef) {
        if (!hasPermission || textureViewRef == null) return@LaunchedEffect
        when (currentState) {
            CameraState.NO_DEVICE -> {
                if (cameraClient != null) {
                    try { cameraClient?.closeCamera() } catch (_: Exception) {}
                    cameraClient = null
                }
            }
            CameraState.DEVICE_FOUND -> {
                if (cameraClient == null) {
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
                    } catch (_: Exception) {
                        currentState = CameraState.ERROR
                    }
                } else {
                    currentState = CameraState.ENGINE_READY
                }
            }
            CameraState.ENGINE_READY -> {
                val client = cameraClient
                if (client != null) {
                    delay(500)
                    try {
                        if (client.isCameraOpened() != true) {
                            client.openCamera(textureViewRef as IAspectRatio)
                        }
                        currentState = CameraState.PREVIEWING
                    } catch (_: Exception) {
                        delay(1000)
                        currentState = CameraState.DEVICE_FOUND
                    }
                }
            }
            CameraState.PREVIEWING -> { }
            CameraState.ERROR -> {
                delay(3000)
                currentState = CameraState.NO_DEVICE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { cameraClient?.closeCamera() } catch (_:Exception) {}
            cameraClient = null
        }
    }

    // ==========================================================================================
    // LOGIC CAPTURE
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

    fun handleShutterClick() {
        scope.launch {
            if (settingMode == "AUTO") {
                // Auto Loop Logic
                if (isAutoLoopActive) return@launch
                isAutoLoopActive = true

                while (capturedPhotos.size < targetCount) {
                    if (capturedPhotos.isNotEmpty()) {
                        statusMessage = "Pose ${capturedPhotos.size + 1}/$targetCount"
                        delay(settingDelay * 1000L)
                    }
                    performSingleCapture()
                }

                delay(800)
                onSessionComplete(capturedPhotos.toList())
                isAutoLoopActive = false

            } else {
                // Manual Logic
                if (isSessionRunning) return@launch
                performSingleCapture()

                if (capturedPhotos.size >= targetCount) {
                    delay(800)
                    onSessionComplete(capturedPhotos.toList())
                } else {
                    statusMessage = "Next Pose!"
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
    // UI LAYOUT
    // ==========================================================================================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP,
                        KeyEvent.KEYCODE_VOLUME_DOWN,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_HEADSETHOOK -> {
                            if (!isSessionRunning && !isAutoLoopActive && capturedPhotos.size < targetCount) {
                                handleShutterClick()
                                return@onKeyEvent true
                            }
                        }
                    }
                }
                false
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // ----------------------------------------------------------------
            // PANEL KIRI: CAMERA PREVIEW (70%) - CLEAN VIEW
            // ----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // 1. Camera Viewfinder
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    cameraClient = cameraClient
                ) { view -> if (view is TextureView) textureViewRef = view }

                // 2. Loading State Overlay
                if (currentState != CameraState.PREVIEWING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeoPink)
                        Spacer(modifier = Modifier.height(16.dp))
                        val msg = when(currentState) {
                            CameraState.NO_DEVICE -> "Hubungkan Kabel Kamera..."
                            CameraState.DEVICE_FOUND -> "Kamera Ditemukan..."
                            CameraState.ENGINE_READY -> "Menyiapkan Lensa..."
                            CameraState.ERROR -> "Gangguan Sinyal..."
                            else -> "Loading..."
                        }
                        Text(msg, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Countdown Raksasa
                if (countdownValue > 0) {
                    val scale by animateFloatAsState(
                        targetValue = if (countdownValue % 2 == 0) 1.2f else 1f,
                        animationSpec = tween(500), label = "scale"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdownValue.toString(),
                            color = NeoYellow,
                            fontSize = 200.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.scale(scale)
                        )
                    }
                }

                // 4. Flash Effect
                if (showFlash) Box(Modifier.fillMaxSize().background(Color.White))
            }

            // ----------------------------------------------------------------
            // PANEL KANAN: DASHBOARD (30%) - INTERACTIVE CONTROL
            // ----------------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NeoCream)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // HEADER STATUS
                Text(
                    text = if(isAutoLoopActive) "AUTO SHOOT" else "PHOTO SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isAutoLoopActive) "POSE NOW!" else statusMessage,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = NeoPurple,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SLOT INDICATOR
                Box(
                    modifier = Modifier
                        .weight(1f) // Mengisi sisa ruang vertikal
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp) // Padding dalam container putih
                ) {

                    val listState = rememberLazyGridState()

                    // 2. EFEK AUTO SCROLL
                    // Setiap kali capturedPhotos bertambah, scroll ke item terakhir (slot kosong berikutnya)
                    LaunchedEffect(capturedPhotos.size) {
                        if (capturedPhotos.size < targetCount) {
                            // Scroll ke index capturedPhotos.size (karena index mulai dari 0)
                            // Contoh: Punya 4 foto (index 0-3), scroll ke index 4 (slot kosong ke-5)
                            listState.animateScrollToItem(capturedPhotos.size)
                        }
                    }
                    LazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(12.dp), // Jarak antar item lebih lega
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(targetCount) { index ->
                            val photoPath = capturedPhotos.getOrNull(index)
                            val isCurrentTarget = index == capturedPhotos.size
                            val isTaken = index < capturedPhotos.size

                            // Container Slot (Row Horizontal)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp) // Sedikit lebih tinggi biar thumbnail jelas
                                    .border(
                                        width = if(isCurrentTarget) 2.dp else 0.dp,
                                        color = if(isCurrentTarget) NeoPink else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8F9FA)), // Background abu sangat muda
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // 1. NOMOR URUT (Kiri)
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .fillMaxHeight()
                                        .background(
                                            when {
                                                isTaken -> NeoGreen // Sudah foto
                                                isCurrentTarget -> NeoPink // Sedang aktif
                                                else -> Color.LightGray // Belum
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                // 2. THUMBNAIL FOTO (4:3 Ratio Fix)
                                // Ini kuncinya: Kita kunci rasionya agar tidak melebar
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(4f/3f) // KUNCI UTAMA: Memaksa rasio 4:3 (Landscape)
                                        .padding(4.dp) // Padding dikit biar ada frame
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoPath != null) {
                                        SubcomposeAsyncImage(
                                            model = File(photoPath),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop, // Crop wajar sesuai rasio 4:3
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // Placeholder Icon kalau belum ada foto
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt, // Pastikan icon ini ada/diganti
                                            contentDescription = null,
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 3. STATUS TEXT (Sisa Ruang Kanan)
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = if (photoPath != null) "Captured" else if (isCurrentTarget) "Shooting..." else "Waiting",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentTarget) NeoPink else NeoBlack
                                    )
                                    if (photoPath == null) {
                                        Text(
                                            text = "4:3 Ratio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FOOTER CONTROLS
                if (!isAutoLoopActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ControlButton(Icons.Default.Refresh, "Reset Cam") {
                            capturedPhotos.clear()
                            try { cameraClient?.closeCamera() } catch (_: Exception) {}
                            cameraClient = null
                            currentState = CameraState.NO_DEVICE
                        }

                        ControlButton(Icons.Default.Settings, "Quality") {
                            showResDialog = true
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ShutterButton(
                        isEnabled = capturedPhotos.size < targetCount,
                        onClick = { handleShutterClick() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        "Please wait...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // DIALOG RESOLUSI
        if (showResDialog) {
            AlertDialog(
                onDismissRequest = { showResDialog = false },
                title = { Text("Camera Quality") },
                text = {
                    Column {
                        availableResolutions.forEachIndexed { index, res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedResIndex = index
                                        showResDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (index == selectedResIndex),
                                    onClick = { selectedResIndex = index; showResDialog = false }
                                )
                                Text(res.name)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showResDialog = false }) { Text("Cancel") } }
            )
        }
    }
}