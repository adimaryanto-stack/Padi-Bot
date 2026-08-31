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
import com.example.padibot.model.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceLocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val hasFix: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val hasPermission: Boolean = false,
    val provider: String = "Menunggu Sinyal...",
    val satellitesCount: Int = 0,
    val satellitesUsed: Int = 0,
    val timestamp: Long = 0L
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    val isAccurate: Boolean get() = hasFix && accuracyMeters in 0.01f..15.0f
}

class DeviceLocationService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _locationState = MutableStateFlow(DeviceLocationState())
    val locationState: StateFlow<DeviceLocationState> = _locationState.asStateFlow()

    private var isListening = false
    private var gnssCallback: GnssStatus.Callback? = null

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
        checkStatus()
        startLocationUpdates()
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsHardwareEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    fun checkStatus() {
        val hasPerm = hasLocationPermission()
        val isGpsOn = isGpsHardwareEnabled()

        _locationState.value = _locationState.value.copy(
            hasPermission = hasPerm,
            isGpsEnabled = isGpsOn
        )

        if (hasPerm && !_locationState.value.hasFix) {
            fetchLastKnown()
        }
    }

    fun startLocationUpdates() {
        val hasPerm = hasLocationPermission()
        val isGpsOn = isGpsHardwareEnabled()

        _locationState.value = _locationState.value.copy(
            hasPermission = hasPerm,
            isGpsEnabled = isGpsOn
        )

        if (!hasPerm || locationManager == null) return

        try {
            fetchLastKnown()

            if (!isListening) {
                // Register GPS Provider (High accuracy satellite fix for fields)
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        800L,   // 800ms updates
                        0.5f,   // 0.5 meter threshold
                        locationListener,
                        Looper.getMainLooper()
                    )
                }

                // Register Network Provider (fast coarse backup)
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1200L,
                        1.0f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }

                // Register GNSS Status Callback if available (API 24+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    gnssCallback = object : GnssStatus.Callback() {
                        override fun onSatelliteStatusChanged(status: GnssStatus) {
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
                        }
                    }
                    try {
                        locationManager.registerGnssStatusCallback(gnssCallback!!, null)
                    } catch (_: SecurityException) {}
                }

                isListening = true
            }
        } catch (_: SecurityException) {
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
        } catch (_: SecurityException) {}
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
        } catch (_: SecurityException) {}
    }

    private fun updateFromLocation(location: Location) {
        val providerName = when (location.provider) {
            LocationManager.GPS_PROVIDER -> "GPS Satelit"
            LocationManager.NETWORK_PROVIDER -> "Jaringan / Wi-Fi"
            else -> location.provider ?: "GPS"
        }

        _locationState.value = _locationState.value.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracyMeters = location.accuracy,
            speedMps = location.speed,
            bearingDeg = location.bearing,
            hasFix = true,
            hasPermission = true,
            isGpsEnabled = true,
            provider = providerName,
            timestamp = location.time
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
