package com.example.padibot.algorithm

import com.example.padibot.model.GeoPoint
import com.example.padibot.model.RoutePattern
import com.example.padibot.model.Waypoint
import com.example.padibot.model.WaypointType
import kotlin.math.*

data class RouteGenerationResult(
    val waypoints: List<Waypoint>,
    val totalLanes: Int,
    val totalDistanceM: Double,
    val coveragePct: Double,
    val estimatedDurationSec: Long,
    val pattern: RoutePattern = RoutePattern.BOUSTROPHEDON
)

object RoutePlanner {

    fun generateCoverageRoute(
        boundary: List<GeoPoint>,
        machineWidthM: Double = 1.5,
        headlandWidthM: Double = 3.0,
        orientationDeg: Double = 0.0,
        pattern: RoutePattern = RoutePattern.BOUSTROPHEDON
    ): RouteGenerationResult {
        if (boundary.size < 3) {
            return RouteGenerationResult(emptyList(), 0, 0.0, 0.0, 0, pattern)
        }

        val origin = PolygonMath.calculateCentroid(boundary)
        val metricBoundary = boundary.map { PolygonMath.toLocalMeters(it, origin) }

        return when (pattern) {
            RoutePattern.BOUSTROPHEDON -> generateBoustrophedon(
                metricBoundary, origin, machineWidthM, headlandWidthM, orientationDeg, withHeadlandCircuit = false
            )
            RoutePattern.HEADLAND_INNER -> generateBoustrophedon(
                metricBoundary, origin, machineWidthM, headlandWidthM, orientationDeg, withHeadlandCircuit = true
            )
            RoutePattern.SPIRAL_INWARD -> generateConcentricSpiral(
                metricBoundary, origin, machineWidthM, headlandWidthM, inward = true
            )
            RoutePattern.SPIRAL_OUTWARD -> generateConcentricSpiral(
                metricBoundary, origin, machineWidthM, headlandWidthM, inward = false
            )
        }
    }

    private fun generateBoustrophedon(
        metricBoundary: List<MetricPoint>,
        origin: GeoPoint,
        machineWidthM: Double,
        headlandWidthM: Double,
        orientationDeg: Double,
        withHeadlandCircuit: Boolean
    ): RouteGenerationResult {
        val angleRad = Math.toRadians(orientationDeg)
        val rotatedBoundary = metricBoundary.map { it.rotate(-angleRad) }

        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE

        for (p in rotatedBoundary) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }

        val width = maxX - minX
        val height = maxY - minY

        val safeHeadland = min(headlandWidthM, min(width, height) * 0.25).coerceAtLeast(0.5)
        val effectiveMinY = minY + safeHeadland
        val effectiveMaxY = maxY - safeHeadland

        val sweepHeight = max(effectiveMaxY - effectiveMinY, machineWidthM)
        val numLanes = ceil(sweepHeight / machineWidthM).toInt().coerceAtLeast(1)
        val stepY = sweepHeight / numLanes

        val generatedWaypoints = mutableListOf<Waypoint>()
        val n = rotatedBoundary.size
        var orderCounter = 0
        var totalDistance = 0.0
        var lastPoint: MetricPoint? = null

        for (laneIdx in 0 until numLanes) {
            val scanY = effectiveMinY + (laneIdx + 0.5) * stepY

            val intersections = mutableListOf<Double>()
            for (i in 0 until n) {
                val p1 = rotatedBoundary[i]
                val p2 = rotatedBoundary[(i + 1) % n]
                val xInt = PolygonMath.getHorizontalIntersection(p1, p2, scanY)
                if (xInt != null) {
                    intersections.add(xInt)
                }
            }

            intersections.sort()

            val (startX, endX) = if (intersections.size >= 2) {
                val rawStart = intersections.first()
                val rawEnd = intersections.last()
                val insetStart = (rawStart + safeHeadland).coerceAtMost(rawEnd - machineWidthM)
                val insetEnd = (rawEnd - safeHeadland).coerceAtLeast(rawStart + machineWidthM)
                Pair(insetStart, insetEnd)
            } else {
                Pair(minX + safeHeadland, maxX - safeHeadland)
            }

            val isLeftToRight = (laneIdx % 2 == 0)
            val pStart = if (isLeftToRight) MetricPoint(startX, scanY) else MetricPoint(endX, scanY)
            val pEnd = if (isLeftToRight) MetricPoint(endX, scanY) else MetricPoint(startX, scanY)

            if (lastPoint != null) {
                val turnPt1 = MetricPoint(lastPoint.x, scanY)
                val geoTurn1 = PolygonMath.toGeoPoint(turnPt1.rotate(angleRad), origin)
                totalDistance += lastPoint.distanceTo(turnPt1)
                generatedWaypoints.add(
                    Waypoint(
                        id = "turn_1_$laneIdx",
                        order = orderCounter++,
                        point = geoTurn1,
                        laneIndex = laneIdx,
                        type = WaypointType.TURN,
                        speedLimitMps = 0.3
                    )
                )
            }

            val geoStart = PolygonMath.toGeoPoint(pStart.rotate(angleRad), origin)
            val geoEnd = PolygonMath.toGeoPoint(pEnd.rotate(angleRad), origin)

            if (lastPoint != null) {
                totalDistance += MetricPoint(lastPoint.x, scanY).distanceTo(pStart)
            }

            generatedWaypoints.add(
                Waypoint(
                    id = "lane_start_$laneIdx",
                    order = orderCounter++,
                    point = geoStart,
                    laneIndex = laneIdx,
                    type = WaypointType.LANE,
                    speedLimitMps = 0.5
                )
            )

            totalDistance += pStart.distanceTo(pEnd)

            generatedWaypoints.add(
                Waypoint(
                    id = "lane_end_$laneIdx",
                    order = orderCounter++,
                    point = geoEnd,
                    laneIndex = laneIdx,
                    type = WaypointType.LANE,
                    speedLimitMps = 0.5
                )
            )

            lastPoint = pEnd
        }

        // Add outer headland perimeter safety circuit if requested
        if (withHeadlandCircuit && metricBoundary.size >= 3) {
            val headlandRing = PolygonMath.shrinkPolygon(metricBoundary, safeHeadland * 0.5)
            for (i in headlandRing.indices) {
                val pt = headlandRing[i]
                val geoPt = PolygonMath.toGeoPoint(pt, origin)
                if (lastPoint != null) {
                    totalDistance += lastPoint.distanceTo(pt)
                }
                generatedWaypoints.add(
                    Waypoint(
                        id = "headland_$i",
                        order = orderCounter++,
                        point = geoPt,
                        laneIndex = numLanes,
                        type = WaypointType.HEADLAND,
                        speedLimitMps = 0.4
                    )
                )
                lastPoint = pt
            }
        }

        val speedAvg = 0.5
        val durationSec = (totalDistance / speedAvg).toLong()
        val coverage = if (withHeadlandCircuit) 98.5 else 96.0

        val pat = if (withHeadlandCircuit) RoutePattern.HEADLAND_INNER else RoutePattern.BOUSTROPHEDON
        return RouteGenerationResult(
            waypoints = generatedWaypoints,
            totalLanes = if (withHeadlandCircuit) numLanes + 1 else numLanes,
            totalDistanceM = totalDistance,
            coveragePct = coverage,
            estimatedDurationSec = durationSec,
            pattern = pat
        )
    }

    private fun generateConcentricSpiral(
        metricBoundary: List<MetricPoint>,
        origin: GeoPoint,
        machineWidthM: Double,
        headlandWidthM: Double,
        inward: Boolean
    ): RouteGenerationResult {
        val rings = mutableListOf<List<MetricPoint>>()
        var currentOffset = machineWidthM * 0.5

        while (true) {
            val shrunken = PolygonMath.shrinkPolygon(metricBoundary, currentOffset)
            if (shrunken.size < 3) break
            rings.add(shrunken)
            currentOffset += machineWidthM
        }

        if (rings.isEmpty()) {
            return generateBoustrophedon(metricBoundary, origin, machineWidthM, headlandWidthM, 0.0, false)
        }

        val orderedRings = if (inward) rings else rings.reversed()
        val generatedWaypoints = mutableListOf<Waypoint>()
        var orderCounter = 0
        var totalDistance = 0.0
        var lastPoint: MetricPoint? = null

        for ((ringIdx, ring) in orderedRings.withIndex()) {
            for (i in 0 until ring.size + 1) {
                val pt = ring[i % ring.size]
                val geoPt = PolygonMath.toGeoPoint(pt, origin)

                if (lastPoint != null) {
                    totalDistance += lastPoint.distanceTo(pt)
                }

                generatedWaypoints.add(
                    Waypoint(
                        id = "spiral_${ringIdx}_$i",
                        order = orderCounter++,
                        point = geoPt,
                        laneIndex = ringIdx,
                        type = if (ringIdx == 0) WaypointType.HEADLAND else WaypointType.LANE,
                        speedLimitMps = 0.5
                    )
                )
                lastPoint = pt
            }
        }

        val speedAvg = 0.5
        val durationSec = (totalDistance / speedAvg).toLong()
        val pat = if (inward) RoutePattern.SPIRAL_INWARD else RoutePattern.SPIRAL_OUTWARD

        return RouteGenerationResult(
            waypoints = generatedWaypoints,
            totalLanes = rings.size,
            totalDistanceM = totalDistance,
            coveragePct = 97.0,
            estimatedDurationSec = durationSec,
            pattern = pat
        )
    }
}
