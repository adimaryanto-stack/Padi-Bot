package com.example.padibot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*

@Composable
fun GpsStatusBadge(
    gpsStatus: String,
    accuracyM: Double
) {
    val isRtk = gpsStatus.contains("RTK", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isRtk) Green100 else WarningOrange.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isRtk) SuccessGreen else WarningOrange)
            )
            Text(
                text = "$gpsStatus (±${String.format("%.2f", accuracyM)}m)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRtk) Green800 else Gray900
            )
        }
    }
}

@Composable
fun MissionStatusBadge(status: MissionStatus) {
    val (bg, fg) = when (status) {
        MissionStatus.READY -> Pair(Green100, Green800)
        MissionStatus.RUNNING -> Pair(InfoBlue.copy(alpha = 0.15f), InfoBlue)
        MissionStatus.PAUSED -> Pair(WarningOrange.copy(alpha = 0.15f), WarningOrange)
        MissionStatus.COMPLETED -> Pair(Green100, SuccessGreen)
        MissionStatus.STOPPED, MissionStatus.ERROR -> Pair(ErrorRed.copy(alpha = 0.15f), ErrorRed)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = status.label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
