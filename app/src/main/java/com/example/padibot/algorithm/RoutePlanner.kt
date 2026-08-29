package com.example.padibot.algorithm

import com.example.padibot.model.GeoPoint
import com.example.padibot.model.RoutePattern
import kotlin.math.*

object RoutePlanner {

    fun generateCoverageRoute(
        boundary: List<GeoPoint>,
        machineWidthM: Double,
        headlandWidthM: Double,
        orientationDeg: Double,
        pattern: RoutePattern
    ): RouteGenerationResult {
        if (boundary.size < 3) {
            return RouteGenerationResult(emptyList(), 0.0, 0, 0.0)
        }

        val refLat = boundary.first().latitude
        val refLon = boundary.first().longitude

        val localPoly = boundary.map { PolygonMath.latLonToMeters(it.latitude, it.longitude, refLat, refLon) }

        // Bounding box in local coordinates
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for ((x, y) in localPoly) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width <= 0 || height <= 0) {
            return RouteGenerationResult(emptyList(), 0.0, 0, 0.0)
        }

        val effectiveSpacing = machineWidthM.coerceAtLeast(0.5)
        val insetX = (headlandWidthM * 0.5).coerceAtMost(width * 0.25)
        val insetY = (headlandWidthM * 0.5).coerceAtMost(height * 0.25)

        val innerMinX = minX + insetX
        val innerMaxX = maxX - insetX
        val innerMinY = minY + insetY
        val innerMaxY = maxY - insetY

        val resultLocalWaypoints = mutableListOf<Pair<Double, Double>>()

        when (pattern) {
            RoutePattern.BOUSTROPHEDON -> {
                var currentX = innerMinX + effectiveSpacing / 2
                var goingUp = true
                var laneCount = 0

                while (currentX <= innerMaxX + 0.1) {
                    val yStart = if (goingUp) innerMinY else innerMaxY
                    val yEnd = if (goingUp) innerMaxY else innerMinY

                    resultLocalWaypoints.add(Pair(currentX, yStart))
                    resultLocalWaypoints.add(Pair(currentX, yEnd))

                    currentX += effectiveSpacing
                    goingUp = !goingUp
                    laneCount++
                }
            }

            RoutePattern.HEADLAND_INNER -> {
                // First do inner boustrophedon
                var currentX = innerMinX + effectiveSpacing
                var goingUp = true
                while (currentX <= innerMaxX - effectiveSpacing) {
                    val yStart = if (goingUp) innerMinY + effectiveSpacing else innerMaxY - effectiveSpacing
                    val yEnd = if (goingUp) innerMaxY - effectiveSpacing else innerMinY + effectiveSpacing
                    resultLocalWaypoints.add(Pair(currentX, yStart))
                    resultLocalWaypoints.add(Pair(currentX, yEnd))
                    currentX += effectiveSpacing
                    goingUp = !goingUp
                }
                // Then perimeter headland loop
                resultLocalWaypoints.add(Pair(innerMinX, innerMinY))
                resultLocalWaypoints.add(Pair(innerMaxX, innerMinY))
                resultLocalWaypoints.add(Pair(innerMaxX, innerMaxY))
                resultLocalWaypoints.add(Pair(innerMinX, innerMaxY))
                resultLocalWaypoints.add(Pair(innerMinX, innerMinY))
            }

            RoutePattern.SPIRAL_INWARD -> {
                var x1 = innerMinX
                var x2 = innerMaxX
                var y1 = innerMinY
                var y2 = innerMaxY

                while (x1 < x2 && y1 < y2) {
                    resultLocalWaypoints.add(Pair(x1, y1))
                    resultLocalWaypoints.add(Pair(x2, y1))
                    resultLocalWaypoints.add(Pair(x2, y2))
                    resultLocalWaypoints.add(Pair(x1, y2))
                    x1 += effectiveSpacing
                    x2 -= effectiveSpacing
                    y1 += effectiveSpacing
                    y2 -= effectiveSpacing
                }
            }

            RoutePattern.SPIRAL_OUTWARD -> {
                val tempWaypoints = mutableListOf<Pair<Double, Double>>()
                var x1 = innerMinX
                var x2 = innerMaxX
                var y1 = innerMinY
                var y2 = innerMaxY

                while (x1 < x2 && y1 < y2) {
                    tempWaypoints.add(Pair(x1, y1))
                    tempWaypoints.add(Pair(x2, y1))
                    tempWaypoints.add(Pair(x2, y2))
                    tempWaypoints.add(Pair(x1, y2))
                    x1 += effectiveSpacing
                    x2 -= effectiveSpacing
                    y1 += effectiveSpacing
                    y2 -= effectiveSpacing
                }
                resultLocalWaypoints.addAll(tempWaypoints.reversed())
            }
        }

        // Convert local waypoints back to GeoPoints and compute total distance
        var totalDist = 0.0
        val geoWaypoints = mutableListOf<GeoPoint>()

        for (i in resultLocalWaypoints.indices) {
            val (x, y) = resultLocalWaypoints[i]
            geoWaypoints.add(PolygonMath.metersToLatLon(x, y, refLat, refLon))
            if (i > 0) {
                val (px, py) = resultLocalWaypoints[i - 1]
                val dx = x - px
                val dy = y - py
                totalDist += sqrt(dx * dx + dy * dy)
            }
        }

        val totalLanes = max(1, (width / effectiveSpacing).toInt())
        val fieldArea = PolygonMath.calculateAreaAndPerimeter(boundary).first
        val coveredArea = totalDist * machineWidthM
        val coveragePct = if (fieldArea > 0) min(99.5, (coveredArea / fieldArea) * 100.0) else 95.0

        return RouteGenerationResult(
            waypoints = geoWaypoints,
            totalDistanceM = totalDist,
            totalLanes = totalLanes,
            coveragePct = (coveragePct * 10).roundToInt() / 10.0
        )
    }
}
