package com.example.padibot.service

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

enum class MapTileLayer(val title: String, val icon: String) {
    SATELLITE("Satelit", "🛰️"),
    STREET("Peta Jalan", "🗺️"),
    TOPO("Topografi", "⛰️"),
    HYBRID_GIS("Grid Agronomi", "🌾")
}

object MapTileProvider {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val memoryCache = LruCache<String, ImageBitmap>(80)
    
    // Reactive tile observer map for Compose UI trigger
    val tileStateMap = mutableStateMapOf<String, ImageBitmap>()

    fun getTileUrl(layer: MapTileLayer, z: Int, x: Int, y: Int): String {
        return when (layer) {
            MapTileLayer.SATELLITE ->
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
            MapTileLayer.STREET ->
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            MapTileLayer.TOPO ->
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/$z/$y/$x"
            MapTileLayer.HYBRID_GIS ->
                "https://tile.openstreetmap.org/$z/$x/$y.png"
        }
    }

    fun getTile(layer: MapTileLayer, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${layer.name}_${z}_${x}_$y"
        val cached = memoryCache.get(key)
        if (cached != null) {
            return cached
        }

        // Trigger asynchronous download
        fetchTileAsync(layer, z, x, y, key)
        return null
    }

    private fun fetchTileAsync(layer: MapTileLayer, z: Int, x: Int, y: Int, key: String) {
        if (tileStateMap.containsKey(key)) return // Already in progress or fetched

        scope.launch {
            try {
                val urlString = getTileUrl(layer, z, x, y)
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "PadiBot-GIS-App/1.0 (Android Agricultural Autonomous System)")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream.close()

                    if (bitmap != null) {
                        val imageBitmap = bitmap.asImageBitmap()
                        memoryCache.put(key, imageBitmap)
                        withContext(Dispatchers.Main) {
                            tileStateMap[key] = imageBitmap
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently fail, vector/synthetic agricultural canvas fallback will render
            }
        }
    }

    // Coordinate conversion utilities
    fun lonToTileX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * (1 shl zoom)
    }

    fun latToTileY(lat: Double, zoom: Int): Double {
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val rad = Math.toRadians(clampedLat)
        return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * (1 shl zoom)
    }

    fun tileXToLon(x: Double, zoom: Int): Double {
        return x / (1 shl zoom) * 360.0 - 180.0
    }

    fun tileYToLat(y: Double, zoom: Int): Double {
        val n = Math.PI - 2.0 * Math.PI * y / (1 shl zoom)
        return Math.toDegrees(atan(sinh(n)))
    }
}
