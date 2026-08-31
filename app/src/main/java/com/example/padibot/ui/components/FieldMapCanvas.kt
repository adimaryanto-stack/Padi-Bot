package com.example.padibot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.padibot.model.GeoPoint
import com.example.padibot.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun FieldMapCanvas(
    points: List<GeoPoint> = emptyList(),
    boundary: List<GeoPoint> = emptyList(),
    currentGpsLocation: GeoPoint? = null,
    walkedTrail: List<GeoPoint> = emptyList(),
    gpsAccuracyMeters: Float = 0f,
    heightDp: Int = 190,
    isInteractive: Boolean = false,
    onPointAdded: ((GeoPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pts = if (points.isNotEmpty()) points else boundary

    // Combine all spatial references to calculate dynamic canvas viewport bounds
    val allRefPoints = buildList {
        addAll(pts)
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
    val minSpan = 0.00045 // approx ~50 meters view window

    val actualLatSpan = max(rawMaxLat - rawMinLat, minSpan)
    val actualLonSpan = max(rawMaxLon - rawMinLon, minSpan)

    val minLat = centerLat - actualLatSpan / 2.0
    val maxLat = centerLat + actualLatSpan / 2.0
    val minLon = centerLon - actualLonSpan / 2.0
    val maxLon = centerLon + actualLonSpan / 2.0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .then(
                if (isInteractive && onPointAdded != null) {
                    Modifier.pointerInput(pts, minLat, maxLat, minLon, maxLon) {
                        detectTapGestures { offset ->
                            val w = size.width
                            val h = size.height
                            if (w > 0 && h > 0) {
                                val padding = 36f
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

        // Dark/Green Field Grid Background
        drawRect(color = Color(0xFF162316))

        // Dynamic Grid lines
        val gridStep = 32f
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < h) {
            drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        val padding = 36f
        val usableW = (w - padding * 2).coerceAtLeast(10f)
        val usableH = (h - padding * 2).coerceAtLeast(10f)

        fun toCanvas(pt: GeoPoint): Offset {
            val px = padding + ((pt.longitude - minLon) / actualLonSpan * usableW).toFloat()
            val py = padding + ((maxLat - pt.latitude) / actualLatSpan * usableH).toFloat()
            return Offset(px, py)
        }

        // 1. Draw Walked GPS Trail (if user is walking in Walk & Map mode)
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
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            )
        }

        // 2. Draw Recorded Polygon Boundary
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
                // Sawah Interior Fill
                drawPath(path = polyPath, color = RouteBoundaryFill)
                // Boundary Border Line
                drawPath(path = polyPath, color = RouteBoundary, style = Stroke(width = 3.5f))
            } else if (screenPoints.size == 2) {
                drawLine(
                    color = RouteBoundary,
                    start = screenPoints[0],
                    end = screenPoints[1],
                    strokeWidth = 3.5f
                )
            }

            // Draw Boundary Vertices / Corner Pins
            screenPoints.forEachIndexed { index, offset ->
                drawCircle(
                    color = if (index == 0) SuccessGreen else Color.White,
                    radius = 7.5f,
                    center = offset
                )
                drawCircle(
                    color = Green900,
                    radius = 3.5f,
                    center = offset
                )
            }
        }

        // 3. Draw User's Real-time Live GPS Position
        if (currentGpsLocation != null && currentGpsLocation.latitude != 0.0) {
            val gpsScreenPos = toCanvas(currentGpsLocation)

            // Accuracy Radius Circle (Semi-transparent Blue)
            val approxMetersToPixels = (usableW / (actualLonSpan * 111320.0)).toFloat()
            val pixelRadius = if (gpsAccuracyMeters > 0) {
                (gpsAccuracyMeters * approxMetersToPixels).coerceIn(12f, 80f)
            } else {
                16f
            }

            drawCircle(
                color = Color(0x333B82F6),
                radius = pixelRadius,
                center = gpsScreenPos
            )
            drawCircle(
                color = Color(0x663B82F6),
                radius = pixelRadius,
                center = gpsScreenPos,
                style = Stroke(width = 1.5f)
            )

            // Outer Pulse Ring
            drawCircle(
                color = Color(0xFF2563EB),
                radius = 9f,
                center = gpsScreenPos
            )
            // Center Core Dot
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = gpsScreenPos
            )
        }
    }
}
