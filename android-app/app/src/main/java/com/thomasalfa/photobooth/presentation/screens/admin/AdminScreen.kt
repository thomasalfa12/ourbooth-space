package com.thomasalfa.photobooth.presentation.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onManageFrames: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // State Data
    var currentFramePath by remember { mutableStateOf<String?>(null) }

    // Session Config State
    var photoCount by remember { mutableIntStateOf(6) }
    var timerDuration by remember { mutableFloatStateOf(3f) }

    // State Baru
    var captureMode by remember { mutableStateOf("AUTO") } // "AUTO" or "MANUAL"
    var autoDelay by remember { mutableFloatStateOf(2f) }  // Detik

    // Load Data
    LaunchedEffect(Unit) {
        settingsManager.framePathFlow.collect { currentFramePath = it }
    }
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val outputFile = File(context.filesDir, "custom_frame_v1.png")
            scope.launch {
                inputStream?.use { input -> FileOutputStream(outputFile).use { output -> input.copyTo(output) } }
                settingsManager.saveFramePath(outputFile.absolutePath)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()), // Bisa di-scroll kalau setting makin banyak
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SESSION CONTROL", style = MaterialTheme.typography.headlineMedium, color = NeoBlack, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text("1. Frame Library", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onManageFrames,
                        colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("MANAGE FRAMES & CATEGORIES", color = NeoBlack, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

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

                    // 3. MODE CAPTURE (BARU!)
                    Text("3. Capture Mode", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "AUTO", onClick = { captureMode = "AUTO" })
                        Text("Auto Loop (Sekali klik, jalan sendiri)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "MANUAL", onClick = { captureMode = "MANUAL" })
                        Text("Manual (Klik tombol tiap foto)")
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. TIMER & DELAY
                    Text("4. Timings", fontWeight = FontWeight.Bold)

                    // A. Countdown Timer (Selalu Ada)
                    Text("Countdown: ${timerDuration.roundToInt()} sec", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = timerDuration, onValueChange = { timerDuration = it },
                        valueRange = 3f..10f, steps = 6,
                        colors = SliderDefaults.colors(thumbColor = NeoPurple, activeTrackColor = NeoPurple)
                    )

                    // B. Delay Antar Foto (Cuma muncul kalau AUTO)
                    if (captureMode == "AUTO") {
                        Text("Delay between shots: ${autoDelay.roundToInt()} sec", fontSize = 12.sp, color = Color.Gray)
                        Slider(
                            value = autoDelay, onValueChange = { autoDelay = it },
                            valueRange = 2f..10f, steps = 7, // 2 sampai 10 detik
                            colors = SliderDefaults.colors(thumbColor = NeoPink, activeTrackColor = NeoPink)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        // Simpan SEMUA setting baru
                        settingsManager.saveSessionSettings(photoCount, timerDuration.roundToInt(), captureMode, autoDelay.roundToInt())
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeoGreen),
                modifier = Modifier.height(60.dp).width(200.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE CHANGES", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}