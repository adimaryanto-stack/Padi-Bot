package com.example.padibot.algorithm

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 2D Kalman Filter for smoothing GPS coordinates (Latitude, Longitude)
 * and filtering out noise, multipath spikes, and rapid drift.
 */
class GpsKalmanFilter(
    private val processNoiseQ: Double = 0.000003 // Process variance (motion model)
) {
    private var lat = 0.0
    private var lon = 0.0
    private var variance = -1.0 // P matrix (error covariance)
    private var lastTimestampMs: Long = 0

    val isInitialized: Boolean get() = variance >= 0

    fun reset() {
        variance = -1.0
        lat = 0.0
        lon = 0.0
        lastTimestampMs = 0
    }

    /**
     * Updates the filter with a new raw GPS observation.
     * @param rawLat Raw latitude from GPS
     * @param rawLon Raw longitude from GPS
     * @param accuracyMeters Accuracy estimate from Android Location (standard deviation)
     * @param timestampMs Epoch timestamp in ms
     * @return Pair of filtered (latitude, longitude) and estimated accuracy
     */
    fun update(
        rawLat: Double,
        rawLon: Double,
        accuracyMeters: Float,
        timestampMs: Long
    ): FilteredGpsResult {
        // Minimum measurement variance in degrees (~1m ≈ 0.000009 deg)
        val accuracyInDeg = (accuracyMeters.coerceAtLeast(0.5f) / 111320.0).toDouble()
        val measurementNoiseR = accuracyInDeg * accuracyInDeg

        if (variance < 0) {
            // First measurement initialization
            lat = rawLat
            lon = rawLon
            variance = measurementNoiseR
            lastTimestampMs = timestampMs
            return FilteredGpsResult(lat, lon, accuracyMeters)
        }

        // Time delta calculation for process noise adjustment
        val dtSec = ((timestampMs - lastTimestampMs).coerceIn(100L, 5000L)) / 1000.0
        lastTimestampMs = timestampMs

        // 1. Predict step: error covariance grows with elapsed time
        variance += processNoiseQ * dtSec

        // 2. Kalman Gain K = P / (P + R)
        val kalmanGain = variance / (variance + measurementNoiseR)

        // 3. Update state estimate with measurement
        lat += kalmanGain * (rawLat - lat)
        lon += kalmanGain * (rawLon - lon)

        // 4. Update error covariance
        variance = (1.0 - kalmanGain) * variance

        val estimatedAccuracyM = (sqrt(variance) * 111320.0).toFloat().coerceAtLeast(0.3f)

        return FilteredGpsResult(lat, lon, estimatedAccuracyM)
    }
}

data class FilteredGpsResult(
    val latitude: Double,
    val longitude: Double,
    val estimatedAccuracyMeters: Float
)
