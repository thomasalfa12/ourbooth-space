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
import com.thomasalfa.photobooth.presentation.screens.capture.CaptureScreen
import com.thomasalfa.photobooth.presentation.screens.home.HomeScreen
import com.thomasalfa.photobooth.presentation.screens.result.ResultScreen
import com.thomasalfa.photobooth.presentation.screens.selection.SelectionScreen
import com.thomasalfa.photobooth.ui.theme.KubikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KubikTheme {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }

                // Navigation State
                var currentScreen by remember { mutableStateOf("HOME") }

                // Data State
                var capturedPhotos by remember { mutableStateOf(listOf<String>()) }
                var selectedPhotos by remember { mutableStateOf(listOf<String>()) }
                // 1. Tambahkan state untuk menyimpan Frame yang dipilih
                var selectedFramePath by remember { mutableStateOf<String?>(null) }

                // PIN Dialog State
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                val correctPin by settingsManager.adminPinFlow.collectAsState(initial = "1234")

                when (currentScreen) {
                    "HOME" -> {
                        HomeScreen(
                            onStartSession = {
                                // Reset state saat sesi baru mulai
                                capturedPhotos = emptyList()
                                selectedPhotos = emptyList()
                                selectedFramePath = null
                                currentScreen = "CAPTURE"
                            },
                            onOpenAdmin = {
                                pinInput = ""
                                showPinDialog = true
                            }
                        )
                    }
                    "ADMIN" -> {
                        AdminScreen(
                            onBack = { currentScreen = "HOME" },
                            onManageFrames = { currentScreen = "FRAME_MANAGER" }
                        )
                    }
                    "FRAME_MANAGER" -> {
                        FrameManagerScreen(
                            onBack = { currentScreen = "ADMIN" }
                        )
                    }
                    "CAPTURE" -> {
                        CaptureScreen(
                            onSessionComplete = { photos ->
                                capturedPhotos = photos
                                currentScreen = "SELECTION"
                            }
                        )
                    }
                    "SELECTION" -> {
                        SelectionScreen(
                            allPhotos = capturedPhotos,
                            onSelectionComplete = { finalPhotos ->
                                selectedPhotos = finalPhotos
                                currentScreen = "RESULT"
                            }
                        )
                    }
                    "RESULT" -> {
                        // HAPUS parameter selectedFramePath
                        ResultScreen(
                            photoPaths = selectedPhotos,
                            onRetake = {
                                capturedPhotos = emptyList()
                                selectedPhotos = emptyList()
                                // selectedFramePath = null // Hapus baris ini juga
                                currentScreen = "HOME"
                            }
                        )
                    }
                }

                // --- DIALOG PIN ADMIN ---
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