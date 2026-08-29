package com.example.padibot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Field
import com.example.padibot.theme.*
import com.example.padibot.ui.components.FieldMapCanvas
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun FieldListScreen(
    viewModel: PadiBotViewModel,
    onNavigateToCreateField: () -> Unit,
    onNavigateToPlantingSettings: () -> Unit
) {
    val allFields by viewModel.allFields.collectAsState()
    val selectedField by viewModel.selectedField.collectAsState()
    var fieldToDelete by remember { mutableStateOf<Field?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateField,
                containerColor = Green700,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                text = { Text("Tambah Sawah", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_field")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = Green700, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Manajemen Petak Sawah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${allFields.size} Petak Sawah Terdaftar • Siap Eksekusi Tanam", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        }
                    }
                }
            }

            if (allFields.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌾", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum Ada Petak Sawah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tekan tombol 'Tambah Sawah' untuk membuat peta batas baru.", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        }
                    }
                }
            } else {
                items(allFields) { field ->
                    val isSelected = field.id == selectedField?.id
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Green50 else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green700) else androidx.compose.foundation.BorderStroke(1.dp, Gray200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectField(field) }
                            .testTag("field_card_${field.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = field.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Green900 else Gray900
                                    )
                                    Text(
                                        text = "📐 Luas: ${field.formatArea()} • ${field.boundary.size} Titik Batas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray600
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Green700,
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            Text(
                                                text = "Aktif",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    IconButton(onClick = { fieldToDelete = field }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = ErrorRed)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Mini GIS Map Preview
                            FieldMapCanvas(
                                boundary = field.boundary,
                                heightDp = 120
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectField(field)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isSelected) "✓ Terpilih" else "Pilih Sawah")
                                }

                                Button(
                                    onClick = {
                                        viewModel.selectField(field)
                                        onNavigateToPlantingSettings()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                                ) {
                                    Text("Rencanakan Tanam →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (fieldToDelete != null) {
        val field = fieldToDelete!!
        AlertDialog(
            onDismissRequest = { fieldToDelete = null },
            title = { Text("Hapus Sawah", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus '${field.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteField(field.id)
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fieldToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
