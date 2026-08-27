package com.example.padibot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.GeoPoint
import com.example.padibot.model.Telemetry
import com.example.padibot.model.Waypoint
import com.example.padibot.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun LiveMissionCanvas(
    boundary: List<GeoPoint>,
    waypoints: List<Waypoint>,
    telemetry: Telemetry?,
    modifier: Modifier = Modifier,
    heightDp: Int = 300
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F5E9))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw agricultural soil/paddy background grid
            drawMissionGrid(width, height)

            if (boundary.isEmpty()) return@Canvas

            val origin = PolygonMath.calculateCentroid(boundary)
            val metricBoundary = boundary.map { PolygonMath.toLocalMeters(it, origin) }

            var minX = metricBoundary.minOf { it.x }
            var maxX = metricBoundary.maxOf { it.x }
            var minY = metricBoundary.minOf { it.y }
            var maxY = metricBoundary.maxOf { it.y }

            val spanX = max(maxX - minX, 15.0)
            val spanY = max(maxY - minY, 15.0)

            val padding = 40f
            val drawWidth = width - padding * 2
            val drawHeight = height - padding * 2

            val scale = min(drawWidth / spanX.toFloat(), drawHeight / spanY.toFloat())

            val midMetricX = (minX + maxX) / 2.0
            val midMetricY = (minY + maxY) / 2.0
            val canvasCenterX = width / 2f
            val canvasCenterY = height / 2f

            fun toCanvas(metricX: Double, metricY: Double): Offset {
                val dx = (metricX - midMetricX).toFloat() * scale
                val dy = -(metricY - midMetricY).toFloat() * scale
                return Offset(canvasCenterX + dx, canvasCenterY + dy)
            }

            fun geoToCanvas(point: GeoPoint): Offset {
                val m = PolygonMath.toLocalMeters(point, origin)
                return toCanvas(m.x, m.y)
            }

            // 2. Draw Field Polygon Boundary
            val boundaryOffsets = metricBoundary.map { toCanvas(it.x, it.y) }
            if (boundaryOffsets.size >= 3) {
                val polyPath = Path().apply {
                    moveTo(boundaryOffsets[0].x, boundaryOffsets[0].y)
                    for (i in 1 until boundaryOffsets.size) {
                        lineTo(boundaryOffsets[i].x, boundaryOffsets[i].y)
                    }
                    close()
                }

                drawPath(
                    path = polyPath,
                    color = Color(0x2B4CAF50),
                    style = Fill
                )
                drawPath(
                    path = polyPath,
                    color = Green800,
                    style = Stroke(
                        width = 3.5.dp.toPx(),
                        pathEffect = PathEffect.cornerPathEffect(8f)
                    )
                )
            }

            // 3. Draw Route Lanes & Transitions
            if (waypoints.isNotEmpty()) {
                val waypointOffsets = waypoints.map {
                    geoToCanvas(GeoPoint(it.lat, it.lon))
                }

                val currentLane = telemetry?.currentLaneIndex ?: 0

                for (i in 0 until waypointOffsets.size - 1) {
                    val p1 = waypointOffsets[i]
                    val p2 = waypointOffsets[i + 1]
                    val wp = waypoints[i]

                    val isCompleted = wp.laneIndex < currentLane
                    val isCurrent = wp.laneIndex == currentLane

                    val laneColor = when {
                        isCompleted -> SuccessGreen
                        isCurrent -> Color(0xFF1E88E5)
                        else -> Color(0xFF90CAF9)
                    }

                    val strokeWidth = if (isCurrent) 4.dp.toPx() else 2.5.dp.toPx()

                    drawLine(
                        color = laneColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Draw Start & End Markers
                val startPos = waypointOffsets.first()
                val endPos = waypointOffsets.last()

                // Start Marker (Green circle with S)
                drawCircle(
                    color = SuccessGreen,
                    radius = 8.dp.toPx(),
                    center = startPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = startPos
                )

                // End Marker (Red circle)
                drawCircle(
                    color = ErrorRed,
                    radius = 8.dp.toPx(),
                    center = endPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = endPos
                )
            }

            // 4. Draw Rover Machine Robot
            if (telemetry != null) {
                val machinePos = geoToCanvas(GeoPoint(telemetry.positionLat, telemetry.positionLon))
                val heading = telemetry.headingDeg

                // Radar glow
                drawCircle(
                    color = Color(0x33FFB300),
                    radius = 18.dp.toPx(),
                    center = machinePos
                )

                // Rover Body (Rotated with Heading)
                rotate(degrees = heading, pivot = machinePos) {
                    // Rover chassis
                    val chassisWidth = 20.dp.toPx()
                    val chassisHeight = 26.dp.toPx()
                    val chassisRect = androidx.compose.ui.geometry.Rect(
                        machinePos.x - chassisWidth / 2f,
                        machinePos.y - chassisHeight / 2f,
                        machinePos.x + chassisWidth / 2f,
                        machinePos.y + chassisHeight / 2f
                    )
                    drawRoundRect(
                        color = Color(0xFFFFB300),
                        topLeft = chassisRect.topLeft,
                        size = chassisRect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Fill
                    )
                    drawRoundRect(
                        color = Color(0xFFE65100),
                        topLeft = chassisRect.topLeft,
                        size = chassisRect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Direction Arrow (Heading Pointer)
                    val arrowPath = Path().apply {
                        moveTo(machinePos.x, machinePos.y - chassisHeight / 2f - 4.dp.toPx())
                        lineTo(machinePos.x - 5.dp.toPx(), machinePos.y - chassisHeight / 2f + 4.dp.toPx())
                        lineTo(machinePos.x + 5.dp.toPx(), machinePos.y - chassisHeight / 2f + 4.dp.toPx())
                        close()
                    }
                    drawPath(arrowPath, color = Color(0xFFD50000))
                }
            }
        }
    }
}

private fun DrawScope.drawMissionGrid(width: Float, height: Float) {
    val step = 20.dp.toPx()
    val color = Color(0x1433691E)
    var x = 0f
    while (x <= width) {
        drawLine(color, Offset(x, 0f), Offset(x, height), 1f)
        x += step
    }
    var y = 0f
    while (y <= height) {
        drawLine(color, Offset(0f, y), Offset(width, y), 1f)
        y += step
    }
}
