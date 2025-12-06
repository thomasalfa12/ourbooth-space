package com.thomasalfa.photobooth.presentation.screens.admin

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onManageFrames: () -> Unit,
    onOpenHistory: () -> Unit,
    onExitKiosk: () -> Unit // Parameter Kiosk Mode
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val db = remember { AppDatabase.getDatabase(context) }

    // --- LOAD DEVICE INFO ---
    val deviceName by settingsManager.deviceNameFlow.collectAsState(initial = "Loading...")
    val deviceType by settingsManager.deviceTypeFlow.collectAsState(initial = "-")

    // --- SETTING STATES ---
    var photoCount by remember { mutableIntStateOf(8) }
    var timerDuration by remember { mutableFloatStateOf(3f) }
    var captureMode by remember { mutableStateOf("AUTO") }
    var autoDelay by remember { mutableFloatStateOf(3f) }

    // --- ACTIVE EVENT STATE ---
    var activeEvent by remember { mutableStateOf("ALL") }
    val allFrames by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    val availableEvents = remember(allFrames) {
        val categories = allFrames.map { it.category }.distinct().filter { it != "Default" }
        listOf("ALL", "Default Only") + categories
    }
    var isEventDropdownExpanded by remember { mutableStateOf(false) }

    // --- LOAD DATA ---
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
    LaunchedEffect(Unit) {
        settingsManager.activeEventFlow.collect { activeEvent = it }
    }

    Box(modifier = Modifier.fillMaxSize().background(KubikBg).padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = KubikBlue, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("SYSTEM CONTROL", style = MaterialTheme.typography.headlineMedium, color = KubikBlack, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- [NEW] DEVICE INFO CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = KubikBlue),
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("DEVICE IDENTITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha=0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(deviceName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White.copy(alpha=0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text(deviceType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal=6.dp, vertical=2.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = KubikSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Online & Active", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Icon Info Kecil
                    Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha=0.5f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SETTINGS CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(32.dp)) {

                    // 1. ACTIVE EVENT
                    SectionHeader("1. Active Event (Filtering)")
                    Text("Only frames from this category will be shown to users.", fontSize = 12.sp, color = KubikGrey)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = activeEvent,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected Event") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            modifier = Modifier.fillMaxWidth().clickable { isEventDropdownExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = KubikBlack,
                                disabledBorderColor = KubikBlue,
                                disabledLabelColor = KubikBlue,
                                disabledTrailingIconColor = KubikBlue
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
                                            val desc = when(event) {
                                                "ALL" -> "Show Default + Custom"
                                                "Default Only" -> "Show Standard Frames"
                                                else -> "Show Default + $event"
                                            }
                                            Text(desc, fontSize=10.sp, color=Color.Gray)
                                        }
                                    },
                                    onClick = { activeEvent = event; isEventDropdownExpanded = false }
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 2. DATA MANAGEMENT
                    SectionHeader("2. Library Management")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onManageFrames,
                            colors = ButtonDefaults.buttonColors(containerColor = KubikGold),
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("MANAGE FRAMES", color = KubikBlack, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onOpenHistory,
                            colors = ButtonDefaults.buttonColors(containerColor = KubikBlue),
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIEW HISTORY", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // 3. CAPTURE SETTINGS
                    SectionHeader("3. Session Configuration")

                    // A. PHOTO COUNT
                    Text("Total Photos to Capture", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=8.dp))
                    Text("Take more photos than needed to allow users to swap.", fontSize = 11.sp, color = KubikGrey)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(8, 10, 12).forEach { count ->
                            FilterChip(
                                selected = photoCount == count,
                                onClick = { photoCount = count },
                                label = { Text("$count Shots") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KubikBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // B. CAPTURE MODE & DELAY
                    Text("Capture Logic", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = captureMode == "AUTO", onClick = { captureMode = "AUTO" })
                        Text("Auto Loop (Recommended)")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = captureMode == "MANUAL", onClick = { captureMode = "MANUAL" })
                        Text("Manual Tap")
                    }

                    // --- SLIDER DELAY (Hanya muncul jika AUTO) ---
                    AnimatedVisibility(
                        visible = captureMode == "AUTO",
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp).background(KubikBg, RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Interval Delay", style = MaterialTheme.typography.labelMedium)
                                Text("${autoDelay.roundToInt()} Seconds", fontWeight = FontWeight.Bold, color = KubikBlue)
                            }
                            Slider(
                                value = autoDelay,
                                onValueChange = { autoDelay = it },
                                valueRange = 2f..10f,
                                steps = 7
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // C. COUNTDOWN
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Start Countdown", fontWeight = FontWeight.Bold)
                        Text("${timerDuration.roundToInt()} Seconds", fontWeight = FontWeight.Bold, color = KubikBlue)
                    }
                    Slider(value = timerDuration, onValueChange = { timerDuration = it }, valueRange = 3f..10f, steps = 6)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ACTION BUTTONS (SAVE)
            Button(
                onClick = {
                    scope.launch {
                        settingsManager.saveSessionSettings(photoCount, timerDuration.roundToInt(), captureMode, autoDelay.roundToInt())
                        settingsManager.saveActiveEvent(activeEvent)
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KubikSuccess),
                modifier = Modifier.height(60.dp).fillMaxWidth(0.8f),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("SAVE CONFIGURATION", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TOMBOL EXIT KIOSK / MAINTENANCE ---
            Button(
                onClick = { onExitKiosk() },
                colors = ButtonDefaults.buttonColors(containerColor = KubikError.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth(0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Warning, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXIT KIOSK MODE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        color = KubikBlue,
        letterSpacing = 1.sp
    )
}