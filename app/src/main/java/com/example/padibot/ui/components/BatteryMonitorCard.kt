package com.example.padibot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.MissionStatus
import com.example.padibot.model.Telemetry
import com.example.padibot.theme.*

/**
 * Material 3 Robot Battery and Power Monitoring Card.
 * Displays real-time battery level, estimated remaining operation time,
 * cell telemetry (voltage, power draw, temperature), and battery health status.
 */
@Composable
fun BatteryMonitorCard(
    telemetry: Telemetry,
    missionStatus: MissionStatus = MissionStatus.READY,
    onViewHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val batteryPct = telemetry.batteryPct
    val isLowBattery = batteryPct <= 20f
    val isCriticalBattery = batteryPct <= 10f
    val isPlanting = telemetry.isPlantingActive || missionStatus == MissionStatus.RUNNING

    // Animated battery fill percentage
    val animatedProgress by animateFloatAsState(
        targetValue = (batteryPct / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "batteryProgress"
    )

    // Dynamic color coding based on battery state
    val batteryColor by animateColorAsState(
        targetValue = when {
            isCriticalBattery -> ErrorRed
            isLowBattery -> WarningOrange
            batteryPct >= 60f -> Green700
            else -> WarningOrange
        },
        label = "batteryColor"
    )

    val batteryBgColor = when {
        isCriticalBattery -> Color(0xFFFEF2F2)
        isLowBattery -> Color(0xFFFFFBEB)
        else -> Color(0xFFF0FDF4)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_battery_monitor_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Battery Icon, Title, and Health/Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = batteryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    telemetry.isCharging -> Icons.Default.BatteryChargingFull
                                    batteryPct > 80f -> Icons.Default.BatteryFull
                                    batteryPct > 50f -> Icons.Default.Battery6Bar
                                    batteryPct > 25f -> Icons.Default.Battery3Bar
                                    batteryPct > 10f -> Icons.Default.Battery1Bar
                                    else -> Icons.Default.BatteryAlert
                                },
                                contentDescription = "Battery Status",
                                tint = batteryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Monitoring Daya & Baterai",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gray900
                        )
                        Text(
                            text = "LiFePO4 48V 60Ah • 2.88 kWh",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray600
                        )
                    }
                }

                // Health & Load Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        telemetry.isCharging -> InfoBlue.copy(alpha = 0.15f)
                        isPlanting -> Green700.copy(alpha = 0.15f)
                        else -> Gray200
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        telemetry.isCharging -> InfoBlue
                                        isPlanting -> Green700
                                        else -> Gray600
                                    }
                                )
                        )
                        Text(
                            text = when {
                                telemetry.isCharging -> "Pengisian Daya"
                                isPlanting -> "Beban Tanam Aktif"
                                else -> "Siaga (Standby)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                telemetry.isCharging -> InfoBlue
                                isPlanting -> Green800
                                else -> Gray700
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Highlight: Big Battery % & Estimated Remaining Time
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = batteryBgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${batteryPct.toInt()}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = batteryColor,
                                lineHeight = 36.sp
                            )
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = batteryColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "Kapasitas Tersedia",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray600
                        )
                    }

                    // Remaining operation time highlight block
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Green700,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Estimasi Sisa Operasi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Gray700
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = telemetry.estimatedRemainingTimeString,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLowBattery) ErrorRed else Green800
                            )
                            Text(
                                text = if (isPlanting) "pada beban tanam ~${telemetry.powerDrawWatts.toInt()}W" else "dalam mode siaga/standby",
                                fontSize = 10.sp,
                                color = Gray600
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Gray200)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedProgress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        batteryColor.copy(alpha = 0.8f),
                                        batteryColor
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0% (Kritis)", fontSize = 10.sp, color = Gray500)
                    Text("20% (Batas Aman)", fontSize = 10.sp, color = Gray500)
                    Text("100% (Penuh)", fontSize = 10.sp, color = Gray500)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Grid Telemetry Metric Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatteryMetricItem(
                    label = "Tegangan (V)",
                    value = "${String.format("%.1f", telemetry.batteryVoltageV)} V",
                    icon = Icons.Default.Bolt,
                    tint = WarningOrange,
                    modifier = Modifier.weight(1f)
                )

                BatteryMetricItem(
                    label = "Daya (Watt)",
                    value = "${telemetry.powerDrawWatts.toInt()} W",
                    icon = Icons.Default.ElectricMeter,
                    tint = InfoBlue,
                    modifier = Modifier.weight(1f)
                )

                BatteryMetricItem(
                    label = "Suhu Pack",
                    value = "${String.format("%.1f", telemetry.batteryTempC)}°C",
                    icon = Icons.Default.Thermostat,
                    tint = if (telemetry.batteryTempC > 40f) ErrorRed else Green700,
                    modifier = Modifier.weight(1f)
                )

                BatteryMetricItem(
                    label = "Kapasitas Lahan",
                    value = "±${String.format("%.2f", telemetry.estimatedRemainingPlantableHa)} Ha",
                    icon = Icons.Default.Grass,
                    tint = Green800,
                    modifier = Modifier.weight(1f)
                )
            }

            // Low Battery Alert / Return to Base Recommendation
            if (isLowBattery) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCriticalBattery) ErrorRed.copy(alpha = 0.12f) else WarningOrange.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isCriticalBattery) ErrorRed else WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCriticalBattery) "BATERAI KRITIS (<10%)" else "Peringatan Baterai Lemah (≤20%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCriticalBattery) ErrorRed else Color(0xFFB45309)
                            )
                            Text(
                                text = "Disarankan segera menyelesaikan petak tanam atau bawa robot ke dock swap baterai untuk menjaga keawetan sel LiFePO4.",
                                fontSize = 10.sp,
                                color = Gray700
                            )
                        }
                    }
                }
            }

            // Expandable Detailed Specs Toggle
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Sembunyikan Rincian BMS" else "Lihat Rincian Sel & Kesehatan BMS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Green700
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Green700,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = Gray200)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BatterySpecRow("Status Sel BMS", "16S Seimbang (Nominal 3.2V/sel)")
                        BatterySpecRow("Kesehatan Sel (SOH)", "${telemetry.batteryHealthPct}% (Prima)")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BatterySpecRow("Arus Beban Transplanter", "${String.format("%.1f", telemetry.batteryCurrentA)} Ampere")
                        BatterySpecRow("Estimasi Waktu Charging", "1j 45m (Fast Charger 30A)")
                    }

                    if (onViewHistory != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onViewHistory,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = Green700)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Grafik Riwayat & Log Database", fontSize = 11.sp, color = Green800, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
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
                fontSize = 9.sp,
                color = Gray600,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BatterySpecRow(
    title: String,
    value: String
) {
    Column {
        Text(title, fontSize = 10.sp, color = Gray600)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
    }
}
