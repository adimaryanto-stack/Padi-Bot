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
import com.example.padibot.model.FieldMarker
import com.example.padibot.model.GeoPoint
import com.example.padibot.model.MarkerType
import com.example.padibot.service.GpsPrecisionGrade
import com.example.padibot.theme.*
import com.example.padibot.ui.components.FieldMapCanvas
import com.example.padibot.ui.components.LocationPermissionCard
import com.example.padibot.ui.components.rememberLocationPermissionState
import com.example.padibot.viewmodel.PadiBotViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CreateFieldScreen(
    viewModel: PadiBotViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val devLoc by viewModel.deviceLocation.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()

    var fieldName by remember { mutableStateOf("") }
    val points = remember { mutableStateListOf<GeoPoint>() }
    val markers = remember { mutableStateListOf<FieldMarker>() }
    val walkedTrail = remember { mutableStateListOf<GeoPoint>() }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Batas Poligon, 1: Penanda Irigasi & Tanam
    var selectedMarkerType by remember { mutableStateOf(MarkerType.IRRIGATION_INLET) }
    var markerNote by remember { mutableStateOf("") }

    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var showAveragingDialog by remember { mutableStateOf(false) }
    var isSamplingAveraging by remember { mutableStateOf(false) }
    var averagingProgress by remember { mutableStateOf(0f) }
    var averagingStats by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isWalkAndMapActive by remember { mutableStateOf(false) }

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

    // GPS Runtime Permission State Helper
    val locationPermState = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.startGpsTracking()
            errorMessage = null
        } else {
            errorMessage = "Izin lokasi diperlukan untuk merekam batas sawah secara akurat."
        }
    }

    // Auto-request location permissions & start updates on enter
    LaunchedEffect(Unit) {
        viewModel.checkGpsPermissions()
        if (!locationPermState.isFullyGranted) {
            locationPermState.request()
        } else {
            viewModel.startGpsTracking()
        }

        // Initialize with default visible field polygon around active GPS location if empty
        if (points.isEmpty()) {
            val base = activeGpsPoint
            points.addAll(
                listOf(
                    GeoPoint(base.latitude + 0.00015, base.longitude - 0.00015),
                    GeoPoint(base.latitude + 0.00015, base.longitude + 0.00020),
                    GeoPoint(base.latitude - 0.00015, base.longitude + 0.00020),
                    GeoPoint(base.latitude - 0.00015, base.longitude - 0.00015)
                )
            )
        }
    }

    // Auto-record trail in Walk & Map mode
    LaunchedEffect(devLoc.latitude, devLoc.longitude, isWalkAndMapActive) {
        if (isWalkAndMapActive && devLoc.hasFix && devLoc.latitude != 0.0) {
            val currentPt = GeoPoint(devLoc.latitude, devLoc.longitude)
            val lastPt = walkedTrail.lastOrNull()
            if (lastPt == null || PolygonMath.distanceBetweenMeters(lastPt, currentPt) >= 1.5) {
                walkedTrail.add(currentPt)
            }
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
        if (!locationPermState.isFullyGranted) {
            item {
                LocationPermissionCard(
                    state = locationPermState,
                    title = "Izin GPS Presisi Tinggi Diperlukan",
                    description = "PadiBot memerlukan izin lokasi akurat (Fine Location & GNSS) untuk mendeteksi posisi titik sudut batas sawah Anda saat memetakan lahan."
                )
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

        // Real-Time GPS Precision Sensor Card
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
                                tint = if (devLoc.hasFix) Color(devLoc.precisionGrade.colorHex) else WarningOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Lokasi & Presisi GPS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = devLoc.precisionGrade.label,
                                    fontSize = 11.sp,
                                    color = Color(devLoc.precisionGrade.colorHex),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Accuracy Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (devLoc.hasFix && devLoc.accuracyMeters <= 5f) Green100 else Color(0xFFFEF3C7),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            val accText = if (devLoc.hasFix) {
                                "±${String.format(Locale.US, "%.1f", devLoc.accuracyMeters)}m (Kalman: ±${String.format(Locale.US, "%.1f", devLoc.filteredAccuracyMeters)}m)"
                            } else {
                                "Mencari Sinyal..."
                            }
                            Text(
                                text = accText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (devLoc.hasFix && devLoc.accuracyMeters <= 5f) Green800 else Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Coordinates Text with 7 decimal resolution (~1cm)
                    val coordText = if (devLoc.hasFix) {
                        "Koordinat HP: ${String.format(Locale.US, "%.7f", devLoc.latitude)}, ${String.format(Locale.US, "%.7f", devLoc.longitude)}"
                    } else if (telemetry.latitude != 0.0) {
                        "Koordinat: ${String.format(Locale.US, "%.7f", telemetry.latitude)}, ${String.format(Locale.US, "%.7f", telemetry.longitude)} (Simulator)"
                    } else {
                        "Koordinat: Sedang menghubungkan ke GPS satelit..."
                    }

                    Text(
                        text = coordText,
                        style = CoordinateFont,
                        fontSize = 12.sp,
                        color = Gray800
                    )

                    // Provider & Satellite Info & Sampling Buffer
                    if (devLoc.hasFix) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🛰️ ${devLoc.provider}" + if (devLoc.satellitesCount > 0) " (${devLoc.satellitesUsed}/${devLoc.satellitesCount} Satelit)" else "",
                                fontSize = 11.sp,
                                color = Gray600
                            )
                            Text(
                                text = "Buffer Sampel: ${devLoc.bufferedSamplesCount}/30",
                                fontSize = 11.sp,
                                color = Green800,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action Buttons: + Titik Cepat & 🎯 Titik Presisi Tinggi (Averaging) & Walk & Map
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Instant Add
                        Button(
                            onClick = {
                                val currentLoc = if (devLoc.hasFix && devLoc.latitude != 0.0) {
                                    GeoPoint(devLoc.latitude, devLoc.longitude)
                                } else {
                                    activeGpsPoint
                                }

                                if (points.isNotEmpty()) {
                                    val dist = PolygonMath.distanceBetweenMeters(points.last(), currentLoc)
                                    if (dist < 1.0) {
                                        infoMessage = "Titik berada sangat dekat (< 1.0m). Melangkahlah ke sudut sawah berikutnya."
                                    } else {
                                        infoMessage = "Titik ${points.size + 1} ditambahkan (${String.format(Locale.US, "%.1f", dist)}m dari titik sebelumnya)"
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Titik", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. High-Precision Averaging Button
                        FilledTonalButton(
                            onClick = {
                                showAveragingDialog = true
                                isSamplingAveraging = true
                                averagingProgress = 0f
                                coroutineScope.launch {
                                    for (i in 1..10) {
                                        delay(200)
                                        averagingProgress = i / 10f
                                    }
                                    val avgRes = viewModel.deviceLocationService.computeAveragedHighPrecisionPoint(maxSamples = 15)
                                    if (avgRes != null) {
                                        averagingStats = "Averaged ${avgRes.sampleCount} sampel • Deviasi: ±${String.format(Locale.US, "%.2f", avgRes.standardDeviationMeters)}m • Estimasi Error: ±${String.format(Locale.US, "%.2f", avgRes.estimatedAccuracyMeters)}m"
                                        points.add(avgRes.point)
                                        infoMessage = "Titik ${points.size} (Presisi Tinggi ±${String.format(Locale.US, "%.2f", avgRes.estimatedAccuracyMeters)}m) berhasil dikunci!"
                                    } else {
                                        points.add(activeGpsPoint)
                                    }
                                    isSamplingAveraging = false
                                    delay(600)
                                    showAveragingDialog = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("🎯 Presisi Tinggi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Walk & Map
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
                                modifier = Modifier.size(16.dp),
                                tint = if (isWalkAndMapActive) Green800 else Gray800
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isWalkAndMapActive) "Berjalan" else "Walk",
                                fontSize = 12.sp,
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
                                    text = "Berjalanlah mengitari pematang sawah. Jejak langkah terfilter Kalman dan tampil garis biru di peta. Tekan '🎯 Presisi Tinggi' di setiap sudut pematang.",
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

        // Mode Switcher: Batas Poligon vs Penanda Irigasi & Tanam
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Green800,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📍", fontSize = 14.sp)
                            Text("Batas Sawah (${points.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🚩", fontSize = 14.sp)
                            Text("Penanda Titik (${markers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
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
                            text = if (activeTab == 0) "Peta Batas Sawah (${points.size} Titik)" else "Peta Penanda (${markers.size} Penanda)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeTab == 0) "Ketuk peta untuk tambah titik" else "Ketuk peta untuk taruh ${selectedMarkerType.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeTab == 0) Green700 else Color(selectedMarkerType.colorHex),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FieldMapCanvas(
                        points = points,
                        markers = markers.toList(),
                        currentGpsLocation = if (devLoc.hasFix) devLoc.toGeoPoint() else activeGpsPoint,
                        walkedTrail = walkedTrail,
                        gpsAccuracyMeters = devLoc.filteredAccuracyMeters.takeIf { it > 0f } ?: devLoc.accuracyMeters,
                        isInteractive = true,
                        onPointAdded = { newPt ->
                            if (activeTab == 0) {
                                points.add(newPt)
                                infoMessage = "Titik batas ke-${points.size} ditambahkan via sentuhan peta."
                            } else {
                                val newMarker = FieldMarker(
                                    type = selectedMarkerType,
                                    point = newPt,
                                    note = markerNote.trim()
                                )
                                markers.add(newMarker)
                                infoMessage = "${selectedMarkerType.emoji} Penanda ${selectedMarkerType.title} ditambahkan di peta."
                                markerNote = ""
                            }
                        },
                        modifier = Modifier.height(220.dp)
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
                                text = if (area >= 10000) String.format(Locale.US, "%.2f ha (%.0f m²)", area / 10000.0, area) else String.format(Locale.US, "%.0f m²", area),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green800
                            )
                        }
                        Column {
                            Text("Keliling", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = String.format(Locale.US, "%.1f m", perimeter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gray900
                            )
                        }
                        Column {
                            Text("Penanda", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(
                                text = "${markers.size} Titik",
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

        // TAB 1: MARKER MANAGEMENT UI
        if (activeTab == 1) {
            // Marker Type Selection Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚩", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pilih Jenis Penanda Khusus",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Marker Types Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.IRRIGATION_INLET,
                                    onClick = { selectedMarkerType = MarkerType.IRRIGATION_INLET },
                                    label = { Text("💧 Inlet Irigasi", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.IRRIGATION_OUTLET,
                                    onClick = { selectedMarkerType = MarkerType.IRRIGATION_OUTLET },
                                    label = { Text("🌊 Outlet Air", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.PLANTING_START,
                                    onClick = { selectedMarkerType = MarkerType.PLANTING_START },
                                    label = { Text("🌱 Awal Tanam", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.PLANTING_FINISH,
                                    onClick = { selectedMarkerType = MarkerType.PLANTING_FINISH },
                                    label = { Text("🏁 Titik Selesai", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.WATER_PUMP,
                                    onClick = { selectedMarkerType = MarkerType.WATER_PUMP },
                                    label = { Text("🚰 Pompa Air", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedMarkerType == MarkerType.OBSTACLE,
                                    onClick = { selectedMarkerType = MarkerType.OBSTACLE },
                                    label = { Text("⚠️ Rintangan", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Description of active marker
                        Text(
                            text = selectedMarkerType.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(selectedMarkerType.colorHex),
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Note Input
                        OutlinedTextField(
                            value = markerNote,
                            onValueChange = { markerNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Catatan / Keterangan (Opsional)") },
                            placeholder = { Text("Contoh: Pintu air barat, Pipa 3 dim") },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Placement Button at GPS location
                        Button(
                            onClick = {
                                val loc = if (devLoc.hasFix && devLoc.latitude != 0.0) {
                                    GeoPoint(devLoc.latitude, devLoc.longitude)
                                } else {
                                    activeGpsPoint
                                }
                                val m = FieldMarker(
                                    type = selectedMarkerType,
                                    point = loc,
                                    note = markerNote.trim()
                                )
                                markers.add(m)
                                infoMessage = "${selectedMarkerType.emoji} Penanda ${selectedMarkerType.title} ditambahkan di lokasi GPS Anda."
                                markerNote = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(selectedMarkerType.colorHex))
                        ) {
                            Icon(imageVector = Icons.Default.AddLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Pasang Penanda di Posisi GPS HP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // List of Placed Markers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Penanda Khusus (${markers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (markers.isNotEmpty()) {
                        TextButton(onClick = { markers.clear() }) {
                            Text("Hapus Semua", color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (markers.isEmpty()) {
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
                                text = "Belum ada penanda. Sentuh peta di atas atau tekan tombol '+ Pasang Penanda' untuk menandai titik irigasi/tanam.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray600
                            )
                        }
                    }
                }
            }

            itemsIndexed(markers) { index, marker ->
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(marker.type.colorHex)
                            ) {
                                Text(
                                    text = "${marker.type.emoji} ${marker.type.code}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = marker.type.title + if (marker.note.isNotBlank()) " - ${marker.note}" else "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = String.format(Locale.US, "%.7f, %.7f", marker.point.latitude, marker.point.longitude),
                                    style = CoordinateFont
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                markers.removeAt(index)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus Penanda",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // TAB 0: BOUNDARY COORDINATE POINTS LIST
        if (activeTab == 0) {
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
                        manualLat = String.format(Locale.US, "%.7f", currentLoc.latitude)
                        manualLon = String.format(Locale.US, "%.7f", currentLoc.longitude)
                        showManualDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Input Manual")
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
                                text = "Belum ada titik batas. Tekan '+ Titik', '🎯 Presisi Tinggi', atau gunakan preset.",
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
                                text = String.format(Locale.US, "%.7f, %.7f", pt.latitude, pt.longitude),
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
                    // 1. Titik (+ GPS Point or Marker)
                    OutlinedButton(
                        onClick = {
                            val cur = if (devLoc.hasFix && devLoc.latitude != 0.0) {
                                GeoPoint(devLoc.latitude, devLoc.longitude)
                            } else {
                                activeGpsPoint
                            }
                            if (activeTab == 0) {
                                val lastPt = points.lastOrNull()
                                val pointToAdd = if (lastPt != null && PolygonMath.distanceBetweenMeters(lastPt, cur) < 1.0) {
                                    // Stationary GPS (emulator / standing still): smart corner placement
                                    val step = (points.size % 4)
                                    val deltaLat = when (step) { 0 -> 0.00015; 1 -> 0.00015; 2 -> -0.00015; else -> -0.00015 }
                                    val deltaLon = when (step) { 0 -> -0.00015; 1 -> 0.00020; 2 -> 0.00020; else -> -0.00015 }
                                    GeoPoint(cur.latitude + deltaLat, cur.longitude + deltaLon)
                                } else {
                                    cur
                                }
                                points.add(pointToAdd)
                                infoMessage = "Titik batas ke-${points.size} ditambahkan."
                            } else {
                                val m = FieldMarker(
                                    type = selectedMarkerType,
                                    point = cur,
                                    note = markerNote.trim()
                                )
                                markers.add(m)
                                infoMessage = "${selectedMarkerType.emoji} Penanda ${selectedMarkerType.title} ditambahkan."
                                markerNote = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (activeTab == 0) "📍" else selectedMarkerType.emoji, fontSize = 16.sp)
                            Text(
                                text = if (activeTab == 0) "+ Titik" else "+ ${selectedMarkerType.code}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Hapus (Delete last point or marker)
                    OutlinedButton(
                        onClick = {
                            if (activeTab == 0 && points.isNotEmpty()) {
                                points.removeAt(points.size - 1)
                            } else if (activeTab == 1 && markers.isNotEmpty()) {
                                markers.removeAt(markers.size - 1)
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
                            viewModel.createField(fieldName, points.toList(), markers.toList()) {
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

    // High Precision Averaging Sampling Dialog
    if (showAveragingDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Survei Presisi Tinggi", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mengumpulkan & merata-ratakan 15 sampel satelit GNSS untuk menghilangkan jitter dan meningkatkan akurasi titik sudut pematang...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700
                    )

                    LinearProgressIndicator(
                        progress = { averagingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Green700,
                        trackColor = Green100
                    )

                    Text(
                        text = if (isSamplingAveraging) "Sampling ${((averagingProgress * 15).toInt())}/15..." else "Selesai! Titik terkunci.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green900
                    )

                    if (averagingStats.isNotBlank()) {
                        Text(
                            text = averagingStats,
                            style = CoordinateFont,
                            fontSize = 11.sp,
                            color = Gray800
                        )
                    }
                }
            },
            confirmButton = {}
        )
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
                            manualLat = String.format(Locale.US, "%.7f", currentLoc.latitude)
                            manualLon = String.format(Locale.US, "%.7f", currentLoc.longitude)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📍 Isi dari GPS HP Terkini", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude (Lintang - 7 desimal)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualLon,
                        onValueChange = { manualLon = it },
                        label = { Text("Longitude (Bujur - 7 desimal)") },
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
