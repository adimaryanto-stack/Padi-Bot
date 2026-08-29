package com.example.padibot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.RoutePattern
import com.example.padibot.theme.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun PlantingSettingsScreen(
    viewModel: PadiBotViewModel,
    onNavigateToPreview: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val allFields by viewModel.allFields.collectAsState()
    val selectedField by viewModel.selectedField.collectAsState()
    val selectedPattern by viewModel.selectedPattern.collectAsState()
    val machineWidth by viewModel.machineWidth.collectAsState()
    val headlandWidth by viewModel.headlandWidth.collectAsState()
    val laneOrientation by viewModel.laneOrientation.collectAsState()
    val generatedRoute by viewModel.generatedRoute.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator
        item {
            StepIndicatorHeader(currentStep = 1)
        }

        // 1. Interactive Field Selector
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌾 Pilih Sawah yang Dikerjakan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allFields.size} Tersedia",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray600
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allFields) { field ->
                        val isSelected = field.id == selectedField?.id
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Green50 else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green700) else androidx.compose.foundation.BorderStroke(1.dp, Gray200),
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { viewModel.selectField(field) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = field.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Green900 else Gray900
                                    )
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Green700
                                        ) {
                                            Text(
                                                text = "✓ Aktif",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Luas: ${field.formatArea()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray700
                                )
                                Text(
                                    text = "${field.boundary.size} Titik Batas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray600
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Route Patterns Selection (4 Options)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🛣️ Pola Jalur Tanam (Coverage Path Planning)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    RoutePattern.entries.forEach { pattern ->
                        val isSelected = pattern == selectedPattern
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Green50 else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green700) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSelectedPattern(pattern) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedPattern(pattern) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Green700)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pattern.formatLabel(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Green900 else Gray900
                                    )
                                    Text(
                                        text = pattern.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray600
                                    )
                                }
                            }
                        }
                    }

                    // Pattern Schematic Visual Diagram
                    PatternSchematicCard(pattern = selectedPattern)
                }
            }
        }

        // 3. Machine Parameters Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "⚙️ Parameter Mesin & Tanam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Machine Width
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lebar Kerja Mesin (W)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("%.2f m", machineWidth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green700
                            )
                        }
                        Slider(
                            value = machineWidth.toFloat(),
                            onValueChange = { viewModel.setMachineWidth(it.toDouble()) },
                            valueRange = 0.8f..3.0f,
                            steps = 21,
                            modifier = Modifier.testTag("slider_machine_width"),
                            colors = SliderDefaults.colors(
                                thumbColor = Green700,
                                activeTrackColor = Green700
                            )
                        )
                        Text(
                            text = "ℹ️ Standar mesin tanam padi di Indonesia: 1.20 - 2.00 meter",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    HorizontalDivider(color = Gray200)

                    // 2. Headland Width
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lebar Headland / Area Putar (H)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("%.2f m", headlandWidth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                        }
                        Slider(
                            value = headlandWidth.toFloat(),
                            onValueChange = { viewModel.setHeadlandWidth(it.toDouble()) },
                            valueRange = 1.0f..6.0f,
                            steps = 24,
                            modifier = Modifier.testTag("slider_headland_width"),
                            colors = SliderDefaults.colors(
                                thumbColor = WarningOrange,
                                activeTrackColor = WarningOrange
                            )
                        )
                        Text(
                            text = "Area perimeter pematang aman untuk manuver putar balik",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    HorizontalDivider(color = Gray200)

                    // 3. Lane Orientation Angle
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Orientasi Arah Jalur (Sudut)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("%.0f°", laneOrientation),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green700
                            )
                        }
                        Slider(
                            value = laneOrientation.toFloat(),
                            onValueChange = { viewModel.setLaneOrientation(it.toDouble()) },
                            valueRange = 0f..180f,
                            steps = 35,
                            modifier = Modifier.testTag("slider_lane_orientation"),
                            colors = SliderDefaults.colors(
                                thumbColor = Green700,
                                activeTrackColor = Green700
                            )
                        )
                        Text(
                            text = "0° = Timur-Barat (Horisontal), 90° = Utara-Selatan (Vertikal)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }
                }
            }
        }

        // Live Estimates Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Green900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kalkulasi Estimasi Otomatis",
                        style = MaterialTheme.typography.titleSmall,
                        color = Green200,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Jalur", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text(
                                text = "${generatedRoute?.totalLanes ?: 0} Baris",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text("Total Jarak", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text(
                                text = String.format("%.0f m", generatedRoute?.totalDistanceM ?: 0.0),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text("Est. Cakupan", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text(
                                text = "${generatedRoute?.coveragePct ?: 0.0}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Green400
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Buttons
        item {
            Button(
                onClick = onNavigateToPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_generate_route"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Text("Generate & Preview Jalur →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PatternSchematicCard(pattern: RoutePattern) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2D1E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📷 Skema Alur: ${pattern.title}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Green300
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Visual Diagram Representation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F1A0F),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E4D2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Green700) {
                            Text("● START (Masuk)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = ErrorRed) {
                            Text("■ END (Keluar)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text(
                        text = when (pattern) {
                            RoutePattern.BOUSTROPHEDON -> "═► ═► ═► Jalur Tanam Paralel Zig-Zag Bolak-Balik ◄═ ◄═ ◄═"
                            RoutePattern.HEADLAND_INNER -> "▓▓ [Area Tengah Terlebih Dahulu] ──► [Keliling Headland Terluar]"
                            RoutePattern.SPIRAL_INWARD -> "◎ Spiral Konsentris Inward: [Sudut Terluar] ──► [Pusat Tengah Sawah]"
                            RoutePattern.SPIRAL_OUTWARD -> "◉ Spiral Konsentris Outward: [Pusat Tengah] ──► [Batas Luar Sawah]"
                        },
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = CoordinateFont
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jalur Tanam", color = Gray400, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFD54F), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Area Putar", color = Gray400, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (pattern) {
                    RoutePattern.BOUSTROPHEDON -> "💡 Mesin masuk melalui titik Start, menanam secara bolak-balik zig-zag di area tengah, dan keluar di titik End."
                    RoutePattern.HEADLAND_INNER -> "💡 Mesin menanam lajur tengah terlebih dahulu, kemudian menuntaskan putaran keliling headland terluar."
                    RoutePattern.SPIRAL_INWARD -> "💡 Mesin masuk dari batas terluar dan berputar konsentris mengecil sampai ke titik pusat tengah."
                    RoutePattern.SPIRAL_OUTWARD -> "💡 Mesin mulai menanam dari titik tengah dan berputar membesar keluar menuju pintu keluar."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Green200
            )
        }
    }
}

@Composable
fun StepIndicatorHeader(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepPill(number = 1, title = "Pengaturan", isActive = currentStep == 1, isDone = currentStep > 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .padding(horizontal = 4.dp)
                .background(if (currentStep > 1) Green700 else Gray300)
        )
        StepPill(number = 2, title = "Preview", isActive = currentStep == 2, isDone = currentStep > 2)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .padding(horizontal = 4.dp)
                .background(if (currentStep > 2) Green700 else Gray300)
        )
        StepPill(number = 3, title = "Eksekusi", isActive = currentStep == 3, isDone = false)
    }
}

@Composable
private fun StepPill(number: Int, title: String, isActive: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = when {
                isDone -> Green700
                isActive -> Green700
                else -> Gray300
            },
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isDone) "✓" else "$number",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Green900 else Gray600
        )
    }
}
