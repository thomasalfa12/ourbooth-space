package com.thomasalfa.photobooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.presentation.screens.admin.AdminScreen
import com.thomasalfa.photobooth.presentation.screens.admin.FrameManagerScreen
import com.thomasalfa.photobooth.presentation.screens.admin.SessionHistoryScreen
import com.thomasalfa.photobooth.presentation.screens.auth.LoginActivityScreen
import com.thomasalfa.photobooth.presentation.screens.capture.CaptureScreen
import com.thomasalfa.photobooth.presentation.screens.editor.EditorScreen
import com.thomasalfa.photobooth.presentation.screens.home.HomeScreen
import com.thomasalfa.photobooth.presentation.screens.result.QrCodeScreen
import com.thomasalfa.photobooth.presentation.screens.result.ResultScreen
import com.thomasalfa.photobooth.presentation.screens.selection.FrameSelectionScreen
import com.thomasalfa.photobooth.presentation.screens.ticket.TicketScreen
import com.thomasalfa.photobooth.ui.theme.KubikBlue
import com.thomasalfa.photobooth.ui.theme.KubikTheme
import com.thomasalfa.photobooth.utils.LocalDataManager
import com.thomasalfa.photobooth.utils.SupabaseManager
// VideoProcessor import DIHAPUS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        hideSystemUI()

        // Init Realtime Connection
        lifecycleScope.launch(Dispatchers.IO) {
            SupabaseManager.initializeRealtime()
        }

        setContent {
            KubikTheme {
                val context = LocalContext.current

                // --- DATA SOURCES ---
                val settingsManager = remember { SettingsManager(context) }
                val db = remember { AppDatabase.getDatabase(context) }

                val deviceType by settingsManager.deviceTypeFlow.collectAsState(initial = "RENTAL")
                val deviceId by settingsManager.deviceIdFlow.collectAsState(initial = "")

                // --- AUTH STATE (LOGIN CHECKER) ---
                var isDeviceLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    settingsManager.isLoggedInFlow.collect { loggedIn ->
                        isDeviceLoggedIn = loggedIn
                    }
                }
                


                // --- APP STATES ---
                var currentScreen by remember { mutableStateOf("LOADING") }

                LaunchedEffect(isDeviceLoggedIn) {
                    if (isDeviceLoggedIn == true) {
                        currentScreen = "HOME"
                    } else if (isDeviceLoggedIn == false) {
                        currentScreen = "LOGIN"
                    }
                }

                LaunchedEffect(isDeviceLoggedIn, deviceId) {
                    val safeDeviceId = deviceId?.takeIf { it.isNotBlank() }

                    if (isDeviceLoggedIn != true || safeDeviceId == null) {
                        Log.d("MAIN", "⚠️ Monitoring skipped")
                        return@LaunchedEffect
                    }

                    Log.d("MAIN", "🚀 Starting Monitoring: $safeDeviceId")

                    try {
                        // 1. Initial Check
                        val currentStatus = SupabaseManager.fetchSingleDeviceStatus(safeDeviceId)
                        Log.d("MAIN", "🔐 Initial Status: $currentStatus")

                        if (currentStatus != "ACTIVE") {
                            Log.w("MAIN", "🚨 Device not active: $currentStatus")
                            settingsManager.logout()
                            currentScreen = "LOGIN"
                            return@LaunchedEffect
                        }

                        // 2. Realtime Monitoring
                        Log.d("MAIN", "👂 Listening to status changes...")

                        SupabaseManager.observeDeviceStatus(safeDeviceId).collect { status ->
                            Log.d("MAIN", "📩 Status update: $status")

                            if (status != "ACTIVE") {
                                Log.e("MAIN", "🔴 KILL SWITCH ACTIVATED: $status")

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Device has been ${status.lowercase()} by admin",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                settingsManager.logout()
                                currentScreen = "LOGIN"
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("MAIN", "❌ Monitoring error: ${e.message}", e)
                    }
                }
                // --- SETTINGS COLLECTION ---
                val activeEvent by settingsManager.activeEventFlow.collectAsState(initial = "ALL")
                val correctPin by settingsManager.adminPinFlow.collectAsState(initial = "1234")
                val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

                val availableFrames = remember(activeEvent, allFrames) {
                    when (activeEvent) {
                        "ALL" -> allFrames
                        "Default Only" -> allFrames.filter { it.category == "Default" }
                        else -> allFrames.filter { it.category == "Default" || it.category == activeEvent }
                    }
                }

                // Session Data
                var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) }
                var capturedPhotos by remember { mutableStateOf(listOf<String>()) }
                var finalPhotoOrder by remember { mutableStateOf(listOf<String>()) }

                // Process State
                var currentSessionUuid by remember { mutableStateOf("") }
                var uploadedQrUrl by remember { mutableStateOf("") }

                // Admin Auth UI
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }

                // --- HELPER: KOMPRESI GAMBAR UNTUK UPLOAD ---
                // Agar upload cepat, kita resize foto ke Full HD (1920px) kualitas 90%
                fun compressImageForUpload(originalPath: String): File? {
                    return try {
                        val originalFile = File(originalPath)
                        if (!originalFile.exists()) return null

                        val bitmap = BitmapFactory.decodeFile(originalPath) ?: return null

                        // Buat file cache sementara
                        val compressedFile = File(context.cacheDir, "upload_${originalFile.name}")
                        val stream = FileOutputStream(compressedFile)

                        // Kompresi JPEG 90% (Sangat cukup untuk Web/HP)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        stream.flush()
                        stream.close()

                        compressedFile
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // --- BACKGROUND LOGIC (NEW: RAW PHOTOS BATCH UPLOAD) ---
                fun startBackgroundProcess(uuid: String, photosToUpload: List<String>) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // 1. Ambil Device ID
                            val deviceId = settingsManager.deviceIdFlow.first()

                            // 2. DB Init (Create Session Row)
                            val dbSuccess = SupabaseManager.insertInitialSession(uuid, null, deviceId)
                            if (!dbSuccess) throw Exception("DB Init Failed")

                            // 3. COMPRESS & UPLOAD RAW PHOTOS (Parallel)
                            // Ubah path string menjadi File yang sudah dikompres
                            val filesToUpload = photosToUpload.mapNotNull { path ->
                                compressImageForUpload(path)
                            }

                            if (filesToUpload.isNotEmpty()) {
                                // Upload ke Supabase Storage (Ngebut!)
                                val uploadedUrls = SupabaseManager.uploadMultipleFiles(filesToUpload)

                                // Update DB dengan list URL (JSON)
                                if (uploadedUrls.isNotEmpty()) {
                                    SupabaseManager.updateSessionRawPhotos(uuid, uploadedUrls)
                                }
                            }

                            // Cleanup: Hapus file kompresi sementara agar memori tidak penuh
                            filesToUpload.forEach { it.delete() }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Error silent agar tidak mengganggu user flow
                        }
                    }
                }

                // --- NAVIGATION GRAPH ---
                when (currentScreen) {
                    "LOADING" -> {
                        Box(modifier = Modifier.fillMaxSize().background(KubikBlue), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    "LOGIN" -> {
                        LoginActivityScreen(
                            onLoginSuccess = { /* Auto handle by LaunchedEffect */ }
                        )
                    }

                    "HOME" -> {
                        HomeScreen(
                            onStartSession = {
                                capturedPhotos = emptyList()
                                finalPhotoOrder = emptyList()
                                selectedFrame = null
                                currentSessionUuid = ""
                                if (deviceType == "VENDING") {
                                    currentScreen = "TICKET_INPUT" // Minta Tiket Dulu
                                } else {
                                    currentScreen = "FRAME_SELECTION" // Langsung Foto (Rental)
                                }
                            },
                            onOpenAdmin = { pinInput = ""; showPinDialog = true }
                        )
                    }

                    "TICKET_INPUT" -> {
                        TicketScreen(
                            deviceId = deviceId ?: "",
                            onTicketValid = {
                                // Tiket Valid -> Lanjut Pilih Frame
                                currentScreen = "FRAME_SELECTION"
                            },
                            onBack = {
                                currentScreen = "HOME"
                            }
                        )
                    }

                    "FRAME_SELECTION" -> {
                        FrameSelectionScreen(
                            frames = availableFrames,
                            onFrameSelected = { frame ->
                                selectedFrame = frame
                                currentScreen = "CAPTURE"
                            },
                            onBack = { currentScreen = "HOME" }
                        )
                    }

                    "CAPTURE" -> {
                        if (selectedFrame != null) {
                            CaptureScreen(
                                selectedFrame = selectedFrame!!,
                                onSessionComplete = { rawPhotos ->
                                    capturedPhotos = rawPhotos

                                    // Generate UUID
                                    val newUuid = UUID.randomUUID().toString()
                                    currentSessionUuid = newUuid

                                    // [NEW] Start Upload Raw Photos in Background
                                    startBackgroundProcess(newUuid, rawPhotos)

                                    currentScreen = "EDITOR"
                                }
                            )
                        } else {
                            currentScreen = "FRAME_SELECTION"
                        }
                    }

                    "EDITOR" -> {
                        if (selectedFrame != null && capturedPhotos.isNotEmpty()) {
                            EditorScreen(
                                capturedPhotos = capturedPhotos,
                                selectedFrame = selectedFrame!!,
                                onEditingComplete = { orderedPhotos ->
                                    finalPhotoOrder = orderedPhotos
                                    currentScreen = "RESULT"
                                }
                            )
                        }
                    }

                    "RESULT" -> {
                        if (selectedFrame != null) {
                            ResultScreen(
                                photoPaths = finalPhotoOrder,
                                selectedFrame = selectedFrame!!,
                                sessionUuid = currentSessionUuid,
                                onRetake = { currentScreen = "HOME" },
                                onFinishClicked = { finalQrUrl, finalLayoutFilePath ->
                                    uploadedQrUrl = finalQrUrl

                                    // Save Local Database
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        LocalDataManager.saveSessionToDb(
                                            context = context,
                                            uuid = currentSessionUuid,
                                            finalPath = finalLayoutFilePath,
                                            gifPath = null, // Video sudah tidak ada
                                            rawPhotoPaths = finalPhotoOrder
                                        )
                                    }

                                    currentScreen = "QR_DISPLAY"
                                }
                            )
                        }
                    }

                    "QR_DISPLAY" -> {
                        QrCodeScreen(url = uploadedQrUrl, onFinish = { currentScreen = "HOME" })
                    }

                    "ADMIN" -> AdminScreen(
                        onBack = { currentScreen = "HOME" },
                        onManageFrames = { currentScreen = "FRAME_MANAGER" },
                        onOpenHistory = { currentScreen = "SESSION_HISTORY" },
                        onExitKiosk = { stopKioskMode() }
                    )
                    "SESSION_HISTORY" -> SessionHistoryScreen(onBack = { currentScreen = "ADMIN" })
                    "FRAME_MANAGER" -> FrameManagerScreen(onBack = { currentScreen = "ADMIN" })
                }

                // --- PIN DIALOG ---
                if (showPinDialog) {
                    Dialog(onDismissRequest = { showPinDialog = false }) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Admin Access", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 4) pinInput = it },
                                    label = { Text("PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                                    Button(onClick = { if (pinInput == correctPin) { showPinDialog = false; currentScreen = "ADMIN" } else { Toast.makeText(context, "Wrong PIN!", Toast.LENGTH_SHORT).show() } }) { Text("Enter") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopKioskMode() {
        try { stopLockTask(); Toast.makeText(this, "Kiosk Disabled", Toast.LENGTH_SHORT).show() } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}