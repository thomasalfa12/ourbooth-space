package com.thomasalfa.photobooth.presentation.screens.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.ui.theme.KubikBlack
import com.thomasalfa.photobooth.ui.theme.KubikBlue
import com.thomasalfa.photobooth.utils.SupabaseManager
import kotlinx.coroutines.launch

@Composable
fun TicketScreen(
    deviceId: String,
    onTicketValid: () -> Unit,
    onBack: () -> Unit
    // HAPUS parameter viewModel karena kita pakai logic langsung agar lebih simpel
) {
    // State UI
    var inputCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Logic Validasi
    fun submitCode() {
        if (inputCode.length != 6) return

        scope.launch {
            isLoading = true
            errorMessage = null

            // Memanggil fungsi redeemTicket yang sudah kita buat di SupabaseManager
            val isValid = SupabaseManager.redeemTicket(inputCode, deviceId)

            if (isValid) {
                onTicketValid()
            } else {
                errorMessage = "Invalid or Expired Token!"
                inputCode = "" // Reset jika salah
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // Back Button (Updated to AutoMirrored to fix warning)
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = KubikBlack
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ENTER TICKET CODE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = KubikBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Please input the 6-digit code from the cashier",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Display Code (Kotak-kotak angka)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(80.dp)
            ) {
                for (i in 0 until 6) {
                    val char = if (i < inputCode.length) inputCode[i].toString() else ""

                    // Logic Border: Highlight jika kotak ini sedang aktif/terisi
                    val isActive = i == inputCode.length

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            // Fix error border disini:
                            .then(
                                if (isActive)
                                    Modifier.border(2.dp, KubikBlue, RoundedCornerShape(12.dp))
                                else
                                    Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubikBlack
                        )
                    }
                }
            }

            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Numeric Keypad
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { key ->
                            KeypadButton(
                                text = key,
                                onClick = {
                                    if (key == "DEL") {
                                        if (inputCode.isNotEmpty()) inputCode = inputCode.dropLast(1)
                                    } else if (key.isNotEmpty()) {
                                        if (inputCode.length < 6) {
                                            inputCode += key
                                            // Auto submit jika sudah 6 digit
                                            if (inputCode.length == 6) submitCode()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KubikBlue)
            }
        }
    }
}

@Composable
fun KeypadButton(text: String, onClick: () -> Unit) {
    if (text.isEmpty()) {
        Box(modifier = Modifier.size(80.dp)) // Spacer placeholder
        return
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (text == "DEL") Color(0xFFFFEBEE) else Color(0xFFF5F5F5))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "DEL") {
            // Updated to AutoMirrored to fix warning
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Delete",
                tint = Color.Red
            )
        } else {
            Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = KubikBlack)
        }
    }
}