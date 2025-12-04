package com.thomasalfa.photobooth.presentation.screens.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.presentation.components.BouncyButton
import com.thomasalfa.photobooth.ui.theme.*
import java.io.File

@Composable
fun FrameSelectionScreen(
    frames: List<FrameEntity>,
    onFrameSelected: (FrameEntity) -> Unit,
    onBack: () -> Unit
) {
    var selectedFrame by remember { mutableStateOf<FrameEntity?>(null) }

    // Auto Select frame pertama
    LaunchedEffect(frames) {
        if (frames.isNotEmpty() && selectedFrame == null) {
            selectedFrame = frames.first()
        }
    }

    // Filter Logic (Simple & Cepat)
    val categories = remember(frames) {
        listOf("All") + frames.map { it.category }.distinct().filter { it != "Default" } + listOf("Default")
    }.distinct()
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredFrames = remember(selectedCategory, frames) {
        if (selectedCategory == "All") frames else frames.filter { it.category == selectedCategory }
    }

    // --- LAYOUT BERSIH (Tanpa Box Bertumpuk) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream) // Background Bersih
            .padding(24.dp) // Padding luar secukupnya
    ) {

        // 1. COMPACT HEADER (Judul + Kategori di satu baris = Hemat Tempat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("PICK A FRAME", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = NeoPurple)
                Text("${filteredFrames.size} designs available", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }

            // Kategori Tabs (Simple Pills)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val containerColor by animateColorAsState(if (isSelected) NeoPurple else Color.White, label = "bg")
                    val contentColor by animateColorAsState(if (isSelected) Color.White else NeoBlack, label = "txt")

                    Surface(
                        color = containerColor,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { selectedCategory = cat }
                            .border(1.dp, if(isSelected) NeoPurple else Color.LightGray, RoundedCornerShape(50))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(cat, color = contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. FRAME GALLERY (MAXIMIZED HEIGHT)
        // Kita gunakan weight(1f) agar mengambil SELURUH sisa ruang vertical.
        // Frame akan terlihat SANGAT BESAR.
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (filteredFrames.isEmpty()) {
                Text("No frames found.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp), // Padding samping dikit aja
                    modifier = Modifier.fillMaxHeight() // PENTING: Isi tinggi penuh
                ) {
                    items(filteredFrames) { frame ->
                        val isSelected = selectedFrame?.id == frame.id

                        // Animasi Ukuran: Yang dipilih sedikit lebih besar
                        val scaleHeight by animateDpAsState(targetValue = if (isSelected) 1f.dp else 0.9f.dp, label = "h") // Dummy calc for trigger

                        // ITEM FRAME (Simple Card)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxHeight(if (isSelected) 1f else 0.85f) // Tinggi Dinamis (Penuh vs 85%)
                                .aspectRatio(2f / 3f) // Rasio Kertas Foto Portrait
                                .clickable { selectedFrame = frame }
                        ) {
                            // Container Gambar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = if (isSelected) 16.dp else 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = NeoPurple
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(
                                        width = if (isSelected) 4.dp else 0.dp,
                                        color = if (isSelected) NeoGreen else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(frame.imagePath))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit, // Pastikan frame utuh terlihat
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )

                                // Checkmark Icon (Pojok Kanan Atas)
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeoGreen,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .size(32.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Nama Frame (Hanya muncul text simple di bawah)
                            Text(
                                text = frame.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if(isSelected) NeoBlack else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. ACTION FOOTER (Simple Row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tombol Back (Kiri)
            TextButton(onClick = onBack) {
                Text("BACK", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            // Tombol Start (Kanan)
            BouncyButton(
                text = "START SESSION",
                onClick = { selectedFrame?.let { onFrameSelected(it) } },
                enabled = selectedFrame != null,
                color = NeoGreen,
                textColor = Color.White,
                modifier = Modifier.width(250.dp) // Ukuran pas, tidak lebay
            )
        }
    }
}