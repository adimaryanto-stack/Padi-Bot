package com.example.padibot.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.GeoPoint
import com.example.padibot.theme.*
import com.example.padibot.ui.components.FieldMapCanvas
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun CreateFieldScreen(
    viewModel: PadiBotViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val devLoc by viewModel.deviceLocation.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    var fieldName by remember { mutableStateOf("") }
    val points = remember { mutableStateListOf<GeoPoint>() }
    val walkedTrail = remember { mutableStateListOf<GeoPoint>() }

    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isWalkAndMapActive by remember { mutableStateOf(false) }

    // GPS Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.startGpsTracking()
        } else {
            errorMessage = "Izin lokasi diperlukan untuk merekam batas sawah secara akurat."
        }
    }

    // Auto-request location permissions & start updates on enter
    LaunchedEffect(Unit) {
        viewModel.checkGpsPermissions()
        if (!viewModel.deviceLocationService.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startGpsTracking()
        }
    }

    // Auto-record trail in Walk & Map mode
    LaunchedEffect(devLoc.latitude, devLoc.longitude, isWalkAndMapActive) {
        if (isWalkAndMapActive && devLoc.hasFix && devLoc.latitude != 0.0) {
            val currentPt = GeoPoint(devLoc.latitude, devLoc.longitude)
            val lastPt = walkedTrail.lastOrNull()
            if (lastPt == null || PolygonMath.distanceBetweenMeters(lastPt, currentPt) >= 2.0) {
                walkedTrail.add(currentPt)
            }
        }
    }

    val (area, perimeter) = remember(points.toList()) {
        PolygonMath.calculateAreaAndPerimeter(points)
    }

    // Current active reference location (real phone GPS preferred, fallback to telemetry)
    val activeGpsPoint = remember(devLoc, telemetry) {
        if (devLoc.hasFix && devLoc.latitude != 0.0) {
            GeoPoint(devLoc.latitude, devLoc.longitude)
        } else if (telemetry.latitude != 0.0) {
            GeoPoint(telemetry.latitude, telemetry.longitude)
        } else {
            GeoPoint(-6.923450, 107.610150)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Field Name Input
        item {
            OutlinedTextField(
                value = fieldName,
                onValueChange = {
                    fieldName = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_field_name"),
                label = { Text("Nama Sawah *") },
                placeholder = { Text("Contoh: Sawah Blok Barat") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // GPS Permission Warning Banner if missing
        if (!devLoc.hasPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Izin Akses Lokasi Diperlukan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aplikasi membutuhkan izin GPS perangkat untuk mendeteksi posisi Anda saat memetakan sudut sawah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Aktifkan Izin GPS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // GPS Hardware Disabled Banner
        if (devLoc.hasPermission && !devLoc.isGpsEnabled) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛰️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS HP Sedang Nonaktif",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aktifkan Lokasi / GPS di pengaturan HP Anda agar titik pemetaan akurat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buka Pengaturan Lokasi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Real-Time GPS Sensor Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (devLoc.hasFix) Green700 else WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Lokasi GPS Terkini",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Accuracy & Source Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (devLoc.hasFix && devLoc.accuracyMeters <= 10f) Green100 else Color(0xFFFEF3C7),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            val accText = if (devLoc.hasFix) {
                                "±${String.format("%.1f", devLoc.accuracyMeters)}m"
                            } else {
                                "Mencari Sinyal..."
                            }
                            Text(
                                text = "Akurasi: $accText",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (devLoc.hasFix && devLoc.accuracyMeters <= 10f) Green800 else Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Coordinates Text
                    val coordText = if (devLoc.hasFix) {
                        "Koordinat HP: ${String.format("%.6f", devLoc.latitude)}, ${String.format("%.6f", devLoc.longitude)}"
                    } else if (telemetry.latitude != 0.0) {
                        "Koordinat: ${String.format("%.6f", telemetry.latitude)}, ${String.format("%.6f", telemetry.longitude)} (Simulator)"
                    } else {
                        "Koordinat: Sedang menghubungkan ke GPS satelit..."
                    }

                    Text(
                        text = coordText,
                        style = CoordinateFont,
                        fontSize = 12.sp,
                        color = Gray800
                    )

                    // Provider & Satellite Info
                    if (devLoc.hasFix) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🛰️ Sumber: ${devLoc.provider}" + if (devLoc.satellitesCount > 0) " (${devLoc.satellitesUsed}/${devLoc.satellitesCount} Satelit)" else "",
                                fontSize = 11.sp,
                                color = Gray600
                            )
                            if (devLoc.speedMps > 0.2f) {
                                Text(
                                    text = "Kecepatan: ${String.format("%.1f", devLoc.speedMps * 3.6f)} km/j",
                                    fontSize = 11.sp,
                                    color = Gray600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons: + Titik GPS & Walk & Map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentLoc = if (devLoc.hasFix && devLoc.latitude != 0.0) {
                                    GeoPoint(devLoc.latitude, devLoc.longitude)
                                } else {
                                    activeGpsPoint
                                }

                                // Check if user already added an identical point
                                if (points.isNotEmpty()) {
                                    val dist = PolygonMath.distanceBetweenMeters(points.last(), currentLoc)
                                    if (dist < 1.0) {
                                        infoMessage = "Titik berada di lokasi yang sama (< 1.0m). Melangkahlah ke sudut sawah berikutnya untuk titik baru."
                                    } else {
                                        infoMessage = "Titik ${points.size + 1} berhasil ditambahkan (${String.format("%.1f", dist)}m dari titik sebelumnya)"
                                    }
                                } else {
                                    infoMessage = "Titik 1 berhasil ditambahkan."
                                }

                                points.add(currentLoc)
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Titik GPS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isWalkAndMapActive = !isWalkAndMapActive
                                if (isWalkAndMapActive) {
                                    walkedTrail.clear()
                                    if (devLoc.hasFix) {
                                        walkedTrail.add(GeoPoint(devLoc.latitude, devLoc.longitude))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isWalkAndMapActive) Green100 else Color.Transparent,
                                contentColor = if (isWalkAndMapActive) Green800 else Gray800
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isWalkAndMapActive) Icons.Default.DirectionsWalk else Icons.Default.Timeline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isWalkAndMapActive) Green800 else Gray800
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isWalkAndMapActive) "Berjalan..." else "Walk & Map",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isWalkAndMapActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Green50,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚶‍♂️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mode Walk & Map Aktif",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Green900
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Berjalanlah mengitari pematang sawah. Jejak langkah Anda direkam dan tampil garis biru di peta. Tekan '+ Titik GPS' di setiap sudut pematang.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Green800
                                )
                                if (walkedTrail.size >= 2) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Jarak terekam: ${walkedTrail.size * 2} m • ${walkedTrail.size} titik jejak",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Green700
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preset Templates Row (Centered around current active GPS location)
        item {
            Text(
                text = "Preset Bentuk Sawah Cepat (Di sekitar lokasi saat ini):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        val base = activeGpsPoint
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(base.latitude + 0.00015, base.longitude - 0.00015),
                                GeoPoint(base.latitude + 0.00015, base.longitude + 0.00020),
                                GeoPoint(base.latitude - 0.00015, base.longitude + 0.00020),
                                GeoPoint(base.latitude - 0.00015, base.longitude - 0.00015)
                            )
                        )
                        infoMessage = "Preset Persegi Panjang diterapkan di lokasi GPS Anda."
                        errorMessage = null
                    },
                    label = { Text("Persegi Panjang") }
                )
                AssistChip(
                    onClick = {
                        val base = activeGpsPoint
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(base.latitude + 0.00018, base.longitude - 0.00010),
                                GeoPoint(base.latitude + 0.00015, base.longitude + 0.00025),
                                GeoPoint(base.latitude - 0.00018, base.longitude + 0.00018),
                                GeoPoint(base.latitude - 0.00015, base.longitude - 0.00020)
                            )
                        )
                        infoMessage = "Preset Trapesium diterapkan di lokasi GPS Anda."
                        errorMessage = null
                    },
                    label = { Text("Trapesium") }
                )
                AssistChip(
                    onClick = {
                        val base = activeGpsPoint
                        points.clear()
                        points.addAll(
                            listOf(
                                GeoPoint(base.latitude + 0.00020, base.longitude - 0.00010),
                                GeoPoint(base.latitude + 0.00015, base.longitude + 0.00022),
                                GeoPoint(base.latitude - 0.00005, base.longitude + 0.00025),
                                GeoPoint(base.latitude - 0.00020, base.longitude + 0.00005),
                                GeoPoint(base.latitude - 0.00015, base.longitude - 0.00020)
                            )
                        )
                        infoMessage = "Preset Poligon 5 Titik diterapkan di lokasi GPS Anda."
                        errorMessage = null
                    },
                    label = { Text("Poligon 5 Titik") }
                )
            }
        }

        // Interactive Map Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Peta Batas Sawah (${points.size} Titik)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ketuk peta untuk tambah titik",
                            style = MaterialTheme.typography.bodySmall,
                            color = Green700
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FieldMapCanvas(
                        points = points,
                        currentGpsLocation = if (devLoc.hasFix) devLoc.toGeoPoint() else activeGpsPoint,
                        walkedTrail = walkedTrail,
                        gpsAccuracyMeters = devLoc.accuracyMeters,
                        isInteractive = true,
                        onPointAdded = { newPt ->
                            points.add(newPt)
                            infoMessage = "Titik ditambahkan via sentuhan peta."
                        },
                        modifier = Modifier.height(210.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Area and Perimeter Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Green50)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Estimasi Luas", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = if (area >= 10000) String.format("%.2f ha (%.0f m²)", area / 10000.0, area) else String.format("%.0f m²", area),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green800
                            )
                        }
                        Column {
                            Text("Keliling", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = String.format("%.1f m", perimeter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gray900
                            )
                        }
                    }
                }
            }
        }

        // Info Notification
        if (infoMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ℹ️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = infoMessage!!,
                            color = Color(0xFF1E40AF),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = "⚠️ $errorMessage",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Coordinate Points List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Koordinat Batas (${points.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = {
                    val currentLoc = activeGpsPoint
                    manualLat = String.format("%.6f", currentLoc.latitude)
                    manualLon = String.format("%.6f", currentLoc.longitude)
                    showManualDialog = true
                }) {
                    Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Input Koordinat")
                }
            }
        }

        if (points.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada titik batas. Tekan '+ Titik GPS', ketuk peta, atau gunakan preset.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }
                }
            }
        }

        itemsIndexed(points) { index, pt ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Titik ${index + 1}: ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = pt.formatDisplay(),
                            style = CoordinateFont
                        )
                    }

                    IconButton(
                        onClick = {
                            points.removeAt(index)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Titik",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3-Button Bottom Mapping Action Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Titik (+ GPS Point)
                    OutlinedButton(
                        onClick = {
                            val cur = if (devLoc.hasFix && devLoc.latitude != 0.0) {
                                GeoPoint(devLoc.latitude, devLoc.longitude)
                            } else {
                                activeGpsPoint
                            }
                            points.add(cur)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍", fontSize = 16.sp)
                            Text("Titik", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 2. Hapus (Delete last point)
                    OutlinedButton(
                        onClick = {
                            if (points.isNotEmpty()) {
                                points.removeAt(points.size - 1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗑️", fontSize = 16.sp)
                            Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. Selesai (Save)
                    Button(
                        onClick = {
                            if (fieldName.isBlank()) {
                                fieldName = "Sawah #${System.currentTimeMillis() % 1000}"
                            }
                            if (points.size < 3) {
                                errorMessage = "Minimal 3 titik batas sawah diperlukan untuk membentuk poligon!"
                                return@Button
                            }
                            viewModel.createField(fieldName, points.toList()) {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💾", fontSize = 16.sp)
                            Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog Input Koordinat Manual / GPS Auto-Fill
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Tambah Titik Koordinat", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val currentLoc = activeGpsPoint
                            manualLat = String.format("%.6f", currentLoc.latitude)
                            manualLon = String.format("%.6f", currentLoc.longitude)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📍 Isi Otomatis dari GPS HP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude (Lintang)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualLon,
                        onValueChange = { manualLon = it },
                        label = { Text("Longitude (Bujur)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = manualLat.replace(',', '.').toDoubleOrNull()
                        val lon = manualLon.replace(',', '.').toDoubleOrNull()
                        if (lat != null && lon != null) {
                            points.add(GeoPoint(lat, lon))
                            showManualDialog = false
                        } else {
                            errorMessage = "Format koordinat tidak valid."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showManualDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
