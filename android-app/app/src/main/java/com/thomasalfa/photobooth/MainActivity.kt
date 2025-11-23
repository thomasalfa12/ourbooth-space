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
import com.thomasalfa.photobooth.presentation.screens.admin.AdminScreen
import com.thomasalfa.photobooth.presentation.screens.admin.FrameManagerScreen
import com.thomasalfa.photobooth.presentation.screens.admin.SessionHistoryScreen
import com.thomasalfa.photobooth.presentation.screens.capture.CaptureScreen
import com.thomasalfa.photobooth.presentation.screens.home.HomeScreen
import com.thomasalfa.photobooth.presentation.screens.result.QrCodeScreen
import com.thomasalfa.photobooth.presentation.screens.result.ResultScreen
import com.thomasalfa.photobooth.presentation.screens.result.UploadProgressScreen
import com.thomasalfa.photobooth.presentation.screens.selection.SelectionScreen
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
                val settingsManager = remember { SettingsManager(context) }

                var currentScreen by remember { mutableStateOf("HOME") }

                // State Intro & Data
                var hasPlayedIntro by rememberSaveable { mutableStateOf(false) }
                var capturedPhotos by remember { mutableStateOf(listOf<String>()) }
                var selectedPhotos by remember { mutableStateOf(listOf<String>()) }

                // State Background Process
                var currentSessionUuid by remember { mutableStateOf("") }
                var currentVideoPath by remember { mutableStateOf<String?>(null) }
                var isBackgroundUploadDone by remember { mutableStateOf(false) }
                var backgroundUploadError by remember { mutableStateOf<String?>(null) }

                // State Result
                var finalResultPath by remember { mutableStateOf<String?>(null) }
                var uploadedQrUrl by remember { mutableStateOf("") }

                // Admin State
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                val correctPin by settingsManager.adminPinFlow.collectAsState(initial = "1234")

                // --- FUNGSI BACKGROUND (Fire & Forget) ---
                fun startBackgroundProcess(uuid: String, photosForVideo: List<String>) {
                    Log.d("DEBUG_KUBIK", "START Background Process UUID: $uuid")

                    // 1. RESET STATE PENTING (FIX MASALAH VIDEO HILANG)
                    currentVideoPath = null // Wajib null biar VideoProcessor jalan lagi
                    isBackgroundUploadDone = false
                    backgroundUploadError = null

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // TAHAP 1: DB INIT (Prioritas Utama - Cepat)
                            val dbSuccess = SupabaseManager.insertInitialSession(uuid, null)

                            if (!dbSuccess) {
                                throw Exception("Gagal koneksi ke Database awal")
                            }

                            // TAHAP 2: BUKA GERBANG UI
                            Log.d("DEBUG_KUBIK", "⚡ DB Ready. Signaling UI to proceed...")
                            isBackgroundUploadDone = true

                            // ---------------------------------------------------------
                            // TAHAP 3: GENERATE VIDEO (Susulan / Background)
                            // ---------------------------------------------------------
                            // Logic ini pasti jalan karena currentVideoPath sudah di-null-kan di atas
                            if (currentVideoPath == null) {
                                Log.d("DEBUG_KUBIK", "🎥 Generating Video in background...")
                                val videoResult = VideoProcessor.generateStopMotion(context, photosForVideo)

                                if (videoResult != null) {
                                    currentVideoPath = videoResult // Simpan path baru

                                    // Upload Video
                                    val videoFile = File(videoResult)
                                    if (videoFile.exists()) {
                                        Log.d("DEBUG_KUBIK", "⬆️ Uploading Video in background...")
                                        val videoUrl = SupabaseManager.uploadFile(videoFile)

                                        if (videoUrl != null) {
                                            // Update DB Susulan
                                            SupabaseManager.updateSessionVideo(uuid, videoUrl)
                                            Log.d("DEBUG_KUBIK", "✅ Video Sync Complete!")
                                        }
                                    }
                                } else {
                                    Log.e("DEBUG_KUBIK", "❌ Video Generation returned NULL")
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("DEBUG_KUBIK", "❌ Error di background: ${e.message}")
                            backgroundUploadError = e.message
                            // Tetap buka gerbang agar user tidak stuck
                            isBackgroundUploadDone = true
                        }
                    }
                }

                when (currentScreen) {
                    "HOME" -> {
                        HomeScreen(
                            hasPlayedIntro = hasPlayedIntro,
                            onIntroFinished = { hasPlayedIntro = true },
                            onStartSession = {
                                Log.d("DEBUG_KUBIK", "Starting New Session")
                                // Reset Data Sesi
                                capturedPhotos = emptyList()
                                selectedPhotos = emptyList()
                                currentSessionUuid = ""
                                currentVideoPath = null // Reset juga disini biar double safety
                                isBackgroundUploadDone = false
                                backgroundUploadError = null
                                currentScreen = "CAPTURE"
                            },
                            onOpenAdmin = { pinInput = ""; showPinDialog = true }
                        )
                    }
                    "ADMIN" -> AdminScreen(
                        onBack = { currentScreen = "HOME" },
                        onManageFrames = { currentScreen = "FRAME_MANAGER" },
                        onOpenHistory = { currentScreen = "SESSION_HISTORY" }
                    )
                    "SESSION_HISTORY" -> SessionHistoryScreen(onBack = { currentScreen = "ADMIN" })
                    "FRAME_MANAGER" -> FrameManagerScreen(onBack = { currentScreen = "ADMIN" })

                    "CAPTURE" -> {
                        CaptureScreen(
                            onSessionComplete = { photos, _ ->
                                capturedPhotos = photos

                                // CURI START PROCESS
                                val newUuid = UUID.randomUUID().toString()
                                currentSessionUuid = newUuid
                                startBackgroundProcess(newUuid, photos)

                                currentScreen = "SELECTION"
                            }
                        )
                    }

                    "SELECTION" -> {
                        SelectionScreen(
                            allPhotos = capturedPhotos,
                            onSelectionComplete = { finalSelection ->
                                selectedPhotos = finalSelection
                                // Jangan panggil startBackgroundProcess lagi disini!
                                currentScreen = "RESULT"
                            }
                        )
                    }

                    "RESULT" -> {
                        ResultScreen(
                            photoPaths = selectedPhotos,
                            onRetake = {
                                capturedPhotos = emptyList(); selectedPhotos = emptyList()
                                currentScreen = "HOME"
                            },
                            onFinishClicked = { finalPath ->
                                finalResultPath = finalPath

                                // Simpan History Lokal
                                lifecycleScope.launch {
                                    LocalDataManager.saveSessionToDb(context, currentSessionUuid, finalPath, currentVideoPath, selectedPhotos)
                                }

                                // Retry Logic (Hanya jika DB Init gagal total sebelumnya)
                                if (backgroundUploadError != null) {
                                    Toast.makeText(context, "Retrying Connection...", Toast.LENGTH_SHORT).show()
                                    // Kita retry logic yang sama
                                    startBackgroundProcess(currentSessionUuid, capturedPhotos)
                                }

                                currentScreen = "UPLOAD_PROGRESS"
                            }
                        )
                    }

                    "UPLOAD_PROGRESS" -> {
                        UploadProgressScreen(
                            photoPath = finalResultPath,
                            sessionUuid = currentSessionUuid,
                            isBackgroundUploadDone = isBackgroundUploadDone,
                            backgroundError = backgroundUploadError,
                            onUploadSuccess = { webLink ->
                                uploadedQrUrl = webLink
                                currentScreen = "QR_DISPLAY"
                            },
                            onUploadFailed = {
                                Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                                currentScreen = "RESULT"
                            }
                        )
                    }

                    "QR_DISPLAY" -> {
                        QrCodeScreen(url = uploadedQrUrl, onFinish = { currentScreen = "HOME" })
                    }
                }

                if (showPinDialog) {
                    Dialog(onDismissRequest = { showPinDialog = false }) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Admin Access", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(value = pinInput, onValueChange = { if (it.length <= 4) pinInput = it }, label = { Text("PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
}