package com.thomasalfa.photobooth.presentation.screens.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.ui.theme.*
import com.thomasalfa.photobooth.utils.layout.LayoutProcessor
import java.io.File

@Composable
fun EditorScreen(
    capturedPhotos: List<String>,
    selectedFrame: FrameEntity,
    onEditingComplete: (List<String>) -> Unit
) {
    // SINGLE SOURCE OF TRUTH: Urutan Foto (Termasuk cadangan)
    val currentPhotoOrder = remember { mutableStateListOf<String>().apply { addAll(capturedPhotos) } }

    // State: Slot mana di FRAME (Kiri) yang sedang dipilih user?
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }

    // Hitung Slot Frame
    val slotDefinitions = remember(selectedFrame) {
        LayoutProcessor.getSlotsForLayout(selectedFrame.layoutType)
    }

    // --- MAIN CONTAINER (SPLIT 50:50) ---
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoCream)
    ) {

        // =====================================================================
        // BAGIAN KIRI: CANVAS WORKSPACE (50%) - ACTIVE POOL
        // =====================================================================
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // 1. LOGIC SCALING (Agar frame fit di layar & tidak gepeng)
            val screenHeight = maxHeight.value
            val scale = (screenHeight * 0.98f) / LayoutProcessor.CANVAS_HEIGHT
            val displayWidth = (LayoutProcessor.CANVAS_WIDTH * scale).dp
            val displayHeight = (LayoutProcessor.CANVAS_HEIGHT * scale).dp

            // 2. WRAPPER ALIGNMENT
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 3. FRAME VISUAL CONTAINER
                Box(
                    modifier = Modifier
                        .size(displayWidth, displayHeight)
                        .shadow(24.dp, RoundedCornerShape(4.dp))
                        .background(Color.White)
                ) {
                    // A. RENDER FOTO DI SLOT (Active Slots)
                    // Gunakan 'slotIndex' agar nama variable unik
                    slotDefinitions.forEachIndexed { slotIndex, slotDef ->
                        if (slotIndex < currentPhotoOrder.size) {
                            val photoPath = currentPhotoOrder[slotIndex]
                            val isSelected = selectedSlotIndex == slotIndex

                            val x = (slotDef.x * scale).dp
                            val y = (slotDef.y * scale).dp
                            val w = (slotDef.width * scale).dp
                            val h = (slotDef.height * scale).dp

                            Box(
                                modifier = Modifier
                                    .offset(x, y)
                                    .size(w, h)
                                    .border(
                                        width = if (isSelected) 4.dp else 0.dp,
                                        color = if (isSelected) NeoGreen else Color.Transparent
                                    )
                                    .clickable {
                                        // LOGIC PILIH SLOT (KIRI)
                                        if (selectedSlotIndex == null) {
                                            selectedSlotIndex = slotIndex // Select
                                        } else {
                                            if (selectedSlotIndex == slotIndex) {
                                                selectedSlotIndex = null // Deselect
                                            } else {
                                                // SWAP INTERNAL (Kiri ke Kiri)
                                                val fromIndex = selectedSlotIndex!!
                                                val toIndex = slotIndex
                                                val temp = currentPhotoOrder[fromIndex]
                                                currentPhotoOrder[fromIndex] = currentPhotoOrder[toIndex]
                                                currentPhotoOrder[toIndex] = temp
                                                selectedSlotIndex = null
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = File(photoPath),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay Icon jika sedang dipilih
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(NeoGreen.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // B. FRAME OVERLAY IMAGE (PNG Transparan)
                    AsyncImage(
                        model = File(selectedFrame.imagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }

        // =====================================================================
        // BAGIAN KANAN: CONTROL PANEL (50%) - RESERVE POOL + FAB
        // =====================================================================
        // Gunakan BOX agar kita bisa menaruh FAB melayang di pojok
        Box(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Padding konten internal
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEADER
                Text(
                    text = "FINAL TOUCH",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeoPurple,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // INSTRUCTION CARD (Dinamis)
                val instructionText = if (selectedSlotIndex == null)
                    "Tap a photo on the LEFT to select a slot."
                else
                    "Now tap ANY photo below to swap!"

                val instructionColor = if (selectedSlotIndex == null) NeoBlack else NeoGreen

                Card(
                    colors = CardDefaults.cardColors(containerColor = if(selectedSlotIndex == null) NeoCream else NeoGreen.copy(alpha=0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            if (selectedSlotIndex != null) 2.dp else 0.dp,
                            if (selectedSlotIndex != null) NeoGreen else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if(selectedSlotIndex != null) NeoGreen else NeoPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(instructionText, fontWeight = FontWeight.Bold, color = instructionColor)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // GRID HEADER
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Original Shots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                        Text("${currentPhotoOrder.size} photos", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // GRID FOTO (RESERVE POOL)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // Padding bawah 88dp agar item paling bawah tidak tertutup tombol FAB
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // Gunakan 'photoIndex' agar tidak konflik dengan 'slotIndex'
                    itemsIndexed(currentPhotoOrder) { photoIndex, path ->
                        val isInFrame = photoIndex < slotDefinitions.size
                        val isSourceSelected = selectedSlotIndex == photoIndex

                        val borderWidth by animateDpAsState(
                            if (selectedSlotIndex != null) 4.dp else if (isSourceSelected) 4.dp else 0.dp,
                            label = "border"
                        )
                        val borderColor by animateColorAsState(
                            if (isSourceSelected) NeoGreen else if (selectedSlotIndex != null) NeoPurple.copy(alpha=0.5f) else Color.Transparent,
                            label = "color"
                        )

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    // LOGIC SWAP (INTI)
                                    if (selectedSlotIndex != null) {
                                        val frameIndex = selectedSlotIndex!! // Target (Kiri)
                                        val rawIndex = photoIndex // Source (Kanan)

                                        // Swap Data
                                        val temp = currentPhotoOrder[frameIndex]
                                        currentPhotoOrder[frameIndex] = currentPhotoOrder[rawIndex]
                                        currentPhotoOrder[rawIndex] = temp

                                        selectedSlotIndex = null // Reset
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if(isSourceSelected) 0.5f else 1f)
                            )

                            // Label Status Foto
                            if (isInFrame) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("USED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(NeoGreen)
                                )
                            }
                        }
                    }
                }
            }

            // FLOATING ACTION BUTTON (DONE)
            FloatingActionButton(
                onClick = {
                    // Hanya ambil foto yang masuk frame (sesuai jumlah slot)
                    val finalPhotos = currentPhotoOrder.take(slotDefinitions.size)
                    onEditingComplete(finalPhotos)
                },
                containerColor = NeoGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}