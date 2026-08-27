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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Field
import com.example.padibot.theme.ErrorRed
import com.example.padibot.theme.Green700
import com.example.padibot.ui.components.FieldMapCanvas
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun FieldListScreen(
    viewModel: PadiBotViewModel,
    onNavigateToCreateField: () -> Unit,
    onNavigateToPlantingSettings: (Field) -> Unit
) {
    val fields by viewModel.allFields.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var fieldToDelete by remember { mutableStateOf<Field?>(null) }

    val filteredFields = fields.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateField,
                containerColor = Green700,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_field")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Sawah")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_fields"),
                placeholder = { Text("Cari sawah berdasarkan nama...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredFields.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = Green700,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "Belum Ada Sawah Terdaftar" else "Sawah Tidak Ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tekan tombol + di bawah untuk memetakan sawah baru.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredFields, key = { it.id }) { field ->
                        FieldCardItem(
                            field = field,
                            onSelect = {
                                viewModel.selectField(field)
                                onNavigateToPlantingSettings(field)
                            },
                            onDelete = { fieldToDelete = field }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (fieldToDelete != null) {
        val field = fieldToDelete!!
        AlertDialog(
            onDismissRequest = { fieldToDelete = null },
            title = { Text("Hapus Sawah?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Sawah '${field.name}' akan dihapus dari sistem.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteField(field.id)
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
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

@Composable
private fun FieldCardItem(
    field: Field,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("field_card_${field.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = field.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${field.boundary.size} Titik Batas • Keliling ${field.formatPerimeter()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus Sawah",
                        tint = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mini polygon canvas preview
            FieldMapCanvas(
                points = field.boundary,
                modifier = Modifier.height(140.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Luas: ${field.formatArea()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Green700
                )

                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mulai Misi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
