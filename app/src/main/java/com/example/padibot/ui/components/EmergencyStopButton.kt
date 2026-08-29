package com.example.padibot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.theme.EmergencyAccentRed
import com.example.padibot.theme.EmergencyDarkRed

@Composable
fun EmergencyStopButton(
    onEmergencyStop: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Button(
        onClick = { showConfirmDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("button_emergency_stop"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EmergencyDarkRed,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Berhenti Darurat",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "⚠️ BERHENTI DARURAT (E-STOP)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Konfirmasi Berhenti Darurat", fontWeight = FontWeight.Bold) },
            text = { Text("Mesin akan segera menghentikan semua motor traksi dan mekanisme tanam secara instan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onEmergencyStop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyDarkRed)
                ) {
                    Text("AKTIFKAN E-STOP")
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
