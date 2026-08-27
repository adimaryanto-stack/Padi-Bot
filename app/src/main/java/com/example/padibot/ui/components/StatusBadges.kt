package com.example.padibot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.ConnectionType
import com.example.padibot.model.GpsStatus
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*

@Composable
fun MissionStatusBadge(status: MissionStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        MissionStatus.DRAFT -> Pair(Color(0xFFEEEEEE), Color(0xFF616161))
        MissionStatus.READY -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        MissionStatus.RUNNING -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        MissionStatus.PAUSED -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        MissionStatus.COMPLETED -> Pair(Color(0xFFDCEDC8), Color(0xFF33691E))
        MissionStatus.STOPPED -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        MissionStatus.ERROR -> Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.label.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConnectionBadge(
    connectionType: ConnectionType,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val (dotColor, text) = if (isConnected) {
        Pair(SuccessGreen, when (connectionType) {
            ConnectionType.SIMULATOR -> "Simulator Aktif"
            ConnectionType.WIFI -> "WiFi ESP Terhubung"
            ConnectionType.BLUETOOTH -> "Bluetooth Terhubung"
            ConnectionType.GSM_MQTT -> "GSM 4G Terhubung"
        })
    } else {
        Pair(ErrorRed, "Terputus")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = " $text",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GpsStatusBadge(gpsStatus: GpsStatus, accuracyM: Float, modifier: Modifier = Modifier) {
    val isGood = gpsStatus != GpsStatus.NONE && accuracyM <= 2.5f
    val (bgColor, textColor) = if (isGood) {
        Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
    } else {
        Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${gpsStatus.label} (±${String.format("%.1f", accuracyM)}m)",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
