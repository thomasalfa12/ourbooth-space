package com.thomasalfa.photobooth.presentation.screens.admin

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onManageFrames: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    // Setting States
    var photoCount by remember { mutableIntStateOf(6) }
    var timerDuration by remember { mutableFloatStateOf(3f) }
    var captureMode by remember { mutableStateOf("AUTO") }
    var autoDelay by remember { mutableFloatStateOf(2f) }

    // NEW: Active Event State
    var activeEvent by remember { mutableStateOf("ALL") }
    val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    // Hitung kategori yang tersedia dari frame yang sudah diupload
    val availableEvents = remember(allFrames) {
        val categories = allFrames.map { it.category }.distinct().filter { it != "Default" }
        listOf("ALL", "Default Only") + categories
    }
    var isEventDropdownExpanded by remember { mutableStateOf(false) }

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
    // Load Active Event
    LaunchedEffect(Unit) {
        settingsManager.activeEventFlow.collect { activeEvent = it }
    }

    Box(modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SESSION CONTROL", style = MaterialTheme.typography.headlineMedium, color = NeoBlack, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // 1. ACTIVE EVENT (FITUR BARU)
                    Text("1. Active Event Context", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NeoPurple)
                    Text("Select which frames appear in result screen", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = activeEvent,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Event") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().clickable { isEventDropdownExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = NeoPurple,
                                disabledLabelColor = NeoPurple
                            )
                        )
                        Surface(
                            modifier = Modifier.matchParentSize().clickable { isEventDropdownExpanded = true },
                            color = Color.Transparent
                        ) {}

                        DropdownMenu(
                            expanded = isEventDropdownExpanded,
                            onDismissRequest = { isEventDropdownExpanded = false }
                        ) {
                            availableEvents.forEach { event ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(event, fontWeight = FontWeight.Bold)
                                            if(event == "ALL") Text("Show Default + All Custom", fontSize=10.sp, color=Color.Gray)
                                            else if(event == "Default Only") Text("Show Default frames only", fontSize=10.sp, color=Color.Gray)
                                            else Text("Show Default + $event frames", fontSize=10.sp, color=Color.Gray)
                                        }
                                    },
                                    onClick = { activeEvent = event; isEventDropdownExpanded = false }
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 2. MANAGEMENT
                    Text("2. Data Management", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onManageFrames, colors = ButtonDefaults.buttonColors(containerColor = NeoYellow), modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp)) {
                            Text("FRAMES LIBRARY", color = NeoBlack, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = onOpenHistory, colors = ButtonDefaults.buttonColors(containerColor = NeoBlue), modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.DateRange, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HISTORY", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 3. SETTINGS LAINNYA (Sama seperti sebelumnya)
                    Text("3. Photo Count", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(6, 8, 10).forEach { count ->
                            FilterChip(
                                selected = photoCount == count,
                                onClick = { photoCount = count },
                                label = { Text("$count Shots") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("4. Capture Mode", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "AUTO", onClick = { captureMode = "AUTO" })
                        Text("Auto Loop")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = captureMode == "MANUAL", onClick = { captureMode = "MANUAL" })
                        Text("Manual Tap")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("5. Countdown: ${timerDuration.roundToInt()}s", fontWeight = FontWeight.Bold)
                    Slider(value = timerDuration, onValueChange = { timerDuration = it }, valueRange = 3f..10f, steps = 6)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TOMBOL SAVE
            Button(
                onClick = {
                    scope.launch {
                        // SIMPAN SEMUA SETTING TERMASUK ACTIVE EVENT
                        settingsManager.saveSessionSettings(photoCount, timerDuration.roundToInt(), captureMode, autoDelay.roundToInt())
                        settingsManager.saveActiveEvent(activeEvent) // <-- SIMPAN EVENT
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeoGreen),
                modifier = Modifier.height(60.dp).fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("SAVE & APPLY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            // Tombol Restart (Sama seperti sebelumnya, boleh dipaste lagi)
            Button(
                onClick = {
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

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}