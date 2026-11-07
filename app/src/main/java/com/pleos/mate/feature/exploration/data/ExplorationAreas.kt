package com.pleos.mate.feature.exploration.data

import android.content.Context
import org.json.JSONArray
import kotlin.math.*

internal data class Area(
    val id: String, val name: String, val province: String, val district: String,
    val bounds: List<Double>, val anchor: Pair<Double, Double>, val polygons: List<List<List<Pair<Double, Double>>>>
)

internal class ExplorationAreas(private val context: Context) {
    private val provinceResolver by lazy { ProvinceResolver(context) }

    fun children(level: String, parent: String): List<Area> {
        val json = JSONArray(context.assets.open("exploration/areas/$level-$parent.json").bufferedReader().use { it.readText() })
        return (0 until json.length()).map { i ->
            val item = json.getJSONObject(i)
            val coords = item.getJSONArray("coordinates")
            Area(item.getString("id"), item.getString("name"), item.getString("province"), item.getString("district"),
                item.getJSONArray("bounds").let { b -> (0..3).map { b.getDouble(it) } },
                item.getJSONArray("anchor").let { it.getDouble(0) to it.getDouble(1) },
                (0 until coords.length()).map { p -> coords.getJSONArray(p).let { poly ->
                    (0 until poly.length()).map { r -> poly.getJSONArray(r).let { ring ->
                        (0 until ring.length()).map { n -> ring.getJSONArray(n).let { it.getDouble(0) to it.getDouble(1) } }
                    } }
                } })
        }
    }

    fun resolve(lat: Double, lon: Double): Area? {
        val province = provinceResolver.resolve(lat, lon) ?: return null
        val provinceId = provinces.firstOrNull { it.second == province }?.first ?: return null
        val district = match(children("sgg", provinceId), lat, lon) ?: return null
        return match(children("emd", district.id), lat, lon)
    }

    private fun match(areas: List<Area>, lat: Double, lon: Double): Area? {
        val matches = areas.filter { a ->
            lon >= a.bounds[0] && lon <= a.bounds[2] && lat >= a.bounds[1] && lat <= a.bounds[3] &&
                a.polygons.any { poly -> poly.fold(false) { inside, ring -> inside xor contains(ring, lon, lat) } }
        }
        val area = matches.singleOrNull() ?: return null
        // Do not award a potentially wrong neighborhood near an administrative boundary.
        return area.takeIf { a -> a.polygons.flatMap { it }.minOf { distance(it, lat, lon) } >= 50.0 }
    }
}

internal val provinces = listOf(
    "11" to "서울특별시", "28" to "인천광역시", "41" to "경기도", "51" to "강원특별자치도",
    "44" to "충청남도", "36" to "세종특별자치시", "43" to "충청북도", "47" to "경상북도",
    "52" to "전북특별자치도", "30" to "대전광역시", "27" to "대구광역시", "31" to "울산광역시",
    "12" to "전남광주통합특별시", "48" to "경상남도", "26" to "부산광역시", "50" to "제주특별자치도"
)

internal fun contains(ring: List<Pair<Double, Double>>, x: Double, y: Double): Boolean {
    var inside = false
    var previous = ring.last()
    for (point in ring) {
        if ((point.second > y) != (previous.second > y) && x < (previous.first - point.first) * (y - point.second) / (previous.second - point.second) + point.first) inside = !inside
        previous = point
    }
    return inside
}

private fun distance(ring: List<Pair<Double, Double>>, lat: Double, lon: Double): Double {
    val sx = 111320.0 * cos(Math.toRadians(lat)); val sy = 111320.0
    var result = Double.POSITIVE_INFINITY
    var previous = ring.last()
    for (point in ring) {
        val ax = (previous.first - lon) * sx; val ay = (previous.second - lat) * sy
        val dx = (point.first - previous.first) * sx; val dy = (point.second - previous.second) * sy
        val length = dx * dx + dy * dy
        val t = if (length == 0.0) 0.0 else (-(ax * dx + ay * dy) / length).coerceIn(0.0, 1.0)
        result = min(result, hypot(ax + t * dx, ay + t * dy)); previous = point
    }
    return result
}
