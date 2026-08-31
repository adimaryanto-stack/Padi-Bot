package com.example.padibot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.BatteryLog
import com.example.padibot.model.Telemetry
import com.example.padibot.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class BatteryTimeRange(val label: String, val durationMs: Long) {
    LAST_HOUR("1 Jam", 3600_000L),
    LAST_6_HOURS("6 Jam", 6 * 3600_000L),
    LAST_24_HOURS("24 Jam", 24 * 3600_000L),
    ALL("Semua", Long.MAX_VALUE)
}

/**
 * Interactive Battery Life History Chart and Progress Bar component.
 * Visualizes time-series battery drain, voltage fluctuation, and power draw
 * fetched from the Room SQLite database.
 */
@Composable
fun BatteryHistoryChartCard(
    batteryLogs: List<BatteryLog>,
    currentTelemetry: Telemetry,
    onRecordSample: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(BatteryTimeRange.ALL) }
    var selectedLogIndex by remember { mutableStateOf<Int?>(null) }
    var showRawTable by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val filteredLogs = remember(batteryLogs, selectedRange) {
        if (selectedRange == BatteryTimeRange.ALL) {
            batteryLogs
        } else {
            val cutoff = now - selectedRange.durationMs
            batteryLogs.filter { it.timestamp >= cutoff }
        }
    }

    // Battery statistics calculated from database logs
    val stats = remember(filteredLogs, currentTelemetry) {
        if (filteredLogs.isEmpty()) {
            BatteryHistoryStats(
                startPct = currentTelemetry.batteryPct,
                endPct = currentTelemetry.batteryPct,
                minPct = currentTelemetry.batteryPct,
                maxPct = currentTelemetry.batteryPct,
                avgVoltage = currentTelemetry.batteryVoltageV,
                peakPowerWatts = currentTelemetry.powerDrawWatts,
                totalRecordedHours = 0.0,
                drainRatePerHour = 0f
            )
        } else {
            val start = filteredLogs.first()
            val end = filteredLogs.last()
            val minPct = filteredLogs.minOf { it.batteryPct }
            val maxPct = filteredLogs.maxOf { it.batteryPct }
            val avgV = filteredLogs.map { it.batteryVoltageV }.average().toFloat()
            val peakW = filteredLogs.maxOf { it.powerDrawWatts }
            val durationHours = ((end.timestamp - start.timestamp).coerceAtLeast(60_000L)) / 3600_000.0
            val dropPct = (start.batteryPct - end.batteryPct).coerceAtLeast(0f)
            val drainRate = if (durationHours > 0.05) (dropPct / durationHours).toFloat() else 0f

            BatteryHistoryStats(
                startPct = start.batteryPct,
                endPct = end.batteryPct,
                minPct = minPct,
                maxPct = maxPct,
                avgVoltage = avgV,
                peakPowerWatts = peakW,
                totalRecordedHours = durationHours,
                drainRatePerHour = drainRate
            )
        }
    }

    val selectedLog = selectedLogIndex?.let { idx ->
        if (idx in filteredLogs.indices) filteredLogs[idx] else null
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_history_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, Icon, and Database Source Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Green700.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Green700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Riwayat Daya & Konsumsi Baterai",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gray900
                        )
                        Text(
                            text = "Tersimpan di SQLite Database (${filteredLogs.size} titik data)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray600
                        )
                    }
                }

                // SQLite Storage Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Gray100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Green700,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SQLite Local",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray800
                        )
                    }
                }
            }

            // Time Range Selector Tabs (1 Jam, 6 Jam, 24 Jam, Semua)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Gray100)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BatteryTimeRange.values().forEach { range ->
                    val isSelected = selectedRange == range
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        onClick = {
                            selectedRange = range
                            selectedLogIndex = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Green800 else Gray600
                            )
                        }
                    }
                }
            }

            // Interactive Recharts-style Area & Line Canvas Chart
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Gray100.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Tooltip / Scrubber Info Header
                    if (selectedLog != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Green700,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = selectedLog.formattedTime,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedLog.isPlantingActive) "• Beban Tanam" else "• Siaga",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedLog.batteryPct.toInt()}%",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${String.format("%.1f", selectedLog.batteryVoltageV)}V",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${selectedLog.powerDrawWatts.toInt()}W",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kurva Penurunan Daya (% Baterai vs Waktu)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray700
                            )
                            Text(
                                text = "Sentuh grafik untuk detail",
                                fontSize = 10.sp,
                                color = Gray500
                            )
                        }
                    }

                    // Canvas Chart Implementation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        BatteryTimeSeriesCanvas(
                            logs = filteredLogs,
                            selectedIndex = selectedLogIndex,
                            onSelectIndex = { selectedLogIndex = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // X-Axis Time Labels
                    if (filteredLogs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = filteredLogs.first().formattedShortTime,
                                fontSize = 9.sp,
                                color = Gray500
                            )
                            if (filteredLogs.size > 2) {
                                val mid = filteredLogs[filteredLogs.size / 2]
                                Text(
                                    text = mid.formattedShortTime,
                                    fontSize = 9.sp,
                                    color = Gray500
                                )
                            }
                            Text(
                                text = filteredLogs.last().formattedShortTime,
                                fontSize = 9.sp,
                                color = Gray500
                            )
                        }
                    }
                }
            }

            // Multi-Progress Bar Visualizers
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Indikator Beban & Kapasitas Baterai",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gray800
                )

                // 1. Current Battery Level Progress Bar with Animated Gradient
                BatteryLinearProgressBar(
                    title = "Kapasitas Tersedia Saat Ini",
                    valueText = "${currentTelemetry.batteryPct.toInt()}% (${String.format("%.1f", currentTelemetry.batteryVoltageV)} V)",
                    progress = currentTelemetry.batteryPct / 100f,
                    primaryColor = when {
                        currentTelemetry.batteryPct <= 10f -> ErrorRed
                        currentTelemetry.batteryPct <= 20f -> WarningOrange
                        else -> Green700
                    },
                    subText = "Kapasitas sisa mampu mengcover ±${String.format("%.2f", currentTelemetry.estimatedRemainingPlantableHa)} Ha sawah"
                )

                // 2. Discharge Load Progress Bar
                val loadPct = (currentTelemetry.powerDrawWatts / 1200f).coerceIn(0f, 1f)
                BatteryLinearProgressBar(
                    title = "Beban Daya Motor Transplanter (Load)",
                    valueText = "${currentTelemetry.powerDrawWatts.toInt()} W / Max 1200W",
                    progress = loadPct,
                    primaryColor = if (loadPct > 0.7f) WarningOrange else InfoBlue,
                    subText = if (currentTelemetry.isPlantingActive) "Motor penanam dan roda aktif bekerja" else "Mode siaga (daya standby komputer & RTK)"
                )

                // 3. Battery Health (SOH) Progress Bar
                BatteryLinearProgressBar(
                    title = "Kesehatan Sel Baterai (State of Health - SOH)",
                    valueText = "${currentTelemetry.batteryHealthPct}% (Sangat Baik)",
                    progress = currentTelemetry.batteryHealthPct / 100f,
                    primaryColor = Green800,
                    subText = "Sel LiFePO4 Grade-A • Degradasi 0.02% per 100 siklus"
                )
            }

            // Statistical Metrics Grid (Min/Max, Peak Watts, Laju Pengurangan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    label = "Laju Penurunan",
                    value = "${String.format("%.1f", stats.drainRatePerHour)}%/jam",
                    icon = Icons.Default.TrendingDown,
                    tint = if (stats.drainRatePerHour > 20f) WarningOrange else Green700,
                    modifier = Modifier.weight(1f)
                )

                StatChip(
                    label = "Daya Puncak",
                    value = "${stats.peakPowerWatts.toInt()} W",
                    icon = Icons.Default.FlashOn,
                    tint = WarningOrange,
                    modifier = Modifier.weight(1f)
                )

                StatChip(
                    label = "Tegangan Rata²",
                    value = "${String.format("%.1f", stats.avgVoltage)} V",
                    icon = Icons.Default.Speed,
                    tint = InfoBlue,
                    modifier = Modifier.weight(1f)
                )

                StatChip(
                    label = "Total Sampel",
                    value = "${filteredLogs.size} log",
                    icon = Icons.Default.Dataset,
                    tint = Gray700,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action Toolbar (Record Instant Log, View Table, Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showRawTable = !showRawTable },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (showRawTable) Icons.Default.VisibilityOff else Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Green700
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showRawTable) "Tutup Tabel Log" else "Lihat Tabel Log SQLite",
                        fontSize = 11.sp,
                        color = Green700,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onRecordSample,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Catat Sampel", fontSize = 11.sp)
                    }

                    if (batteryLogs.isNotEmpty()) {
                        IconButton(
                            onClick = onClearLogs,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Bersihkan Log",
                                tint = Gray500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Raw SQLite Table Drawer
            AnimatedVisibility(visible = showRawTable) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gray100)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("WAKTU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700, modifier = Modifier.weight(1.2f))
                        Text("LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700, modifier = Modifier.weight(0.8f))
                        Text("VOLT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700, modifier = Modifier.weight(0.8f))
                        Text("DAYA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700, modifier = Modifier.weight(0.8f))
                        Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Gray300)

                    val displayLogs = filteredLogs.takeLast(10).reversed()
                    if (displayLogs.isEmpty()) {
                        Text("Belum ada data log dalam rentang ini.", fontSize = 11.sp, color = Gray600)
                    } else {
                        displayLogs.forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.formattedTime, fontSize = 10.sp, color = Gray800, modifier = Modifier.weight(1.2f))
                                Text("${log.batteryPct.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Green800, modifier = Modifier.weight(0.8f))
                                Text("${String.format("%.1f", log.batteryVoltageV)}V", fontSize = 10.sp, color = Gray800, modifier = Modifier.weight(0.8f))
                                Text("${log.powerDrawWatts.toInt()}W", fontSize = 10.sp, color = Gray800, modifier = Modifier.weight(0.8f))
                                Text(
                                    text = if (log.isPlantingActive) "Tanam" else if (log.isCharging) "Charge" else "Siaga",
                                    fontSize = 9.sp,
                                    color = if (log.isPlantingActive) Green700 else Gray600,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas Chart with Area Gradient, Smooth Curve, Grid Lines, and Touch Scrubbing
 */
@Composable
private fun BatteryTimeSeriesCanvas(
    logs: List<BatteryLog>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Menunggu pembacaan sensor baterai...", fontSize = 12.sp, color = Gray500)
        }
        return
    }

    Canvas(
        modifier = modifier
            .pointerInput(logs) {
                detectTapGestures(
                    onTap = { offset ->
                        val count = logs.size
                        if (count > 0) {
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            val idx = (ratio * (count - 1)).roundToInt()
                            onSelectIndex(idx)
                        }
                    }
                )
            }
            .pointerInput(logs) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val count = logs.size
                        if (count > 0) {
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            val idx = (ratio * (count - 1)).roundToInt()
                            onSelectIndex(idx)
                        }
                    },
                    onDragEnd = { /* keep selected or clear */ }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32f
        val paddingBottom = 20f
        val paddingTop = 10f
        val paddingRight = 10f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Draw horizontal guideline thresholds (100%, 50%, 20%, 0%)
        val levels = listOf(100f, 50f, 20f, 0f)
        levels.forEach { level ->
            val y = paddingTop + chartHeight * (1f - (level / 100f))
            drawLine(
                color = when (level) {
                    20f -> WarningOrange.copy(alpha = 0.35f)
                    0f -> ErrorRed.copy(alpha = 0.35f)
                    else -> Gray300
                },
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (level == 20f || level == 50f) PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) else null
            )
        }

        // Generate Chart Coordinates
        val count = logs.size
        val points = logs.mapIndexed { index, log ->
            val x = if (count == 1) {
                paddingLeft + chartWidth / 2f
            } else {
                paddingLeft + (index.toFloat() / (count - 1)) * chartWidth
            }
            val y = paddingTop + chartHeight * (1f - (log.batteryPct.coerceIn(0f, 100f) / 100f))
            Offset(x, y)
        }

        // Draw Area Fill under the curve
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                points.forEach { pt ->
                    lineTo(pt.x, pt.y)
                }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Green700.copy(alpha = 0.35f),
                        Green400.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )

            // Draw Line Chart
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = linePath,
                color = Green700,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else if (points.size == 1) {
            drawCircle(
                color = Green700,
                radius = 6.dp.toPx(),
                center = points.first()
            )
        }

        // Draw Selected Scrubbing Indicator
        if (selectedIndex != null && selectedIndex in points.indices) {
            val activePoint = points[selectedIndex]
            // Vertical scrubber guide line
            drawLine(
                color = Green800,
                start = Offset(activePoint.x, paddingTop),
                end = Offset(activePoint.x, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            // Outer glowing ring
            drawCircle(
                color = Green700.copy(alpha = 0.25f),
                radius = 10.dp.toPx(),
                center = activePoint
            )
            // Inner solid dot
            drawCircle(
                color = Green800,
                radius = 5.dp.toPx(),
                center = activePoint
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = activePoint
            )
        }
    }
}

/**
 * Elegant single-metric progress bar with header and descriptive subtext
 */
@Composable
private fun BatteryLinearProgressBar(
    title: String,
    valueText: String,
    progress: Float,
    primaryColor: Color,
    subText: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progressBarAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Gray100.copy(alpha = 0.7f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray800
            )
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )
        }

        // Linear Track Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Gray200)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.8f),
                                primaryColor
                            )
                        )
                    )
            )
        }

        Text(
            text = subText,
            fontSize = 9.5.sp,
            color = Gray600
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Gray100,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 8.5.sp,
                color = Gray600,
                maxLines = 1
            )
        }
    }
}

private data class BatteryHistoryStats(
    val startPct: Float,
    val endPct: Float,
    val minPct: Float,
    val maxPct: Float,
    val avgVoltage: Float,
    val peakPowerWatts: Float,
    val totalRecordedHours: Double,
    val drainRatePerHour: Float
)
