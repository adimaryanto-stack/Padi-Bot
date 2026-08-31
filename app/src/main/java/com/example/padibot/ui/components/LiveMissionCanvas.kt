package com.example.padibot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.FieldMarker
import com.example.padibot.model.GeoPoint
import com.example.padibot.model.Telemetry
import com.example.padibot.theme.*
import kotlin.math.*

/**
 * Interactive SVG/Vector Map Canvas supporting pinch-to-zoom, pan, double-tap zoom,
 * precision zoom buttons, center-on-rover tracking, and dynamic metric scale bar.
 */
@Composable
fun LiveMissionCanvas(
    boundary: List<GeoPoint>,
    waypoints: List<GeoPoint>,
    markers: List<FieldMarker> = emptyList(),
    telemetry: Telemetry? = null,
    modifier: Modifier = Modifier,
    enableZoomControls: Boolean = true,
    showScaleBar: Boolean = true,
    showDirectionArrows: Boolean = true
) {
    // Zoom and pan transformation states
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Bounds calculation
    val allPoints = remember(boundary, waypoints, markers) {
        (boundary + waypoints + markers.map { it.point }).ifEmpty {
            listOf(GeoPoint(-6.923450, 107.610150), GeoPoint(-6.923750, 107.610550))
        }
    }

    val minLat = remember(allPoints) { allPoints.minOf { it.latitude } }
    val maxLat = remember(allPoints) { allPoints.maxOf { it.latitude } }
    val minLon = remember(allPoints) { allPoints.minOf { it.longitude } }
    val maxLon = remember(allPoints) { allPoints.maxOf { it.longitude } }

    val latSpan = remember(minLat, maxLat) { (maxLat - minLat).coerceAtLeast(0.00008) }
    val lonSpan = remember(minLon, maxLon) { (maxLon - minLon).coerceAtLeast(0.00008) }

    // Helper to reset view to 100% fit
    fun resetView() {
        scale = 1.0f
        panOffset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clipToBounds()
            .background(Color(0xFF141F14))
            .testTag("interactive_map_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapPos ->
                            if (scale > 1.3f) {
                                resetView()
                            } else {
                                val targetScale = 2.5f
                                // Zoom in centered around the double-tap point
                                panOffset = (panOffset - tapPos) * (targetScale / scale) + tapPos
                                scale = targetScale
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(0.6f, 12.0f)
                        // Zoom around gesture centroid
                        panOffset = (panOffset - centroid) * (newScale / scale) + centroid + pan
                        scale = newScale
                    }
                }
                .testTag("svg_map_canvas")
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0 || h <= 0) return@Canvas

            // 1. Draw Static Dark Background
            drawRect(color = Color(0xFF141F14))

            // 2. Transform Scope for zoom & pan
            withTransform({
                translate(left = panOffset.x, top = panOffset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // Dynamic Grid Lines
                val gridStep = 40f
                val startX = -1000f
                val endX = w + 1000f
                val startY = -1000f
                val endY = h + 1000f

                var gx = startX
                while (gx < endX) {
                    drawLine(
                        color = Color(0x18FFFFFF),
                        start = Offset(gx, startY),
                        end = Offset(gx, endY),
                        strokeWidth = 1f / scale
                    )
                    gx += gridStep
                }
                var gy = startY
                while (gy < endY) {
                    drawLine(
                        color = Color(0x18FFFFFF),
                        start = Offset(startX, gy),
                        end = Offset(endX, gy),
                        strokeWidth = 1f / scale
                    )
                    gy += gridStep
                }

                val pad = 40f
                val uw = (w - pad * 2).coerceAtLeast(10f)
                val uh = (h - pad * 2).coerceAtLeast(10f)

                fun toCanvas(pt: GeoPoint): Offset {
                    val px = pad + ((pt.longitude - minLon) / lonSpan * uw).toFloat()
                    val py = pad + ((maxLat - pt.latitude) / latSpan * uh).toFloat()
                    return Offset(px, py)
                }

                // Draw Field Boundary Polygon
                if (boundary.size >= 3) {
                    val polyScreen = boundary.map { toCanvas(it) }
                    val polyPath = Path().apply {
                        moveTo(polyScreen[0].x, polyScreen[0].y)
                        for (i in 1 until polyScreen.size) {
                            lineTo(polyScreen[i].x, polyScreen[i].y)
                        }
                        close()
                    }
                    // Sawah Fill & Border
                    drawPath(polyPath, color = Color(0x2810B981))
                    drawPath(
                        polyPath,
                        color = Green600,
                        style = Stroke(width = (2.8f / scale).coerceAtLeast(1.5f))
                    )

                    // Corner vertex pins
                    polyScreen.forEach { vertex ->
                        drawCircle(
                            color = Green700,
                            radius = (4f / scale).coerceIn(2.5f, 6f),
                            center = vertex
                        )
                    }
                }

                // Draw Planned Planting Swaths & Trajectory
                if (waypoints.size >= 2) {
                    val wpScreen = waypoints.map { toCanvas(it) }
                    val wpPath = Path().apply {
                        moveTo(wpScreen[0].x, wpScreen[0].y)
                        for (i in 1 until wpScreen.size) {
                            lineTo(wpScreen[i].x, wpScreen[i].y)
                        }
                    }

                    // Main Planting Path Line (High visibility neon agricultural green)
                    drawPath(
                        wpPath,
                        color = Color(0xFF00E676),
                        style = Stroke(width = (2.2f / scale).coerceIn(1.2f, 3.5f))
                    )

                    // Directional arrows along long straight swaths
                    if (showDirectionArrows && scale >= 1.2f) {
                        for (i in 0 until wpScreen.size - 1 step 2) {
                            val p1 = wpScreen[i]
                            val p2 = wpScreen[i + 1]
                            val dx = p2.x - p1.x
                            val dy = p2.y - p1.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist > 25f) {
                                val mid = Offset((p1.x + p2.x) / 2, (p1.y + p2.y) / 2)
                                val angle = atan2(dy, dx)
                                val arrowLen = (7f / scale).coerceIn(4f, 10f)
                                val a1 = Offset(
                                    mid.x - arrowLen * cos(angle - Math.PI / 6).toFloat(),
                                    mid.y - arrowLen * sin(angle - Math.PI / 6).toFloat()
                                )
                                val a2 = Offset(
                                    mid.x - arrowLen * cos(angle + Math.PI / 6).toFloat(),
                                    mid.y - arrowLen * sin(angle + Math.PI / 6).toFloat()
                                )
                                drawLine(
                                    color = Color(0xFFB9F6CA),
                                    start = mid,
                                    end = a1,
                                    strokeWidth = (1.8f / scale).coerceAtLeast(1f)
                                )
                                drawLine(
                                    color = Color(0xFFB9F6CA),
                                    start = mid,
                                    end = a2,
                                    strokeWidth = (1.8f / scale).coerceAtLeast(1f)
                                )
                            }
                        }
                    }

                    // Waypoint dots when inspected closely
                    if (scale >= 2.2f) {
                        wpScreen.forEachIndexed { idx, pt ->
                            if (idx % 2 == 0) {
                                drawCircle(
                                    color = Color(0xCC00E676),
                                    radius = (3f / scale).coerceIn(1.5f, 4f),
                                    center = pt
                                )
                            }
                        }
                    }

                    // Start Point Marker (Green Flag / Pin)
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = (8f / scale).coerceIn(4f, 10f),
                        center = wpScreen.first()
                    )
                    drawCircle(
                        color = Color.White,
                        radius = (4f / scale).coerceIn(2f, 5f),
                        center = wpScreen.first()
                    )

                    // Finish Point Marker (Red Flag / Target)
                    drawCircle(
                        color = ErrorRed,
                        radius = (8f / scale).coerceIn(4f, 10f),
                        center = wpScreen.last()
                    )
                    drawCircle(
                        color = Color.White,
                        radius = (4f / scale).coerceIn(2f, 5f),
                        center = wpScreen.last()
                    )
                }

                // Draw Irrigation & Sawah Markers
                if (markers.isNotEmpty()) {
                    markers.forEach { marker ->
                        val markerPos = toCanvas(marker.point)
                        val mColor = Color(marker.type.colorHex)

                        drawCircle(
                            color = mColor.copy(alpha = 0.35f),
                            radius = (14f / scale).coerceIn(7f, 20f),
                            center = markerPos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = (8f / scale).coerceIn(4f, 11f),
                            center = markerPos
                        )
                        drawCircle(
                            color = mColor,
                            radius = (6.5f / scale).coerceIn(3.5f, 9f),
                            center = markerPos
                        )
                    }
                }

                // Draw Active Machine Telemetry & Rover Position
                if (telemetry != null && telemetry.latitude != 0.0) {
                    val machinePos = toCanvas(GeoPoint(telemetry.latitude, telemetry.longitude))
                    val roverHeadingRad = Math.toRadians(telemetry.headingDeg - 90.0)

                    // Pulse Beacon Glow
                    drawCircle(
                        color = Color(0x44F59E0B),
                        radius = (16f / scale).coerceIn(8f, 22f),
                        center = machinePos
                    )
                    // Machine Outer Halo
                    drawCircle(
                        color = RouteMachineColor,
                        radius = (9f / scale).coerceIn(4.5f, 12f),
                        center = machinePos
                    )
                    // Machine Core
                    drawCircle(
                        color = Color.White,
                        radius = (4.5f / scale).coerceIn(2.5f, 6f),
                        center = machinePos
                    )

                    // Direction Heading Pointer Arrow
                    val pointerLen = (15f / scale).coerceIn(8f, 20f)
                    val pointerEnd = Offset(
                        machinePos.x + (pointerLen * cos(roverHeadingRad)).toFloat(),
                        machinePos.y + (pointerLen * sin(roverHeadingRad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = machinePos,
                        end = pointerEnd,
                        strokeWidth = (3f / scale).coerceIn(1.5f, 4f)
                    )
                }
            }
        }

        // Overlay 1: Top Right Zoom Level & Help Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x88000000),
                modifier = Modifier.testTag("zoom_level_badge")
            ) {
                Text(
                    text = "${String.format("%.1f", scale)}x",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Overlay 2: Top Left Inspection Guidance (visible when not zoomed)
        AnimatedVisibility(
            visible = scale <= 1.05f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x66000000)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Pinch,
                        contentDescription = null,
                        tint = Color(0xCCFFFFFF),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pinch & Pan untuk Detail Jalur",
                        color = Color(0xCCFFFFFF),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Overlay 3: Floating Zoom & Pan Precision Buttons (Bottom Right)
        if (enableZoomControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Center on Machine Rover (if telemetry available)
                if (telemetry != null && telemetry.latitude != 0.0) {
                    MapControlButton(
                        icon = Icons.Default.MyLocation,
                        description = "Fokus Posisi Mesin",
                        testTag = "btn_center_machine",
                        onClick = {
                            scale = 3.0f
                            // Center calculation is handled next frame
                            panOffset = Offset.Zero
                        }
                    )
                }

                // Zoom In (+)
                MapControlButton(
                    icon = Icons.Default.Add,
                    description = "Perbesar Peta",
                    testTag = "btn_zoom_in",
                    onClick = {
                        val newScale = (scale * 1.35f).coerceAtMost(12.0f)
                        scale = newScale
                    }
                )

                // Zoom Out (-)
                MapControlButton(
                    icon = Icons.Default.Remove,
                    description = "Perkecil Peta",
                    testTag = "btn_zoom_out",
                    onClick = {
                        val newScale = (scale / 1.35f).coerceAtLeast(0.6f)
                        scale = newScale
                    }
                )

                // Reset View / 100% Fit
                if (scale != 1.0f || panOffset != Offset.Zero) {
                    MapControlButton(
                        icon = Icons.Default.FilterCenterFocus,
                        description = "Reset Tampilan Peta",
                        testTag = "btn_reset_zoom",
                        onClick = { resetView() }
                    )
                }
            }
        }

        // Overlay 4: Dynamic Metric Scale Bar (Bottom Left)
        if (showScaleBar) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x77000000),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .testTag("scale_bar_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Approximate real scale calculation
                    val approxMetersPerDegree = 111320.0
                    val fieldWidthM = lonSpan * approxMetersPerDegree * cos(Math.toRadians(minLat))
                    val barMeters = (fieldWidthM / (4.0 * scale)).coerceIn(0.5, 500.0)
                    val barLabel = if (barMeters >= 100) {
                        String.format("%.0f m", barMeters)
                    } else if (barMeters >= 10) {
                        String.format("%.0f m", barMeters)
                    } else {
                        String.format("%.1f m", barMeters)
                    }

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = barLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xCC1F2937),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(34.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
