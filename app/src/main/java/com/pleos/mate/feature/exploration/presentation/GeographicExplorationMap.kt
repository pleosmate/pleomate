package com.pleos.mate.feature.exploration.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.mate.feature.exploration.data.Area
import com.pleos.mate.feature.exploration.data.ExplorationAreas
import com.pleos.mate.feature.exploration.data.contains
import com.pleos.mate.feature.exploration.data.provinces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private fun rootAreas(json: String, anchorJson: String): List<Area> {
    val anchors = JSONObject(anchorJson)
    val features = JSONObject(json).getJSONArray("features")
    return (0 until features.length()).mapNotNull { i ->
        val feature = features.getJSONObject(i); val name = feature.getJSONObject("properties").getString("name")
        val id = provinces.firstOrNull { it.second == name }?.first ?: return@mapNotNull null
        val geometry = feature.getJSONObject("geometry"); val coords = geometry.getJSONArray("coordinates")
        val polys = if (geometry.getString("type") == "Polygon") JSONArray().put(coords) else coords
        val polygons = (0 until polys.length()).map { p -> polys.getJSONArray(p).let { poly ->
            (0 until poly.length()).map { r -> poly.getJSONArray(r).let { ring ->
                (0 until ring.length()).map { n -> ring.getJSONArray(n).let { it.getDouble(0) to it.getDouble(1) } }
            } }
        } }
        val points = polygons.flatten().flatten()
        Area(id, explorationPlaces.firstOrNull { it.region == name }?.label ?: name, name, "", listOf(points.minOf { it.first }, points.minOf { it.second }, points.maxOf { it.first }, points.maxOf { it.second }), anchors.getJSONArray(name).let { it.getDouble(0) to it.getDouble(1) }, polygons)
    }
}

@Composable
internal fun GeographicExplorationMap(onNeighborhood: (Area?) -> Unit) {
    val context = LocalContext.current
    var province by remember { mutableStateOf<Area?>(null) }
    var district by remember { mutableStateOf<Area?>(null) }
    var areas by remember { mutableStateOf<List<Area>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(province?.id, district?.id) {
        loading = true
        areas = withContext(Dispatchers.IO) {
            val source = ExplorationAreas(context)
            when {
                district != null -> source.children("emd", district!!.id)
                province != null -> source.children("sgg", province!!.id)
                else -> rootAreas(context.assets.open("exploration/provinces-display.json").bufferedReader().use { it.readText() }, context.assets.open("exploration/province-anchors.json").bufferedReader().use { it.readText() })
            }
        }
        loading = false
    }
    fun select(area: Area) {
        when {
            province == null -> { loading = true; areas = emptyList(); province = area; onNeighborhood(area) }
            district == null -> { loading = true; areas = emptyList(); district = area; onNeighborhood(area) }
            else -> onNeighborhood(area)
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp).testTag("exploration_map"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(when { district != null -> "${province!!.name} · ${district!!.name}"; province != null -> province!!.name; else -> "우리의 발견 지도" }, style = MaterialTheme.typography.titleLarge)
        Text(when { district != null -> "동네를 눌러 조각을 살펴보세요"; province != null -> "시·군·구를 눌러 더 가까이 볼까요?"; else -> "지역을 눌러 가까이 볼까요?" }, style = MaterialTheme.typography.bodySmall)
        if (province != null) TextButton(onClick = { loading = true; areas = emptyList(); if (district != null) district = null else province = null; onNeighborhood(null) }) { Text("← 이전 지도") }
        if (loading || areas.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else {
            key(province?.id, district?.id) { AreaCanvas(areas, ::select) }
        }
        Text("이름표나 지역을 눌러 선택해요. 확대 후 지도를 끌어 움직일 수 있어요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class MapLabel(val area: Area, val anchor: Offset, val bounds: Rect)
private data class MapLayout(val scale: Double, val left: Double, val top: Double, val minX: Double, val maxY: Double, val labels: List<MapLabel>) {
    fun point(x: Double, y: Double) = Offset((left + (x - minX) * .82 * scale).toFloat(), (top + (maxY - y) * scale).toFloat())
}

private fun mapLayout(areas: List<Area>, width: Float, height: Float, zoom: Float, pan: Offset, textSize: Float, padding: Float): MapLayout {
    val minX = areas.minOf { it.bounds[0] }; val minY = areas.minOf { it.bounds[1] }
    val maxX = areas.maxOf { it.bounds[2] }; val maxY = areas.maxOf { it.bounds[3] }
    val scale = minOf(width / ((maxX - minX) * .82), height / (maxY - minY)) * .87 * zoom
    val left = (width - (maxX - minX) * .82 * scale) / 2 + pan.x
    val top = (height - (maxY - minY) * scale) / 2 + pan.y
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { this.textSize = textSize; isFakeBoldText = true }
    val labels = mutableListOf<MapLabel>()
    areas.forEach { area ->
        val anchor = Offset((left + (area.anchor.first - minX) * .82 * scale).toFloat(), (top + (maxY - area.anchor.second) * scale).toFloat())
        if (anchor.x < 0 || anchor.x > width || anchor.y < 0 || anchor.y > height) return@forEach
        val w = paint.measureText(area.name) + padding * 2
        val h = textSize * 1.55f
        val offsets = listOf(Offset(0f, 0f), Offset(0f, -h), Offset(0f, h), Offset(-w * .65f, -h), Offset(w * .65f, h), Offset(-w * .65f, h), Offset(w * .65f, -h))
        val rect = offsets.firstNotNullOfOrNull { offset ->
            val x = (anchor.x + offset.x - w / 2).coerceIn(0f, (width - w).coerceAtLeast(0f))
            val y = (anchor.y + offset.y - h / 2).coerceIn(0f, (height - h).coerceAtLeast(0f))
            val candidate = Rect(x, y, x + w, y + h)
            candidate.takeIf { labels.none { previous -> previous.bounds.inflate(padding / 2).overlaps(it) } }
        }
        if (rect != null) labels.add(MapLabel(area, anchor, rect))
    }
    return MapLayout(scale, left, top, minX, maxY, labels)
}

@Composable
private fun AreaCanvas(areas: List<Area>, onSelect: (Area) -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val textSize = with(density) { 14.sp.toPx() }
    val padding = with(density) { 8.dp.toPx() }
    val colors = listOf(Color(0xFFFFDCAD), Color(0xFFBEE2CA), Color(0xFFFFD1C2), Color(0xFFD4DDF4), Color(0xFFF3E2B5))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
            OutlinedButton(onClick = { val next = (zoom - .5f).coerceAtLeast(1f); pan *= (next - 1f) / (zoom - 1f); zoom = next }, enabled = zoom > 1f, modifier = Modifier.heightIn(min = 48.dp).testTag("map_zoom_out")) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { zoom = (zoom + .5f).coerceAtMost(3f) }, enabled = zoom < 3f, modifier = Modifier.heightIn(min = 48.dp).testTag("map_zoom_in")) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        val drag = if (zoom > 1f) Modifier.pointerInput(zoom) {
            detectDragGestures { change, amount ->
                change.consume()
                pan = Offset((pan.x + amount.x).coerceIn(-size.width * (zoom - 1f) / 2, size.width * (zoom - 1f) / 2),
                    (pan.y + amount.y).coerceIn(-size.height * (zoom - 1f) / 2, size.height * (zoom - 1f) / 2))
            }
        } else Modifier
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(26.dp)).background(Color(0xFFCDEBE7))
            .testTag("exploration_area_canvas").semantics { customActions = areas.map { area -> CustomAccessibilityAction(area.name) { onSelect(area); true } } }
            .then(drag).pointerInput(areas, zoom, pan, textSize) { detectTapGestures { tap ->
                val layout = mapLayout(areas, size.width.toFloat(), size.height.toFloat(), zoom, pan, textSize, padding)
                val label = layout.labels.lastOrNull { it.bounds.contains(tap) }
                val x = (tap.x - layout.left) / (layout.scale * .82) + layout.minX
                val y = layout.maxY - (tap.y - layout.top) / layout.scale
                val smallTarget = areas.filter { area -> (area.bounds[2] - area.bounds[0]) * .82 * layout.scale < 30.dp.toPx() && (area.bounds[3] - area.bounds[1]) * layout.scale < 30.dp.toPx() }
                    .minByOrNull { (layout.point(it.anchor.first, it.anchor.second) - tap).getDistance() }
                    ?.takeIf { (layout.point(it.anchor.first, it.anchor.second) - tap).getDistance() < 14.dp.toPx() }
                (label?.area ?: smallTarget ?: areas.firstOrNull { area -> area.polygons.any { poly -> poly.fold(false) { inside, ring -> inside xor contains(ring, x, y) } } })?.let(onSelect)
            } }) {
            val layout = mapLayout(areas, size.width, size.height, zoom, pan, textSize, padding)
            // A few quiet clouds and paw prints make the atlas feel like a shared travel journal.
            listOf(Offset(size.width * .09f, size.height * .1f), Offset(size.width * .89f, size.height * .18f), Offset(size.width * .12f, size.height * .86f)).forEach { cloud ->
                val white = Color(0xFFFFFCF2).copy(alpha = .7f)
                drawCircle(white, 13.dp.toPx(), cloud); drawCircle(white, 10.dp.toPx(), cloud + Offset(13.dp.toPx(), 3.dp.toPx()))
                drawRoundRect(white, cloud + Offset(-12.dp.toPx(), 2.dp.toPx()), Size(38.dp.toPx(), 12.dp.toPx()), CornerRadius(8.dp.toPx()))
            }
            areas.forEachIndexed { index, area ->
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    area.polygons.forEach { poly -> poly.forEach { ring -> ring.forEachIndexed { n, point ->
                        val p = layout.point(point.first, point.second)
                        if (n == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }; close() } }
                }
                drawPath(path, colors[index % colors.size]); drawPath(path, Color(0xFFFFFCF2), style = Stroke(2.5.dp.toPx()))
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(54, 57, 48); textAlign = android.graphics.Paint.Align.CENTER; this.textSize = textSize; isFakeBoldText = true
            }
            layout.labels.forEach { label ->
                val rect = label.bounds
                drawLine(Color(0xFF9E9B80).copy(alpha = .8f), label.anchor, rect.center, strokeWidth = 1.5.dp.toPx())
                drawRoundRect(Color(0xFF8C9C86).copy(alpha = .2f), Offset(rect.left, rect.top + 2.dp.toPx()), Size(rect.width, rect.height), CornerRadius(rect.height / 2))
                drawRoundRect(Color(0xFFFFFCF3), rect.topLeft, Size(rect.width, rect.height), CornerRadius(rect.height / 2))
                drawContext.canvas.nativeCanvas.drawText(label.area.name, rect.center.x, rect.center.y - (paint.ascent() + paint.descent()) / 2, paint)
            }
        }
        Text("이름이 겹치면 + 버튼으로 확대해 보세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
