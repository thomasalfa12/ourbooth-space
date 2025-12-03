package com.thomasalfa.photobooth.presentation.screens.result

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.LocalShareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QrCodeScreen(
    url: String, // URL SUDAH PASTI ADA
    onFinish: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    // Generate QR Image (Cepat, Local process)
    LaunchedEffect(url) {
        scope.launch {
            qrBitmap = LocalShareManager.generateQrCode(url)
        }
    }

    // Auto Close 60s
    LaunchedEffect(Unit) {
        delay(60000)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // ... (Sisa UI Card sama persis dengan yang Anda kirim, tidak perlu ubah)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Box {
                IconButton(onClick = onFinish, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp).fillMaxWidth()) {
                    Text("YOUR MEMORIES ARE READY!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = NeoPurple)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scan to download & share instantly", color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))

                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR",
                            modifier = Modifier.size(350.dp).border(4.dp, NeoBlack, RoundedCornerShape(16.dp)).padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = NeoPink)
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = NeoGreen), modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("I'M DONE - START NEW SESSION", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}