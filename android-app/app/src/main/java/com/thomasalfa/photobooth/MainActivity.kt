package com.thomasalfa.photobooth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.lifecycleScope
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.presentation.screens.admin.AdminScreen
import com.thomasalfa.photobooth.presentation.screens.admin.FrameManagerScreen
import com.thomasalfa.photobooth.presentation.screens.admin.SessionHistoryScreen
import com.thomasalfa.photobooth.presentation.screens.capture.CaptureScreen
import com.thomasalfa.photobooth.presentation.screens.editor.EditorScreen
import com.thomasalfa.photobooth.presentation.screens.home.HomeScreen
import com.thomasalfa.photobooth.presentation.screens.result.QrCodeScreen
import com.thomasalfa.photobooth.presentation.screens.result.ResultScreen
import com.thomasalfa.photobooth.presentation.screens.selection.FrameSelectionScreen
import com.thomasalfa.photobooth.ui.theme.KubikTheme
import com.thomasalfa.photobooth.utils.LocalDataManager
import com.thomasalfa.photobooth.utils.SupabaseManager
import com.thomasalfa.photobooth.utils.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)


        setContent {
            KubikTheme {
                val context = LocalContext.current

                // --- DATA SOURCES ---
                val settingsManager = remember { SettingsManager(context) }
                val db = remember { AppDatabase.getDatabase(context) }

                // Collect Settings Realtime
                val activeEvent by settingsManager.activeEventFlow.collectAsState(initial = "ALL")
                val correctPin by settingsManager.adminPinFlow.collectAsState(initial = "1234")

                // Collect Frames Realtime
                val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

                // --- FILTER LOGIC (SMART FILTERING) ---
                // Filter frame berdasarkan setting "Active Event" di Admin
                val availableFrames = remember(activeEvent, allFrames) {
                    when (activeEvent) {
                        "ALL" -> allFrames
                        "Default Only" -> allFrames.filter { it.category == "Default" }
                        // Jika event spesifik, tampilkan Frame Event Tersebut + Frame Default
                        else -> allFrames.filter { it.category == "Default" || it.category == activeEvent }
                    }
                }

                // --- APP STATES ---
                var currentScreen by remember { mutableStateOf("HOME") }

                // Session Data
                var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) } // Frame yang dipilih user
                var capturedPhotos by remember { mutableStateOf(listOf<String>()) } // Foto mentah dari kamera
                var finalPhotoOrder by remember { mutableStateOf(listOf<String>()) } // Foto urutan final dari Editor

                // Background Process State
                var currentSessionUuid by remember { mutableStateOf("") }
                var currentVideoPath by remember { mutableStateOf<String?>(null) }
                var isBackgroundUploadDone by remember { mutableStateOf(false) }
                var backgroundUploadError by remember { mutableStateOf<String?>(null) }

                // Result State
                var uploadedQrUrl by remember { mutableStateOf("") }

                // Admin Auth
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }

                // --- BACKGROUND LOGIC ---
                fun startBackgroundProcess(uuid: String, photosForVideo: List<String>) {
                    // Reset State
                    currentVideoPath = null
                    isBackgroundUploadDone = false
                    backgroundUploadError = null

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // 1. DB Init (Membuat sesi kosong di Supabase)
                            val dbSuccess = SupabaseManager.insertInitialSession(uuid, null)
                            if (!dbSuccess) throw Exception("DB Init Failed")

                            isBackgroundUploadDone = true // Signal UI OK

                            // 2. Video Generation (Stop Motion)
                            val videoResult = VideoProcessor.generateStopMotion(context, photosForVideo)
                            if (videoResult != null) {
                                currentVideoPath = videoResult
                                val videoFile = File(videoResult)
                                if (videoFile.exists()) {
                                    val videoUrl = SupabaseManager.uploadFile(videoFile)
                                    if (videoUrl != null) {
                                        SupabaseManager.updateSessionVideo(uuid, videoUrl)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            backgroundUploadError = e.message
                            // Tetap lanjut agar user tidak stuck (akan diretry di ResultScreen jika perlu)
                            isBackgroundUploadDone = true
                        }
                    }
                }

                // --- NAVIGATION GRAPH ---
                when (currentScreen) {
                    // 1. HOME SCREEN
                    "HOME" -> {
                        HomeScreen(
                            onStartSession = {
                                // RESET SEMUA DATA SESI BARU
                                capturedPhotos = emptyList()
                                finalPhotoOrder = emptyList()
                                selectedFrame = null
                                currentSessionUuid = ""
                                currentVideoPath = null
                                isBackgroundUploadDone = false

                                // Navigasi ke Pemilihan Frame (Flow Baru)
                                currentScreen = "FRAME_SELECTION"
                            },
                            onOpenAdmin = { pinInput = ""; showPinDialog = true }
                        )
                    }

                    // 2. FRAME SELECTION
                    "FRAME_SELECTION" -> {
                        FrameSelectionScreen(
                            frames = availableFrames, // Menggunakan Data Asli Database + Filter
                            onFrameSelected = { frame ->
                                selectedFrame = frame
                                currentScreen = "CAPTURE"
                            },
                            onBack = { currentScreen = "HOME" }
                        )
                    }

                    // 3. CAPTURE SCREEN
                    "CAPTURE" -> {
                        if (selectedFrame != null) {
                            CaptureScreen(
                                selectedFrame = selectedFrame!!,
                                onSessionComplete = { rawPhotos ->
                                    capturedPhotos = rawPhotos

                                    // Generate UUID & Mulai proses background (DB Init & Video)
                                    val newUuid = UUID.randomUUID().toString()
                                    currentSessionUuid = newUuid
                                    startBackgroundProcess(newUuid, rawPhotos)

                                    currentScreen = "EDITOR"
                                }
                            )
                        } else {
                            // Fallback safety
                            currentScreen = "FRAME_SELECTION"
                        }
                    }

                    // 4. EDITOR SCREEN
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

                    // 5. RESULT SCREEN
                    "RESULT" -> {
                        if (selectedFrame != null) {
                            ResultScreen(
                                photoPaths = finalPhotoOrder, // Foto Final dari Editor
                                selectedFrame = selectedFrame!!, // Frame Pilihan
                                sessionUuid = currentSessionUuid,
                                onRetake = {
                                    currentScreen = "HOME"
                                },
                                onFinishClicked = { finalQrUrl ->
                                    // Langsung dapat URL Final, lompat ke QR
                                    uploadedQrUrl = finalQrUrl

                                    // Simpan History Lokal (Opsional, untuk backup)
                                    // LocalDataManager.saveSessionToDb(...)

                                    currentScreen = "QR_DISPLAY"
                                }
                            )
                        }
                    }

                    // 6. QR DISPLAY
                    "QR_DISPLAY" -> {
                        QrCodeScreen(url = uploadedQrUrl, onFinish = { currentScreen = "HOME" })
                    }

                    // --- ADMIN SCREENS ---
                    "ADMIN" -> AdminScreen(
                        onBack = { currentScreen = "HOME" },
                        onManageFrames = { currentScreen = "FRAME_MANAGER" },
                        onOpenHistory = { currentScreen = "SESSION_HISTORY" },
                        // Callback untuk Matikan Kiosk Mode
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

    // --- FUNGSI KIOSK MODE ---
    private fun startKioskMode() {
        try {
            // Mengunci layar agar user tidak bisa keluar (Home/Back/Recent disembunyikan)
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopKioskMode() {
        try {
            stopLockTask()
            Toast.makeText(this, "Kiosk Mode Disabled", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}