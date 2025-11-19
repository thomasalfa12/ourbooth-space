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
import androidx.compose.animation.core.animateFloatAsState // Animasi Timer
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale // Untuk animasi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow // Bayangan Teks
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.widget.IAspectRatio
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.presentation.components.ControlButton
import com.thomasalfa.photobooth.presentation.components.SessionProgressIndicator
import com.thomasalfa.photobooth.presentation.components.ShutterButton
import com.thomasalfa.photobooth.ui.theme.* // Import Theme Neo (NeoCream, NeoBlack, etc)
import com.thomasalfa.photobooth.utils.camera.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun CaptureScreen(
    modifier: Modifier = Modifier,
    onSessionComplete: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // --- SETTINGS ---
    val settingPhotoCount by settingsManager.photoCountFlow.collectAsState(initial = 6)
    val settingTimer by settingsManager.timerDurationFlow.collectAsState(initial = 3)
    val settingMode by settingsManager.captureModeFlow.collectAsState(initial = "AUTO")
    val settingDelay by settingsManager.autoDelayFlow.collectAsState(initial = 2)

    val camWidth = 2592
    val camHeight = 1944

    // State
    var cameraClient by remember { mutableStateOf<CameraClient?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    val capturedPhotos = remember { mutableStateListOf<String>() }

    // Logic State
    var isSessionRunning by remember { mutableStateOf(false) }
    var isAutoLoopActive by remember { mutableStateOf(false) }

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
                        FileOutputStream(saveFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }
                        withContext(Dispatchers.Main) {
                            capturedPhotos.add(saveFile.absolutePath)
                        }
                    } catch (e: Exception) {}
                }
            }
        } else {
            delay(500)
        }

        showFlash = false
        isSessionRunning = false
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

                isAutoLoopActive = false
                delay(1000)
                onSessionComplete(capturedPhotos.toList())

            } else {
                if (isSessionRunning) return@launch

                performSingleCapture()

                if (capturedPhotos.size >= settingPhotoCount) {
                    delay(1000)
                    onSessionComplete(capturedPhotos.toList())
                } else {
                    statusMessage = "Tap for Next!"
                }
            }
        }
    }

    // Permissions & Init Code (Boilerplate)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && audioGranted) isCameraReady = true
    }

    LaunchedEffect(Unit) {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
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

    DisposableEffect(isCameraReady) {
        if (isCameraReady) {
            try {
                val client = CameraClient.newBuilder(context)
                    .setEnableGLES(true)
                    .setRawImage(false)
                    .setCameraStrategy(CameraUvcStrategy(context))
                    .setCameraRequest(
                        CameraRequest.Builder()
                            .setPreviewWidth(camWidth)
                            .setPreviewHeight(camHeight)
                            .setFrontCamera(false)
                            .setContinuousAFModel(true)
                            .create()
                    )
                    .openDebug(true)
                    .build()
                cameraClient = client
                if (textureViewRef != null) client.openCamera(textureViewRef as IAspectRatio)
            } catch (e: Exception) {
                Log.e("KUBIKCAM", "Init Error: ${e.message}")
            }
        }
        onDispose { cameraClient?.closeCamera() }
    }

    // --- UI BARU (VISUAL FIX) ---
    Row(modifier = modifier
        .fillMaxSize()
        .background(NeoCream) // 1. Background Utama CREAM
        .padding(16.dp)
    ) {
        // PANEL KIRI (CAMERA)
        Box(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(4.dp, NeoBlack, RoundedCornerShape(24.dp)) // 2. Border Hitam Tebal
                .background(Color.Black)
        ) {
            CameraPreview(modifier = Modifier.fillMaxSize(), cameraClient = cameraClient) { view ->
                if (view is TextureView) {
                    textureViewRef = view
                    try { cameraClient?.openCamera(view) } catch (e:Exception){}
                }
            }

            // 3. TIMER OVERLAY (POP STYLE)
            if (countdownValue > 0) {
                // Animasi Denyut Jantung
                val scale by animateFloatAsState(
                    targetValue = if (countdownValue % 2 == 0) 1.2f else 1f,
                    animationSpec = tween(500), label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownValue.toString(),
                        // Warna Kuning Terang + Font Tebal + Shadow Hitam
                        color = NeoYellow,
                        fontSize = 180.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.scale(scale),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(5f, 5f),
                                blurRadius = 10f
                            )
                        )
                    )
                }
            }
            if (showFlash) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // PANEL KANAN (CONTROLS)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White) // 4. Panel Kanan PUTIH Bersih
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SESSION #A01", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                // Status Teks
                Text(
                    text = if (isAutoLoopActive || isSessionRunning) statusMessage else "Ready?",
                    style = MaterialTheme.typography.headlineMedium,
                    // 5. Warna Teks HITAM (NeoBlack) agar terbaca di background putih
                    color = NeoBlack,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                SessionProgressIndicator(totalShots = settingPhotoCount, currentShot = capturedPhotos.size)
            }

            // Grid Foto dengan Border
            Box(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedPhotos) { photoPath ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(photoPath)).build(),
                            contentDescription = "Result",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, NeoBlack, RoundedCornerShape(12.dp)) // Border foto
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (!isAutoLoopActive && capturedPhotos.isEmpty()) {
                    ControlButton(Icons.Default.Settings, "Set", {})
                    ControlButton(Icons.Default.Refresh, "Clear", { capturedPhotos.clear() })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Start
            ShutterButton(
                isEnabled = !isAutoLoopActive && !isSessionRunning && capturedPhotos.size < settingPhotoCount,
                onClick = { handleShutterClick() }
            )
        }
    }
}