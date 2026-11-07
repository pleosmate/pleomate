package com.pleos.mate.feature.exploration.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

/** Reject locations near another region; a coastline alone is not an administrative ambiguity. */
internal class ProvinceResolver(context: Context) {
    private val features = JSONObject(context.assets.open("exploration/provinces-20261001.geojson").bufferedReader().use { it.readText() }).getJSONArray("features")

    fun resolve(latitude: Double, longitude: Double): String? {
        var match: String? = null
        val distances = mutableMapOf<String, Double>()
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val name = feature.getJSONObject("properties").getString("name")
            var regionDistance = Double.POSITIVE_INFINITY
            val geometry = feature.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            val polygons = if (geometry.getString("type") == "Polygon") JSONArray().put(coords) else coords
            for (j in 0 until polygons.length()) {
                val rings = polygons.getJSONArray(j)
                var inside = false
                for (k in 0 until rings.length()) {
                    val ring = rings.getJSONArray(k)
                    if (contains(ring, latitude, longitude)) inside = !inside
                    regionDistance = min(regionDistance, distance(ring, latitude, longitude))
                }
                if (inside) {
                    if (match != null && match != name) return null
                    match = name
                }
            }
            distances[name] = regionDistance
        }
        val found = match ?: return null
        return found.takeIf { distances.filterKeys { it != found }.values.none { it < 1500.0 } }
    }

    private fun contains(ring: JSONArray, lat: Double, lon: Double): Boolean {
        var inside = false
        var prev = ring.getJSONArray(ring.length() - 1)
        for (i in 0 until ring.length()) {
            val next = ring.getJSONArray(i)
            val x1 = prev.getDouble(0); val y1 = prev.getDouble(1)
            val x2 = next.getDouble(0); val y2 = next.getDouble(1)
            if ((y1 > lat) != (y2 > lat) && lon < (x2 - x1) * (lat - y1) / (y2 - y1) + x1) inside = !inside
            prev = next
        }
        return inside
    }

    private fun distance(ring: JSONArray, lat: Double, lon: Double): Double {
        val scaleX = 111_320.0 * cos(Math.toRadians(lat))
        val scaleY = 111_320.0
        var minDistance = Double.POSITIVE_INFINITY
        var prev = ring.getJSONArray(ring.length() - 1)
        for (i in 0 until ring.length()) {
            val next = ring.getJSONArray(i)
            val ax = (prev.getDouble(0) - lon) * scaleX; val ay = (prev.getDouble(1) - lat) * scaleY
            val bx = (next.getDouble(0) - lon) * scaleX; val by = (next.getDouble(1) - lat) * scaleY
            val dx = bx - ax; val dy = by - ay
            val length = dx * dx + dy * dy
            val t = if (length == 0.0) 0.0 else (-(ax * dx + ay * dy) / length).coerceIn(0.0, 1.0)
            minDistance = min(minDistance, hypot(ax + t * dx, ay + t * dy))
            prev = next
        }
        return minDistance
    }
}
