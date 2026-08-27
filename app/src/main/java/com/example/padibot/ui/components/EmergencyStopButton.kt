package com.example.padibot.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.theme.EmergencyAccentRed
import com.example.padibot.theme.EmergencyDarkRed

@Composable
fun EmergencyStopButton(
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier,
    isTriggered: Boolean = false
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "EmergencyPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderGlow"
    )

    Button(
        onClick = {
            // Instant trigger or quick confirm dialog
            onEmergencyStop()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = EmergencyDarkRed)
            .testTag("emergency_stop_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = EmergencyDarkRed,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            3.dp,
            EmergencyAccentRed.copy(alpha = borderAlpha)
        )
    ) {
        Text(
            text = "⛔  BERHENTI DARURAT (E-STOP)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "⚠️ Konfirmasi Berhenti Darurat?",
                    fontWeight = FontWeight.Bold,
                    color = EmergencyDarkRed
                )
            },
            text = {
                Text("Mesin tanam padi akan langsung mematikan motor penggerak dan berhenti di posisi saat ini.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onEmergencyStop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyDarkRed)
                ) {
                    Text("YA, BERHENTI SEKARANG", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
