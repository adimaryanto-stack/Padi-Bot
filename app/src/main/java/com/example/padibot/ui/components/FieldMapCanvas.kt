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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.padibot.model.GeoPoint
import com.example.padibot.theme.*

@Composable
fun FieldMapCanvas(
    points: List<GeoPoint> = emptyList(),
    boundary: List<GeoPoint> = emptyList(),
    heightDp: Int = 180,
    isInteractive: Boolean = false,
    onPointAdded: ((GeoPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pts = if (points.isNotEmpty()) points else boundary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .then(
                if (isInteractive && onPointAdded != null) {
                    Modifier.pointerInput(pts) {
                        detectTapGestures { offset ->
                            // Map pixel tap back to approx geo coordinates
                            val w = size.width
                            val h = size.height
                            if (w > 0 && h > 0) {
                                val latBase = if (pts.isNotEmpty()) pts.first().latitude else -6.923500
                                val lonBase = if (pts.isNotEmpty()) pts.first().longitude else 107.610200
                                val normX = (offset.x / w - 0.5) * 0.001
                                val normY = -(offset.y / h - 0.5) * 0.001
                                onPointAdded(GeoPoint(latBase + normY, lonBase + normX))
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        val w = size.width
        val h = size.height

        // Dark/Green Field Grid Background
        drawRect(color = Color(0xFF1E2D1E))

        // Grid lines
        val gridStep = 30f
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0x1AFFFFFF),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < h) {
            drawLine(
                color = Color(0x1AFFFFFF),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        if (pts.isEmpty()) return@Canvas

        // Normalize coords to fit nicely inside canvas with padding
        val minLat = pts.minOf { it.latitude }
        val maxLat = pts.maxOf { it.latitude }
        val minLon = pts.minOf { it.longitude }
        val maxLon = pts.maxOf { it.longitude }

        val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonSpan = (maxLon - minLon).coerceAtLeast(0.0001)

        val padding = 40f
        val usableW = (w - padding * 2).coerceAtLeast(10f)
        val usableH = (h - padding * 2).coerceAtLeast(10f)

        val screenPoints = pts.map { pt ->
            val px = padding + ((pt.longitude - minLon) / lonSpan * usableW).toFloat()
            val py = padding + ((maxLat - pt.latitude) / latSpan * usableH).toFloat()
            Offset(px, py)
        }

        if (screenPoints.size >= 3) {
            val path = Path().apply {
                moveTo(screenPoints[0].x, screenPoints[0].y)
                for (i in 1 until screenPoints.size) {
                    lineTo(screenPoints[i].x, screenPoints[i].y)
                }
                close()
            }
            // Fill
            drawPath(path = path, color = RouteBoundaryFill)
            // Stroke
            drawPath(path = path, color = RouteBoundary, style = Stroke(width = 3f))
        } else if (screenPoints.size == 2) {
            drawLine(
                color = RouteBoundary,
                start = screenPoints[0],
                end = screenPoints[1],
                strokeWidth = 3f
            )
        }

        // Draw vertices
        screenPoints.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == 0) SuccessGreen else Color.White,
                radius = 7f,
                center = offset
            )
            drawCircle(
                color = Green900,
                radius = 3.5f,
                center = offset
            )
        }
    }
}
