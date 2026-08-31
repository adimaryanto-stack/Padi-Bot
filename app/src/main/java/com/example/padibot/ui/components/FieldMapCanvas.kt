package com.example.padibot.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.FieldMarker
import com.example.padibot.model.GeoPoint
import com.example.padibot.service.MapTileLayer
import com.example.padibot.service.MapTileProvider
import com.example.padibot.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FieldMapCanvas(
    points: List<GeoPoint> = emptyList(),
    boundary: List<GeoPoint> = emptyList(),
    markers: List<FieldMarker> = emptyList(),
    currentGpsLocation: GeoPoint? = null,
    walkedTrail: List<GeoPoint> = emptyList(),
    gpsAccuracyMeters: Float = 0f,
    heightDp: Int = 220,
    isInteractive: Boolean = true,
    onPointAdded: ((GeoPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedLayer by remember { mutableStateOf(MapTileLayer.SATELLITE) }
    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var showLayerMenu by remember { mutableStateOf(false) }

    // Read reactive tile state to trigger recomposition when tiles arrive
    val tileStateCount = MapTileProvider.tileStateMap.size

    val pts = if (points.isNotEmpty()) points else boundary

    // Combine all spatial references to calculate dynamic canvas viewport bounds
    val allRefPoints = buildList {
        addAll(pts)
        addAll(markers.map { it.point })
        if (currentGpsLocation != null && currentGpsLocation.latitude != 0.0) {
            add(currentGpsLocation)
        }
        addAll(walkedTrail)
    }.ifEmpty {
        listOf(GeoPoint(-6.923500, 107.610200))
    }

    val rawMinLat = allRefPoints.minOf { it.latitude }
    val rawMaxLat = allRefPoints.maxOf { it.latitude }
    val rawMinLon = allRefPoints.minOf { it.longitude }
    val rawMaxLon = allRefPoints.maxOf { it.longitude }

    // Ensure minimum span so single-point or closely clustered points don't collapse to corner
    val centerLat = (rawMinLat + rawMaxLat) / 2.0
    val centerLon = (rawMinLon + rawMaxLon) / 2.0
    val baseSpan = 0.00065 // approx ~70 meters view window
    val effectiveMinSpan = baseSpan / zoomFactor.toDouble()

    val actualLatSpan = max(rawMaxLat - rawMinLat, effectiveMinSpan)
    val actualLonSpan = max(rawMaxLon - rawMinLon, effectiveMinSpan)

    val minLat = centerLat - actualLatSpan / 2.0
    val maxLat = centerLat + actualLatSpan / 2.0
    val minLon = centerLon - actualLonSpan / 2.0
    val maxLon = centerLon + actualLonSpan / 2.0

    // Compute Mercator Zoom Level for tile rendering
    val tileZoom = when {
        actualLatSpan < 0.0004 -> 19
        actualLatSpan < 0.0008 -> 18
        actualLatSpan < 0.0016 -> 17
        actualLatSpan < 0.0032 -> 16
        else -> 15
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isInteractive && onPointAdded != null) {
                        Modifier.pointerInput(pts, markers, minLat, maxLat, minLon, maxLon) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val h = size.height
                                if (w > 0 && h > 0) {
                                    val padding = 24f
                                    val usableW = (w - padding * 2).coerceAtLeast(10f)
                                    val usableH = (h - padding * 2).coerceAtLeast(10f)

                                    val clickedLon = minLon + ((offset.x - padding) / usableW) * actualLonSpan
                                    val clickedLat = maxLat - ((offset.y - padding) / usableH) * actualLatSpan
                                    onPointAdded(GeoPoint(clickedLat, clickedLon))
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            val padding = 24f
            val usableW = (w - padding * 2).coerceAtLeast(10f)
            val usableH = (h - padding * 2).coerceAtLeast(10f)

            fun toCanvas(pt: GeoPoint): Offset {
                val px = padding + ((pt.longitude - minLon) / actualLonSpan * usableW).toFloat()
                val py = padding + ((maxLat - pt.latitude) / actualLatSpan * usableH).toFloat()
                return Offset(px, py)
            }

            // 1. BASE BACKGROUND & SATELLITE/MAP TILE RENDERING
            if (selectedLayer != MapTileLayer.HYBRID_GIS) {
                // Satellite / Map Dark background
                drawRect(color = Color(0xFF131E17))

                val startTileX = MapTileProvider.lonToTileX(minLon, tileZoom).toInt()
                val endTileX = MapTileProvider.lonToTileX(maxLon, tileZoom).toInt()
                val startTileY = MapTileProvider.latToTileY(maxLat, tileZoom).toInt()
                val endTileY = MapTileProvider.latToTileY(minLat, tileZoom).toInt()

                for (tx in (startTileX - 1)..(endTileX + 1)) {
                    for (ty in (startTileY - 1)..(endTileY + 1)) {
                        val tileBmp = MapTileProvider.getTile(selectedLayer, tileZoom, tx, ty)

                        val tileNwLon = MapTileProvider.tileXToLon(tx.toDouble(), tileZoom)
                        val tileNwLat = MapTileProvider.tileYToLat(ty.toDouble(), tileZoom)
                        val tileSeLon = MapTileProvider.tileXToLon((tx + 1).toDouble(), tileZoom)
                        val tileSeLat = MapTileProvider.tileYToLat((ty + 1).toDouble(), tileZoom)

                        val topLeft = toCanvas(GeoPoint(tileNwLat, tileNwLon))
                        val bottomRight = toCanvas(GeoPoint(tileSeLat, tileSeLon))

                        val dstW = (bottomRight.x - topLeft.x).roundToInt()
                        val dstH = (bottomRight.y - topLeft.y).roundToInt()

                        if (tileBmp != null && dstW > 0 && dstH > 0) {
                            drawImage(
                                image = tileBmp,
                                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                                dstSize = IntSize(dstW, dstH)
                            )
                        }
                    }
                }
            } else {
                // Agronomi GIS Green Canvas Background
                drawRect(color = Color(0xFF17291A))
            }

            // Dynamic GIS Grid lines (subtle overlay)
            val gridStep = 36f
            var gx = 0f
            while (gx < w) {
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = 0f
            while (gy < h) {
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // 2. Walked Trail Path
            if (walkedTrail.size >= 2) {
                val trailScreen = walkedTrail.map { toCanvas(it) }
                val trailPath = Path().apply {
                    moveTo(trailScreen[0].x, trailScreen[0].y)
                    for (i in 1 until trailScreen.size) {
                        lineTo(trailScreen[i].x, trailScreen[i].y)
                    }
                }
                drawPath(
                    path = trailPath,
                    color = Color(0xFF38BDF8),
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )
            }

            // 3. Draw Sawah Boundary Polygon
            if (pts.isNotEmpty()) {
                val screenPoints = pts.map { toCanvas(it) }

                if (screenPoints.size >= 3) {
                    val polyPath = Path().apply {
                        moveTo(screenPoints[0].x, screenPoints[0].y)
                        for (i in 1 until screenPoints.size) {
                            lineTo(screenPoints[i].x, screenPoints[i].y)
                        }
                        close()
                    }
                    // Sawah Interior Fill with vibrant semi-transparent green
                    drawPath(path = polyPath, color = Color(0x5516A34A))
                    // Boundary Border Line
                    drawPath(path = polyPath, color = Color(0xFF22C55E), style = Stroke(width = 4f))
                } else if (screenPoints.size == 2) {
                    drawLine(
                        color = Color(0xFF22C55E),
                        start = screenPoints[0],
                        end = screenPoints[1],
                        strokeWidth = 4f
                    )
                }

                // Draw Boundary Corner Pins
                screenPoints.forEachIndexed { index, offset ->
                    drawCircle(
                        color = if (index == 0) Color(0xFFF59E0B) else Color.White,
                        radius = 8.5f,
                        center = offset
                    )
                    drawCircle(
                        color = if (index == 0) Color(0xFFB45309) else Color(0xFF15803D),
                        radius = 5f,
                        center = offset
                    )
                }
            }

            // 4. Draw Markers
            if (markers.isNotEmpty()) {
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val labelPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 19f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                markers.forEach { marker ->
                    val markerPos = toCanvas(marker.point)
                    val markerColor = Color(marker.type.colorHex)

                    drawCircle(
                        color = markerColor.copy(alpha = 0.35f),
                        radius = 20f,
                        center = markerPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 13f,
                        center = markerPos
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 11f,
                        center = markerPos
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        marker.type.code,
                        markerPos.x,
                        markerPos.y + 7.5f,
                        textPaint
                    )

                    val labelText = if (marker.note.isNotBlank()) marker.note else marker.type.title
                    val textWidth = labelPaint.measureText(labelText)
                    val pillW = textWidth + 18f
                    val pillH = 26f
                    val pillTop = markerPos.y - 32f
                    val pillLeft = markerPos.x - pillW / 2f

                    drawRoundRect(
                        color = Color(0xEE111827),
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = markerColor,
                        topLeft = Offset(pillLeft, pillTop),
                        size = Size(pillW, pillH),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 1.5f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        markerPos.x,
                        pillTop + 18f,
                        labelPaint
                    )
                }
            }

            // 5. Real-time GPS Position Indicator
            if (currentGpsLocation != null && currentGpsLocation.latitude != 0.0) {
                val gpsScreenPos = toCanvas(currentGpsLocation)
                val approxMetersToPixels = (usableW / (actualLonSpan * 111320.0)).toFloat()
                val pixelRadius = if (gpsAccuracyMeters > 0) {
                    (gpsAccuracyMeters * approxMetersToPixels).coerceIn(12f, 70f)
                } else 16f

                drawCircle(
                    color = Color(0x333B82F6),
                    radius = pixelRadius,
                    center = gpsScreenPos
                )
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = 9f,
                    center = gpsScreenPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = gpsScreenPos
                )
            }
        }

        // Top-Right Layer Switcher Floating Button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xCC111827),
                onClick = {
                    selectedLayer = when (selectedLayer) {
                        MapTileLayer.SATELLITE -> MapTileLayer.STREET
                        MapTileLayer.STREET -> MapTileLayer.HYBRID_GIS
                        MapTileLayer.HYBRID_GIS -> MapTileLayer.SATELLITE
                        else -> MapTileLayer.SATELLITE
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(selectedLayer.icon, fontSize = 12.sp)
                    Text(
                        text = selectedLayer.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom-Right Zoom & Recenter Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xDD1E293B),
                onClick = { zoomFactor = (zoomFactor * 1.3f).coerceAtMost(5.0f) },
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color(0xDD1E293B),
                onClick = { zoomFactor = (zoomFactor / 1.3f).coerceAtLeast(0.4f) },
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color(0xDD1E293B),
                onClick = { zoomFactor = 1.0f },
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Reset Zoom",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
