package com.example.padibot.algorithm

import com.example.padibot.model.GeoPoint
import kotlin.math.*

data class MetricPoint(val x: Double, val y: Double) {
    fun distanceTo(other: MetricPoint): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun rotate(angleRad: Double): MetricPoint {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return MetricPoint(
            x = x * cosA - y * sinA,
            y = x * sinA + y * cosA
        )
    }
}

object PolygonMath {
    private const val EARTH_RADIUS_M = 6378137.0

    fun calculateCentroid(points: List<GeoPoint>): GeoPoint {
        if (points.isEmpty()) return GeoPoint(-6.9234, 107.6100)
        var sumLat = 0.0
        var sumLon = 0.0
        for (p in points) {
            sumLat += p.lat
            sumLon += p.lon
        }
        return GeoPoint(sumLat / points.size, sumLon / points.size)
    }

    fun toLocalMeters(point: GeoPoint, origin: GeoPoint): MetricPoint {
        val latRad = Math.toRadians(origin.lat)
        val dLat = Math.toRadians(point.lat - origin.lat)
        val dLon = Math.toRadians(point.lon - origin.lon)

        val y = dLat * EARTH_RADIUS_M
        val x = dLon * EARTH_RADIUS_M * cos(latRad)
        return MetricPoint(x, y)
    }

    fun toGeoPoint(metric: MetricPoint, origin: GeoPoint): GeoPoint {
        val latRad = Math.toRadians(origin.lat)
        val dLat = metric.y / EARTH_RADIUS_M
        val dLon = metric.x / (EARTH_RADIUS_M * cos(latRad))

        val lat = origin.lat + Math.toDegrees(dLat)
        val lon = origin.lon + Math.toDegrees(dLon)
        return GeoPoint(lat, lon)
    }

    fun calculateAreaAndPerimeter(points: List<GeoPoint>): Pair<Double, Double> {
        if (points.size < 3) return Pair(0.0, 0.0)
        val origin = calculateCentroid(points)
        val metricPoints = points.map { toLocalMeters(it, origin) }

        var areaSum = 0.0
        var perimeterSum = 0.0
        val n = metricPoints.size

        for (i in 0 until n) {
            val p1 = metricPoints[i]
            val p2 = metricPoints[(i + 1) % n]
            areaSum += (p1.x * p2.y) - (p2.x * p1.y)
            perimeterSum += p1.distanceTo(p2)
        }

        val area = abs(areaSum) * 0.5
        return Pair(area, perimeterSum)
    }

    /**
     * Shrinks a polygon inward toward its centroid by the specified offset in meters.
     */
    fun shrinkPolygon(polygon: List<MetricPoint>, offset: Double): List<MetricPoint> {
        if (polygon.size < 3 || offset <= 0.0) return polygon
        val n = polygon.size
        var cX = 0.0
        var cY = 0.0
        for (p in polygon) {
            cX += p.x
            cY += p.y
        }
        cX /= n
        cY /= n

        var maxDist = 0.0
        for (p in polygon) {
            val d = sqrt((p.x - cX).pow(2) + (p.y - cY).pow(2))
            if (d > maxDist) maxDist = d
        }

        if (maxDist <= offset) return emptyList()
        val scale = ((maxDist - offset) / maxDist).coerceAtLeast(0.01)

        return polygon.map { p ->
            MetricPoint(
                x = cX + (p.x - cX) * scale,
                y = cY + (p.y - cY) * scale
            )
        }
    }

    /**
     * Checks if a 2D line segment (p1 -> p2) intersects with horizontal line y = scanY.
     * Returns the x coordinate of intersection or null.
     */
    fun getHorizontalIntersection(p1: MetricPoint, p2: MetricPoint, scanY: Double): Double? {
        val minY = min(p1.y, p2.y)
        val maxY = max(p1.y, p2.y)
        if (scanY < minY || scanY > maxY || p1.y == p2.y) return null

        val t = (scanY - p1.y) / (p2.y - p1.y)
        return p1.x + t * (p2.x - p1.x)
    }
}
