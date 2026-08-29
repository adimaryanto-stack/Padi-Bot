package com.example.padibot.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.padibot.theme.Green700
import com.example.padibot.ui.navigation.Screen

@Composable
fun PadiBotBottomBar(
    currentRoute: String?,
    onNavigateTo: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { onNavigateTo(Screen.Dashboard.route) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Utama") },
            modifier = Modifier.testTag("nav_dashboard"),
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green700, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
        )
        NavigationBarItem(
            selected = currentRoute == Screen.FieldList.route,
            onClick = { onNavigateTo(Screen.FieldList.route) },
            icon = { Icon(Icons.Default.Map, contentDescription = "Peta Sawah") },
            label = { Text("Sawah") },
            modifier = Modifier.testTag("nav_fields"),
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green700, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
        )
        NavigationBarItem(
            selected = currentRoute == Screen.MissionHistory.route,
            onClick = { onNavigateTo(Screen.MissionHistory.route) },
            icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
            label = { Text("Riwayat") },
            modifier = Modifier.testTag("nav_history"),
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green700, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigateTo(Screen.Settings.route) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
            label = { Text("Alat") },
            modifier = Modifier.testTag("nav_settings"),
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green700, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
        )
    }
}
