package com.thomasalfa.photobooth.presentation.screens.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.thomasalfa.photobooth.data.database.AppDatabase
import com.thomasalfa.photobooth.data.database.FrameEntity
import com.thomasalfa.photobooth.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val framesList by db.frameDao().getAllFrames().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(NeoCream).padding(24.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FRAME LIBRARY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("BACK")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (framesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No frames yet. Add one!", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(framesList) { frame ->
                        FrameItemCard(frame, onDelete = {
                            scope.launch(Dispatchers.IO) { db.frameDao().deleteFrame(frame) }
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeoPurple),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD NEW FRAME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showAddDialog) {
            // Ambil daftar kategori unik yang sudah ada di database untuk saran dropdown
            val existingCategories = framesList.map { it.category }.distinct().filter { it != "Default" }

            AddFrameDialog(
                isLoading = isSaving,
                existingCategories = existingCategories,
                onDismiss = { if (!isSaving) showAddDialog = false },
                onSave = { name, category, layout, uri ->
                    isSaving = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val fileName = "frame_${System.currentTimeMillis()}.png"
                                val file = File(context.filesDir, fileName)
                                inputStream?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }

                                val newFrame = FrameEntity(
                                    displayName = name,
                                    category = category,
                                    layoutType = layout,
                                    imagePath = file.absolutePath
                                )
                                db.frameDao().insertFrame(newFrame)
                            }
                            Toast.makeText(context, "Frame Saved!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FrameItemCard(frame: FrameEntity, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(File(frame.imagePath)), contentDescription = null,
                modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).background(Color.LightGray), contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(frame.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row {
                    Badge(frame.layoutType, NeoYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Warna badge beda untuk Default vs Custom
                    Badge(frame.category, if(frame.category == "Default") Color.LightGray else NeoPink)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFrameDialog(
    isLoading: Boolean,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Uri) -> Unit
) {
    var name by remember { mutableStateOf("") }

    // --- LOGIC KATEGORI BARU ---
    // Bisa pilih "Default" atau ketik custom category
    var categoryInput by remember { mutableStateOf("Default") }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val defaultOptions = listOf("Default") + existingCategories

    var selectedLayout by remember { mutableStateOf("GRID") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Upload New Frame", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Frame Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                Spacer(modifier = Modifier.height(12.dp))

                // CATEGORY INPUT (Editable Dropdown)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it }, // User bisa ketik manual
                        label = { Text("Event Category (e.g. Wedding A)") },
                        trailingIcon = { IconButton(onClick = { isCategoryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = isCategoryExpanded, onDismissRequest = { isCategoryExpanded = false }) {
                        defaultOptions.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { categoryInput = cat; isCategoryExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Layout Type:", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedLayout == "GRID", onClick = { selectedLayout = "GRID" })
                    Text("Grid")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = selectedLayout == "STRIP", onClick = { selectedLayout = "STRIP" })
                    Text("Strip")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { launcher.launch("image/png") }, colors = ButtonDefaults.buttonColors(containerColor = if(selectedImageUri != null) NeoGreen else NeoYellow), modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedImageUri != null) "IMAGE SELECTED ✅" else "SELECT PNG IMAGE", color = NeoBlack)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotEmpty() && selectedImageUri != null) onSave(name, categoryInput, selectedLayout, selectedImageUri!!) },
                        enabled = !isLoading && name.isNotEmpty() && selectedImageUri != null
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) else Text("Save Frame")
                    }
                }
            }
        }
    }
}