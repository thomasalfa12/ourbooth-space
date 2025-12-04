package com.thomasalfa.photobooth.presentation.screens.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.R
import com.thomasalfa.photobooth.data.SettingsManager
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.SupabaseManager
import kotlinx.coroutines.launch

@Composable
fun LoginActivityScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KubikBlue), // Brand Color Background
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(16.dp),
            modifier = Modifier.width(420.dp) // Card lebar untuk Tablet
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LOGO
                Image(
                    painter = painterResource(id = R.drawable.logo_kubik),
                    contentDescription = "Kubik Booth",
                    modifier = Modifier.height(70.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text("DEVICE ACTIVATION", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = KubikBlack)
                Text("Enter credentials to setup this booth", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(40.dp))

                // INPUT USERNAME
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Device Username") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KubikBlue,
                        focusedLabelColor = KubikBlue
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // INPUT PIN
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if(it.length <= 8) pin = it },
                    label = { Text("Security PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KubikBlue,
                        focusedLabelColor = KubikBlue
                    )
                )

                Spacer(modifier = Modifier.height(40.dp))

                // LOGIN BUTTON
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val device = SupabaseManager.loginDevice(username, pin)
                            if (device != null) {
                                // Save Session Locally
                                settingsManager.saveLoginSession(device.id, device.name, device.type)
                                Toast.makeText(context, "Activated: ${device.name}", Toast.LENGTH_LONG).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Authentication Failed", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KubikGold, contentColor = KubikBlack),
                    enabled = !isLoading && username.isNotEmpty() && pin.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = KubikBlack)
                    } else {
                        Text("ACTIVATE BOOTH", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }

        // Footer Version
        Text(
            text = "KubikOS v2.0 Enterprise",
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            fontSize = 12.sp
        )
    }
}