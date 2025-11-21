package com.thomasalfa.photobooth

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KubikTheme {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }

                var currentScreen by remember { mutableStateOf("HOME") }

                // STATE INTRO
                var hasPlayedIntro by rememberSaveable { mutableStateOf(false) }

                // STATE DATA FOTO (Passing antar layar)
                var capturedPhotos by remember { mutableStateOf(listOf<String>()) }
                var selectedPhotos by remember { mutableStateOf(listOf<String>()) }
                var boomerangPhotos by remember { mutableStateOf(listOf<String>()) }

                // STATE UPLOAD & QR (Inilah yang tadi error unresolved)
                // Kita simpan hasil final (JPG) dan GIF disini agar bisa diakses oleh UploadScreen
                var finalResultPath by remember { mutableStateOf<String?>(null) }
                var finalGifPath by remember { mutableStateOf<String?>(null) }
                var uploadedQrUrl by remember { mutableStateOf("") }

                // PIN State
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                val correctPin by settingsManager.adminPinFlow.collectAsState(initial = "1234")

                when (currentScreen) {
                    "HOME" -> {
                        HomeScreen(
                            hasPlayedIntro = hasPlayedIntro,
                            onIntroFinished = { hasPlayedIntro = true },
                            onStartSession = { currentScreen = "CAPTURE" },
                            onOpenAdmin = {
                                pinInput = ""
                                showPinDialog = true
                            }
                        )
                    }
                    "ADMIN" -> {
                        AdminScreen(
                            onBack = { currentScreen = "HOME" },
                            onManageFrames = { currentScreen = "FRAME_MANAGER" },
                            onOpenHistory = { currentScreen = "SESSION_HISTORY" }
                        )
                    }
                    "SESSION_HISTORY" -> {
                        SessionHistoryScreen(
                            onBack = { currentScreen = "ADMIN" }
                        )
                    }
                    "FRAME_MANAGER" -> {
                        FrameManagerScreen(
                            onBack = { currentScreen = "ADMIN" }
                        )
                    }
                    "CAPTURE" -> {
                        CaptureScreen(
                            onSessionComplete = { photos, booms ->
                                capturedPhotos = photos
                                boomerangPhotos = booms
                                currentScreen = "SELECTION"
                            }
                        )
                    }
                    "SELECTION" -> {
                        SelectionScreen(
                            allPhotos = capturedPhotos,
                            onSelectionComplete = { finalSelection ->
                                selectedPhotos = finalSelection
                                currentScreen = "RESULT"
                            }
                        )
                    }
                    "RESULT" -> {
                        ResultScreen(
                            photoPaths = selectedPhotos,
                            boomerangPaths = boomerangPhotos,
                            onRetake = {
                                capturedPhotos = emptyList()
                                selectedPhotos = emptyList()
                                boomerangPhotos = emptyList()
                                currentScreen = "HOME"
                            },
                            // Callback saat tombol Finish ditekan
                            onFinishClicked = { finalPath, gifPath ->
                                finalResultPath = finalPath // Simpan path foto jadi
                                finalGifPath = gifPath     // Simpan path GIF
                                currentScreen = "UPLOAD_PROGRESS" // Pindah ke pesawat
                            }
                        )
                    }

                    "UPLOAD_PROGRESS" -> {
                        UploadProgressScreen(
                            photoPath = finalResultPath, // Kirim path foto jadi
                            gifPath = finalGifPath,      // Kirim path GIF
                            onUploadSuccess = { webLink ->
                                uploadedQrUrl = webLink  // Simpan link website
                                currentScreen = "QR_DISPLAY"
                            },
                            onUploadFailed = {
                                Toast.makeText(context, "Upload Gagal, Cek Koneksi", Toast.LENGTH_SHORT).show()
                                currentScreen = "RESULT" // Balik ke result
                            }
                        )
                    }

                    "QR_DISPLAY" -> {
                        QrCodeScreen(
                            url = uploadedQrUrl, // Tampilkan link website
                            onFinish = {
                                // Reset Total & Balik Home
                                capturedPhotos = emptyList()
                                selectedPhotos = emptyList()
                                boomerangPhotos = emptyList()
                                currentScreen = "HOME"
                            }
                        )
                    }
                }

                // Dialog PIN
                if (showPinDialog) {
                    Dialog(onDismissRequest = { showPinDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Admin Access", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 4) pinInput = it },
                                    label = { Text("Enter PIN") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                                    Button(
                                        onClick = {
                                            if (pinInput == correctPin) {
                                                showPinDialog = false
                                                currentScreen = "ADMIN"
                                            } else {
                                                Toast.makeText(context, "Wrong PIN!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) { Text("Enter") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}