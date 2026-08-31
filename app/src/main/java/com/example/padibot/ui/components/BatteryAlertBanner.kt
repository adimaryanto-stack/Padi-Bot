package com.example.padibot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Telemetry
import com.example.padibot.theme.*

/**
 * Prominent visual low-battery alert banner for Dashboard and Mission screens.
 * Triggers automatically whenever the robot's battery level <= 20%.
 */
@Composable
fun BatteryAlertBanner(
    telemetry: Telemetry,
    onPauseMission: (() -> Unit)? = null,
    onOpenBatteryMonitor: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val batteryPct = telemetry.batteryPct
    val isLow = batteryPct <= 20f
    val isCritical = batteryPct <= 10f

    // Flashing pulse animation for urgency
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCritical) 450 else 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AnimatedVisibility(
        visible = isLow,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val bannerBg = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
        val borderColor = if (isCritical) ErrorRed else WarningOrange
        val textColor = if (isCritical) ErrorRed else Color(0xFFB45309)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bannerBg),
            border = BorderStroke(1.5.dp, borderColor.copy(alpha = pulseAlpha)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("visual_battery_alert_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with Pulsing Warning Badge & Battery Percentage
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
                            color = borderColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCritical) Icons.Default.BatteryAlert else Icons.Default.Warning,
                                    contentDescription = "Alert Icon",
                                    tint = borderColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .alpha(pulseAlpha)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (isCritical) "🚨 PERINGATAN: BATERAI KRITIS!" else "⚠️ PERINGATAN BATERAI RENDAH",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                            Text(
                                text = if (isCritical) "Level daya di bawah 10% — Segera amankan robot!" else "Level daya ≤ 20% (${telemetry.estimatedRemainingTimeString} tersisa)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray700
                            )
                        }
                    }

                    // Bold Percentage Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = borderColor,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "${batteryPct.toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Advisory message
                Text(
                    text = if (isCritical) {
                        "Daya baterai hampir habis. Motor traksi dan mekanisme tanam dapat terhenti otomatis untuk melindungi sel LiFePO4 dari over-discharge."
                    } else {
                        "Kapasitas baterai tersisa ${batteryPct.toInt()}%. Diselesaikan baris petak tanam ini dan arahkan robot ke home point / base untuk pengisian daya atau swap pack baterai."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray800,
                    lineHeight = 16.sp
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onPauseMission != null && telemetry.isPlantingActive) {
                        Button(
                            onClick = onPauseMission,
                            colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jeda Operasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onOpenBatteryMonitor != null) {
                        OutlinedButton(
                            onClick = onOpenBatteryMonitor,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, borderColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ElectricMeter, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detail Daya", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Pill indicator for TopBar when battery is low
 */
@Composable
fun LowBatteryTopBarPill(
    batteryPct: Float,
    onClick: () -> Unit = {}
) {
    val isCritical = batteryPct <= 10f
    val infiniteTransition = rememberInfiniteTransition(label = "topBarPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillAlpha"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCritical) ErrorRed.copy(alpha = alpha) else WarningOrange.copy(alpha = alpha),
        modifier = Modifier
            .clickable { onClick() }
            .padding(end = 6.dp)
            .testTag("topbar_low_battery_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = "Low Battery",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${batteryPct.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
