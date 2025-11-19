package com.thomasalfa.photobooth.presentation.screens.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thomasalfa.photobooth.presentation.components.BouncyButton
import com.thomasalfa.photobooth.ui.theme.*
import java.io.File

@Composable
fun SelectionScreen(
    allPhotos: List<String>, // Menerima 8 Foto
    onSelectionComplete: (List<String>) -> Unit // Mengirim 6 Foto Terpilih
) {
    val context = LocalContext.current

    // State: Foto mana saja yang dipilih user
    // Kita inisialisasi dengan memilih 6 foto pertama secara default
    val selectedPhotos = remember { mutableStateListOf<String>().apply {
        addAll(allPhotos.take(6))
    }}

    val targetCount = 6
    val isComplete = selectedPhotos.size == targetCount

    // Fungsi toggle selection
    fun toggleSelection(path: String) {
        if (selectedPhotos.contains(path)) {
            selectedPhotos.remove(path)
        } else {
            if (selectedPhotos.size < targetCount) {
                selectedPhotos.add(path)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            Text(
                text = "PICK YOUR BEST!",
                style = MaterialTheme.typography.headlineLarge,
                color = NeoBlack,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Indikator Jumlah (Berubah warna kalau sudah pas)
            Text(
                text = "${selectedPhotos.size} / $targetCount Selected",
                style = MaterialTheme.typography.titleLarge,
                color = if (isComplete) NeoGreen else NeoPink,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- GRID FOTO (4 Kolom x 2 Baris) ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(allPhotos) { path ->
                    val isSelected = selectedPhotos.contains(path)

                    // Item Foto
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 5.dp else 0.dp,
                                color = if (isSelected) NeoGreen else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { toggleSelection(path) }
                    ) {
                        // Gambar
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(path)).build(),
                            contentDescription = "Candidate",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                // Efek dim (gelap) kalau tidak dipilih, biar fokus ke yang dipilih
                                .drawForegroundLayer(if (!isSelected) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                        )

                        // Icon Centang Besar
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeoGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TOMBOL CONFIRM ---
            BouncyButton(
                text = if (isComplete) "CONFIRM SELECTION" else "PICK ${targetCount - selectedPhotos.size} MORE",
                onClick = {
                    if (isComplete) {
                        onSelectionComplete(selectedPhotos.toList())
                    }
                },
                enabled = isComplete,
                color = if (isComplete) NeoGreen else Color.Gray,
                textColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.5f) // Lebar tombol setengah layar
                    .height(70.dp)
            )
        }
    }
}

// Helper extension biar kodingan rapi
fun Modifier.drawForegroundLayer(color: Color): Modifier = this.then(
    Modifier.background(color) // Ini trick simple overlay, atau bisa pakai drawWithContent
)