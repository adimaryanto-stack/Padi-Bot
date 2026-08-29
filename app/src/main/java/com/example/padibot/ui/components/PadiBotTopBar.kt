package com.example.padibot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.ConnectionType
import com.example.padibot.theme.Green700
import com.example.padibot.theme.Green800
import com.example.padibot.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadiBotTopBar(
    title: String,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    connectionType: ConnectionType,
    isConnected: Boolean
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isConnected) SuccessGreen else MaterialTheme.colorScheme.error)
                    )
                    Text(
                        text = when (connectionType) {
                            ConnectionType.SIMULATOR -> "Simulator"
                            ConnectionType.WIFI -> "WiFi"
                            ConnectionType.BLUETOOTH -> "BT"
                            ConnectionType.GSM_MQTT -> "GSM"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
