package com.thomasalfa.photobooth.presentation.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onManageFrames: () -> Unit,
    onOpenHistory: () -> Unit // <--- 1. PARAMETER BARU
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // Session Config State
    var photoCount by remember { mutableIntStateOf(6) }
    var timerDuration by remember { mutableFloatStateOf(3f) }
    var captureMode by remember { mutableStateOf("AUTO") }
    var autoDelay by remember { mutableFloatStateOf(2f) }

    // Load Data
    LaunchedEffect(Unit) {
        settingsManager.photoCountFlow.collect { photoCount = it }
    }
    LaunchedEffect(Unit) {
        settingsManager.timerDurationFlow.collect { timerDuration = it.toFloat() }
    }
    LaunchedEffect(Unit) {
        settingsManager.captureModeFlow.collect { captureMode = it }
    }
    LaunchedEffect(Unit) {
        settingsManager.autoDelayFlow.collect { autoDelay = it.toFloat() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SESSION CONTROL", style = MaterialTheme.typography.headlineMedium, color = NeoBlack, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth(0.9f) // Sedikit lebih lebar
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // 1. MANAGEMENT (FRAME & HISTORY)
                    Text("1. Data Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Tombol Frames
                        Button(
                            onClick = onManageFrames,
                            colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("FRAMES", color = NeoBlack, fontWeight = FontWeight.Bold)
                        }

                        // Tombol History (BARU)
                        Button(
                            onClick = onOpenHistory,
                            colors = ButtonDefaults.buttonColors(containerColor = NeoBlue),
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HISTORY", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 2. JUMLAH FOTO
                    Text("2. Total Shots", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(6, 8, 10).forEach { count ->
                            FilterChip(
                                selected = photoCount == count,
                                onClick = { photoCount = count },
                                label = { Text("$count Shots") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeoPurple, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // 3. MODE CAPTURE
                    Text("3. Capture Mode", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "AUTO", onClick = { captureMode = "AUTO" })
                        Text("Auto Loop (Otomatis)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "MANUAL", onClick = { captureMode = "MANUAL" })
                        Text("Manual (Klik per foto)")
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. TIMINGS
                    Text("4. Timings", fontWeight = FontWeight.Bold)
                    Text("Countdown: ${timerDuration.roundToInt()} sec", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = timerDuration, onValueChange = { timerDuration = it },
                        valueRange = 3f..10f, steps = 6,
                        colors = SliderDefaults.colors(thumbColor = NeoPurple, activeTrackColor = NeoPurple)
                    )

                    if (captureMode == "AUTO") {
                        Text("Delay between shots: ${autoDelay.roundToInt()} sec", fontSize = 12.sp, color = Color.Gray)
                        Slider(
                            value = autoDelay, onValueChange = { autoDelay = it },
                            valueRange = 2f..10f, steps = 7,
                            colors = SliderDefaults.colors(thumbColor = NeoPink, activeTrackColor = NeoPink)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TOMBOL SAVE (Hijau)
            Button(
                onClick = {
                    scope.launch {
                        settingsManager.saveSessionSettings(photoCount, timerDuration.roundToInt(), captureMode, autoDelay.roundToInt())
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeoGreen),
                modifier = Modifier.height(60.dp).fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE & BACK TO HOME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL EMERGENCY RESTART (Merah)
            // Ini fitur penting untuk Kiosk Mode jika kamera macet
            Text("SYSTEM MAINTENANCE", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // Logic Restart App
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    exitProcess(0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("FORCE RESTART APP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(50.dp)) // Jarak bawah
        }
    }
}