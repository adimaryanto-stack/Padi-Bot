package com.example.padibot.algorithm

import com.example.padibot.model.GeoPoint
import kotlin.math.*

object PolygonMath {

    private const val EARTH_RADIUS = 6378137.0 // Earth radius in meters

    /**
     * Converts a lat/lon point to local Cartesian (x, y in meters) relative to a reference origin.
     */
    fun latLonToMeters(lat: Double, lon: Double, refLat: Double, refLon: Double): Pair<Double, Double> {
        val dLat = Math.toRadians(lat - refLat)
        val dLon = Math.toRadians(lon - refLon)
        val y = dLat * EARTH_RADIUS
        val x = dLon * EARTH_RADIUS * cos(Math.toRadians(refLat))
        return Pair(x, y)
    }

    /**
     * Converts local Cartesian (x, y in meters) back to GeoPoint.
     */
    fun metersToLatLon(x: Double, y: Double, refLat: Double, refLon: Double): GeoPoint {
        val dLat = y / EARTH_RADIUS
        val dLon = x / (EARTH_RADIUS * cos(Math.toRadians(refLat)))
        val lat = refLat + Math.toDegrees(dLat)
        val lon = refLon + Math.toDegrees(dLon)
        return GeoPoint(lat, lon)
    }

    /**
     * Calculates Geodesic Area (m²) and Perimeter (m) of a polygon using Shoelace formula in local projection.
     */
    fun calculateAreaAndPerimeter(points: List<GeoPoint>): Pair<Double, Double> {
        if (points.size < 3) return Pair(0.0, 0.0)

        val refLat = points.first().latitude
        val refLon = points.first().longitude

        val localPts = points.map { latLonToMeters(it.latitude, it.longitude, refLat, refLon) }

        var areaSum = 0.0
        var perimeterSum = 0.0
        val n = localPts.size

        for (i in 0 until n) {
            val (x1, y1) = localPts[i]
            val (x2, y2) = localPts[(i + 1) % n]
            areaSum += (x1 * y2 - x2 * y1)
            val dx = x2 - x1
            val dy = y2 - y1
            perimeterSum += sqrt(dx * dx + dy * dy)
        }

        val area = abs(areaSum) / 2.0
        return Pair(area, perimeterSum)
    }

    /**
     * Ray-casting algorithm to check if a point is inside a polygon.
     */
    fun isPointInPolygon(pt: Pair<Double, Double>, poly: List<Pair<Double, Double>>): Boolean {
        var inside = false
        val n = poly.size
        var j = n - 1
        for (i in 0 until n) {
            val (xi, yi) = poly[i]
            val (xj, yj) = poly[j]

            val intersect = ((yi > pt.second) != (yj > pt.second)) &&
                    (pt.first < (xj - xi) * (pt.second - yi) / (yj - yi + 1e-12) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
