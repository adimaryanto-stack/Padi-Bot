package com.example.padibot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.GeoPoint
import com.example.padibot.theme.Green700
import com.example.padibot.theme.Green100
import com.example.padibot.theme.Green50
import com.example.padibot.theme.WarningOrange
import kotlin.math.max
import kotlin.math.min

@Composable
fun FieldMapCanvas(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = false,
    onPointAdded: ((GeoPoint) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Green50)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isInteractive) {
                    if (isInteractive && onPointAdded != null) {
                        detectTapGestures { tapOffset ->
                            // Convert tap relative to canvas into estimated geo point offset
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val normX = (tapOffset.x - centerX) / (size.width * 0.4f)
                            val normY = (tapOffset.y - centerY) / (size.height * 0.4f)

                            val baseLat = if (points.isNotEmpty()) points.first().lat else -6.9234
                            val baseLon = if (points.isNotEmpty()) points.first().lon else 107.6100

                            val newLat = baseLat - normY * 0.0004
                            val newLon = baseLon + normX * 0.0004
                            onPointAdded(GeoPoint(newLat, newLon))
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Draw grid pattern
            drawFieldGrid(width, height)

            if (points.isEmpty()) return@Canvas

            // Compute bounding box in local metric coordinates
            val origin = PolygonMath.calculateCentroid(points)
            val metricPoints = points.map { PolygonMath.toLocalMeters(it, origin) }

            var minX = metricPoints.minOf { it.x }
            var maxX = metricPoints.maxOf { it.x }
            var minY = metricPoints.minOf { it.y }
            var maxY = metricPoints.maxOf { it.y }

            val spanX = max(maxX - minX, 10.0)
            val spanY = max(maxY - minY, 10.0)

            val padding = 36f
            val drawWidth = width - padding * 2
            val drawHeight = height - padding * 2

            val scale = min(drawWidth / spanX.toFloat(), drawHeight / spanY.toFloat())

            val midMetricX = (minX + maxX) / 2.0
            val midMetricY = (minY + maxY) / 2.0
            val canvasCenterX = width / 2f
            val canvasCenterY = height / 2f

            fun toCanvasOffset(metricX: Double, metricY: Double): Offset {
                val dx = (metricX - midMetricX).toFloat() * scale
                val dy = -(metricY - midMetricY).toFloat() * scale // Invert Y for screen coords
                return Offset(canvasCenterX + dx, canvasCenterY + dy)
            }

            val canvasOffsets = metricPoints.map { toCanvasOffset(it.x, it.y) }

            // Draw polygon fill and stroke
            if (canvasOffsets.size >= 3) {
                val polygonPath = Path().apply {
                    moveTo(canvasOffsets[0].x, canvasOffsets[0].y)
                    for (i in 1 until canvasOffsets.size) {
                        lineTo(canvasOffsets[i].x, canvasOffsets[i].y)
                    }
                    close()
                }

                drawPath(
                    path = polygonPath,
                    color = Color(0x337CB342),
                    style = Fill
                )

                drawPath(
                    path = polygonPath,
                    color = Green700,
                    style = Stroke(width = 3.dp.toPx())
                )
            } else if (canvasOffsets.size == 2) {
                drawLine(
                    color = Green700,
                    start = canvasOffsets[0],
                    end = canvasOffsets[1],
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Draw vertex handles
            canvasOffsets.forEachIndexed { index, offset ->
                drawCircle(
                    color = if (index == 0) WarningOrange else Green700,
                    radius = 7.dp.toPx(),
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = offset
                )
            }
        }
    }
}

private fun DrawScope.drawFieldGrid(width: Float, height: Float) {
    val gridSize = 24.dp.toPx()
    val gridColor = Color(0x1A558B2F)

    var x = 0f
    while (x <= width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = 0f
    while (y <= height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}
