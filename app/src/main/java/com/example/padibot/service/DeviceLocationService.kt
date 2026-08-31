package com.example.padibot.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.padibot.algorithm.GpsKalmanFilter
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

enum class GpsPrecisionGrade(val label: String, val colorHex: Long) {
    ULTRA("Sangat Tinggi (Sub-meter)", 0xFF059669),
    HIGH("Tinggi (1 - 3m)", 0xFF10B981),
    MEDIUM("Sedang (3 - 8m)", 0xFFF59E0B),
    LOW("Rendah (> 8m)", 0xFFEF4444),
    SEARCHING("Mencari Sinyal Satelit...", 0xFF6B7280)
}

data class DeviceLocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rawLatitude: Double = 0.0,
    val rawLongitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val filteredAccuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val hasFix: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val hasPermission: Boolean = false,
    val provider: String = "Menunggu Sinyal...",
    val satellitesCount: Int = 0,
    val satellitesUsed: Int = 0,
    val precisionGrade: GpsPrecisionGrade = GpsPrecisionGrade.SEARCHING,
    val bufferedSamplesCount: Int = 0,
    val timestamp: Long = 0L
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    val isAccurate: Boolean get() = hasFix && accuracyMeters in 0.01f..10.0f
}

data class AveragingResult(
    val point: GeoPoint,
    val sampleCount: Int,
    val standardDeviationMeters: Double,
    val estimatedAccuracyMeters: Double
)

class DeviceLocationService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val locationManager: LocationManager? = try {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    } catch (_: Throwable) {
        null
    }

    private val _locationState = MutableStateFlow(DeviceLocationState())
    val locationState: StateFlow<DeviceLocationState> = _locationState.asStateFlow()

    private var isListening = false
    private var gnssCallback: GnssStatus.Callback? = null

    // Kalman Filter for real-time noise reduction
    private val kalmanFilter = GpsKalmanFilter()

    // Circular buffer of recent fixes for high-accuracy surveying
    private val recentFixes = ArrayDeque<Location>(30)

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateFromLocation(location)
        }

        override fun onProviderEnabled(provider: String) {
            checkStatus()
        }

        override fun onProviderDisabled(provider: String) {
            checkStatus()
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            checkStatus()
        }
    }

    init {
        try {
            checkStatus()
            startLocationUpdates()
        } catch (_: Throwable) {}
    }

    fun hasLocationPermission(): Boolean {
        return try {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fine || coarse
        } catch (_: Throwable) {
            false
        }
    }

    fun isGpsHardwareEnabled(): Boolean {
        return try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Throwable) {
            false
        }
    }

    fun checkStatus() {
        try {
            val hasPerm = hasLocationPermission()
            val isGpsOn = isGpsHardwareEnabled()

            _locationState.value = _locationState.value.copy(
                hasPermission = hasPerm,
                isGpsEnabled = isGpsOn
            )

            if (hasPerm && !_locationState.value.hasFix) {
                fetchLastKnown()
            }
        } catch (_: Throwable) {}
    }

    fun startLocationUpdates() {
        try {
            val hasPerm = hasLocationPermission()
            val isGpsOn = isGpsHardwareEnabled()

            _locationState.value = _locationState.value.copy(
                hasPermission = hasPerm,
                isGpsEnabled = isGpsOn
            )

            if (!hasPerm || locationManager == null) return

            fetchLastKnown()

            if (!isListening) {
                // Register GPS Provider with highest precision frequency (500ms, 0m)
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        500L,
                        0.0f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }

                // Register Network Provider as backup
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        0.5f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }

                // Register GNSS Status Callback if available (API 24+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    gnssCallback = object : GnssStatus.Callback() {
                        override fun onSatelliteStatusChanged(status: GnssStatus) {
                            try {
                                val totalSats = status.satelliteCount
                                var usedSats = 0
                                for (i in 0 until totalSats) {
                                    if (status.usedInFix(i)) {
                                        usedSats++
                                    }
                                }
                                _locationState.value = _locationState.value.copy(
                                    satellitesCount = totalSats,
                                    satellitesUsed = usedSats
                                )
                            } catch (_: Throwable) {}
                        }
                    }
                    try {
                        locationManager.registerGnssStatusCallback(gnssCallback!!, null)
                    } catch (_: Throwable) {}
                }

                isListening = true
            }
        } catch (_: Throwable) {
            _locationState.value = _locationState.value.copy(hasPermission = false)
        }
    }

    fun stopLocationUpdates() {
        if (!isListening || locationManager == null) return
        try {
            locationManager.removeUpdates(locationListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
                locationManager.unregisterGnssStatusCallback(gnssCallback!!)
            }
            isListening = false
        } catch (_: Throwable) {}
    }

    private fun fetchLastKnown() {
        if (locationManager == null || !hasLocationPermission()) return
        try {
            var bestLoc: Location? = null
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                bestLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (bestLoc == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                bestLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (bestLoc == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                bestLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
            bestLoc?.let { updateFromLocation(it) }
        } catch (_: Throwable) {}
    }

    private fun updateFromLocation(location: Location) {
        val providerName = when (location.provider) {
            LocationManager.GPS_PROVIDER -> "GPS Satelit"
            LocationManager.NETWORK_PROVIDER -> "Jaringan / Wi-Fi"
            else -> location.provider ?: "GPS"
        }

        // Apply Kalman filter for smoothed coordinate tracking
        val filtered = kalmanFilter.update(
            rawLat = location.latitude,
            rawLon = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5.0f,
            timestampMs = location.time
        )

        // Store into rolling buffer for multi-sample averaging if accuracy is decent (< 15m)
        val acc = if (location.hasAccuracy()) location.accuracy else 5.0f
        if (acc <= 15.0f) {
            if (recentFixes.size >= 30) {
                recentFixes.removeFirst()
            }
            recentFixes.addLast(location)
        }

        // Determine precision grade
        val grade = when {
            acc <= 1.5f -> GpsPrecisionGrade.ULTRA
            acc <= 3.5f -> GpsPrecisionGrade.HIGH
            acc <= 8.0f -> GpsPrecisionGrade.MEDIUM
            else -> GpsPrecisionGrade.LOW
        }

        _locationState.value = _locationState.value.copy(
            latitude = filtered.latitude,
            longitude = filtered.longitude,
            rawLatitude = location.latitude,
            rawLongitude = location.longitude,
            altitude = location.altitude,
            accuracyMeters = acc,
            filteredAccuracyMeters = filtered.estimatedAccuracyMeters,
            speedMps = location.speed,
            bearingDeg = location.bearing,
            hasFix = true,
            hasPermission = true,
            isGpsEnabled = true,
            provider = providerName,
            precisionGrade = grade,
            bufferedSamplesCount = recentFixes.size,
            timestamp = location.time
        )
    }

    /**
     * Computes high-precision multi-sample weighted average coordinates
     * using inverse-variance weighting based on recorded GPS fixes.
     */
    fun computeAveragedHighPrecisionPoint(maxSamples: Int = 15): AveragingResult? {
        val list = recentFixes.takeLast(maxSamples)
        if (list.isEmpty()) {
            val cur = _locationState.value
            return if (cur.hasFix) {
                AveragingResult(
                    point = GeoPoint(cur.latitude, cur.longitude),
                    sampleCount = 1,
                    standardDeviationMeters = cur.accuracyMeters.toDouble(),
                    estimatedAccuracyMeters = cur.filteredAccuracyMeters.toDouble()
                )
            } else null
        }

        var totalWeight = 0.0
        var weightedLat = 0.0
        var weightedLon = 0.0

        for (loc in list) {
            val sigma = if (loc.hasAccuracy()) loc.accuracy.toDouble().coerceAtLeast(0.5) else 5.0
            val weight = 1.0 / (sigma * sigma)
            totalWeight += weight
            weightedLat += loc.latitude * weight
            weightedLon += loc.longitude * weight
        }

        val finalLat = weightedLat / totalWeight
        val finalLon = weightedLon / totalWeight
        val finalPoint = GeoPoint(finalLat, finalLon)

        // Calculate standard deviation across samples in meters
        var varianceSum = 0.0
        for (loc in list) {
            val dist = PolygonMath.distanceBetweenMeters(finalPoint, GeoPoint(loc.latitude, loc.longitude))
            varianceSum += dist * dist
        }
        val stdDevMeters = sqrt(varianceSum / list.size)
        val estimatedErrorMeters = (stdDevMeters / sqrt(list.size.toDouble())).coerceAtLeast(0.2)

        return AveragingResult(
            point = finalPoint,
            sampleCount = list.size,
            standardDeviationMeters = stdDevMeters,
            estimatedAccuracyMeters = estimatedErrorMeters
        )
    }

    fun getLatestGeoPoint(): GeoPoint? {
        val s = _locationState.value
        return if (s.hasFix && s.latitude != 0.0 && s.longitude != 0.0) {
            GeoPoint(s.latitude, s.longitude)
        } else {
            null
        }
    }
}
