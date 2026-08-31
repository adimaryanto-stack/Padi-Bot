package com.example.padibot.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.padibot.theme.Green700
import com.example.padibot.theme.Green800
import com.example.padibot.theme.WarningOrange

/**
 * State holder for location runtime permissions.
 */
class LocationPermissionState(
    val hasFineLocation: Boolean,
    val hasCoarseLocation: Boolean,
    val shouldShowRationale: Boolean,
    val permissionRequested: Boolean,
    private val onRequestPermission: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    val isAnyGranted: Boolean get() = hasFineLocation || hasCoarseLocation
    val isFullyGranted: Boolean get() = hasFineLocation

    fun request() {
        onRequestPermission()
    }

    fun openSettings() {
        onOpenSettings()
    }
}

/**
 * Remember and observe location permissions state in Compose UI.
 */
@Composable
fun rememberLocationPermissionState(
    onPermissionResult: ((Boolean) -> Unit)? = null
): LocationPermissionState {
    val context = LocalContext.current

    var hasFineLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCoarseLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequested by remember { mutableStateOf(false) }
    var shouldShowRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = true
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasFineLocation
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: hasCoarseLocation
        hasFineLocation = fineGranted
        hasCoarseLocation = coarseGranted
        val granted = fineGranted || coarseGranted
        shouldShowRationale = !fineGranted
        onPermissionResult?.invoke(granted)
    }

    // Refresh permission state whenever composition is active
    DisposableEffect(Unit) {
        hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        onDispose { }
    }

    return remember(hasFineLocation, hasCoarseLocation, permissionRequested, shouldShowRationale) {
        LocationPermissionState(
            hasFineLocation = hasFineLocation,
            hasCoarseLocation = hasCoarseLocation,
            shouldShowRationale = shouldShowRationale,
            permissionRequested = permissionRequested,
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )
    }
}

/**
 * An informative Card/Banner requesting Fine Location access with contextual rationale.
 */
@Composable
fun LocationPermissionCard(
    state: LocationPermissionState,
    title: String = "Izin GPS Presisi Tinggi Diperlukan",
    description: String = "PadiBot memerlukan izin lokasi akurat (Fine Location & GNSS/RTK) untuk pemetaan poligon batas sawah dan navigasi autonomous robot tanam.",
    modifier: Modifier = Modifier
) {
    if (state.isFullyGranted) return

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!state.hasFineLocation && state.hasCoarseLocation)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (state.hasCoarseLocation) WarningOrange else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.hasCoarseLocation) Icons.Default.GpsNotFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!state.hasFineLocation && state.hasCoarseLocation)
                            "Hanya Izin Lokasi Perkiraan (Coarse)"
                        else
                            title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!state.hasFineLocation && state.hasCoarseLocation)
                            "Tingkatkan ke izin 'Lokasi Akurat' di setelan untuk akurasi sub-meter."
                        else
                            description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.permissionRequested && !state.isAnyGranted) {
                    OutlinedButton(
                        onClick = { state.openSettings() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka Setelan HP", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = { state.request() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green700
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.hasCoarseLocation) "Izinkan Lokasi Akurat" else "Izinkan Lokasi GPS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Modal Rationale Dialog before requesting or after permission denial.
 */
@Composable
fun LocationPermissionRationaleDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Green700,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Izin Akses Lokasi GPS & GNSS",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Aplikasi PadiBot membutuhkan izin ACCESS_FINE_LOCATION untuk fungsi utama berikut:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• 📍 Pemetaan batas poligon sawah otomatis (Walk & Map)\n" +
                           "• 🛰️ Sinkronisasi koordinat GPS RTK sub-meter untuk mesin tanam\n" +
                           "• 🚩 Penandaan titik saluran irigasi, pompa air, dan rintangan fisik sawah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirmRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Text("Lanjutkan & Izinkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti Saja")
            }
        }
    )
}
