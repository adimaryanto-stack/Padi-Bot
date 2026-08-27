package com.example.padibot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.GeoPoint
import com.example.padibot.theme.*
import com.example.padibot.ui.components.FieldMapCanvas
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun CreateFieldScreen(
    viewModel: PadiBotViewModel,
    onNavigateBack: () -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    val points = remember {
        mutableStateListOf(
            GeoPoint(-6.923400, 107.610000),
            GeoPoint(-6.923400, 107.610350),
            GeoPoint(-6.923700, 107.610350),
            GeoPoint(-6.923700, 107.610000)
        )
    }

    var manualLat by remember { mutableStateOf("-6.923500") }
    var manualLon by remember { mutableStateOf("107.610500") }
    var showManualDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val (area, perimeter) = remember(points.toList()) {
        PolygonMath.calculateAreaAndPerimeter(points)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = fieldName,
                onValueChange = {
                    fieldName = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_field_name"),
                label = { Text("Nama Sawah *") },
                placeholder = { Text("Contoh: Sawah Blok Barat") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Preset Templates Row
        item {
            Text(
                text = "Preset Bentuk Sawah Cepat:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(-6.923400, 107.610000),
                                GeoPoint(-6.923400, 107.610400),
                                GeoPoint(-6.923700, 107.610400),
                                GeoPoint(-6.923700, 107.610000)
                            )
                        )
                    },
                    label = { Text("Persegi Panjang") }
                )
                AssistChip(
                    onClick = {
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(-6.923300, 107.610100),
                                GeoPoint(-6.923400, 107.610500),
                                GeoPoint(-6.923750, 107.610400),
                                GeoPoint(-6.923650, 107.609900)
                            )
                        )
                    },
                    label = { Text("Trapesium") }
                )
                AssistChip(
                    onClick = {
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(-6.923200, 107.610000),
                                GeoPoint(-6.923250, 107.610350),
                                GeoPoint(-6.923500, 107.610500),
                                GeoPoint(-6.923750, 107.610250),
                                GeoPoint(-6.923600, 107.609850)
                            )
                        )
                    },
                    label = { Text("Poligon 5 Titik") }
                )
            }
        }

        // Interactive Map Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Peta Batas Sawah (${points.size} Titik)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ketuk peta untuk tambah titik",
                            style = MaterialTheme.typography.bodySmall,
                            color = Green700
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FieldMapCanvas(
                        points = points,
                        isInteractive = true,
                        onPointAdded = { newPt ->
                            points.add(newPt)
                        },
                        modifier = Modifier.height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Area and Perimeter Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Green50)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estimasi Luas", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = if (area >= 10000) String.format("%.2f ha (%.0f m²)", area / 10000.0, area) else String.format("%.0f m²", area),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green800
                            )
                        }
                        Column {
                            Text("Keliling", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = String.format("%.1f m", perimeter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gray900
                            )
                        }
                    }
                }
            }
        }

        // Coordinate Points List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Koordinat Batas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { showManualDialog = true }) {
                    Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Input Koordinat")
                }
            }
        }

        itemsIndexed(points) { index, pt ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Titik ${index + 1}: ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = pt.formatDisplay(),
                            style = CoordinateFont
                        )
                    }

                    IconButton(
                        onClick = {
                            if (points.size > 3) {
                                points.removeAt(index)
                            } else {
                                errorMessage = "Sawah membutuhkan minimal 3 titik batas."
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Titik",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = "⚠️ $errorMessage",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    if (fieldName.isBlank()) {
                        errorMessage = "Nama sawah wajib diisi!"
                        return@Button
                    }
                    if (points.size < 3) {
                        errorMessage = "Minimal 3 titik batas sawah diperlukan!"
                        return@Button
                    }
                    if (area < 20.0) {
                        errorMessage = "Luas sawah terlalu kecil. Pastikan titik membentuk poligon tertutup."
                        return@Button
                    }

                    viewModel.createField(fieldName, points.toList()) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_save_field"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Sawah", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Dialog Input Koordinat Manual
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Tambah Titik Koordinat", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude (Lintang)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualLon,
                        onValueChange = { manualLon = it },
                        label = { Text("Longitude (Bujur)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = manualLat.toDoubleOrNull()
                        val lon = manualLon.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            points.add(GeoPoint(lat, lon))
                            showManualDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
