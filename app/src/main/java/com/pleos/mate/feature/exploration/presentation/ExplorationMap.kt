package com.pleos.mate.feature.exploration.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pleos.mate.R

internal data class ExplorationPlace(val region: String, val label: String, val souvenir: String, val x: Float, val y: Float)

// Display positions are deliberately spaced: this is a collection atlas, not a navigation map.
internal val explorationPlaces = listOf(
    ExplorationPlace("서울특별시", "서울", "궁궐의 작은 문", .40f, .12f),
    ExplorationPlace("인천광역시", "인천", "바다를 비추는 등대", .10f, .23f),
    ExplorationPlace("경기도", "경기", "고요한 도자기", .47f, .27f),
    ExplorationPlace("강원특별자치도", "강원", "숲을 품은 산", .83f, .11f),
    ExplorationPlace("충청남도", "충남", "서해의 조개", .09f, .40f),
    ExplorationPlace("세종특별자치시", "세종", "이야기를 쓰는 붓", .39f, .43f),
    ExplorationPlace("충청북도", "충북", "햇살 머금은 사과", .70f, .35f),
    ExplorationPlace("경상북도", "경북", "오래된 누각", .94f, .46f),
    ExplorationPlace("전북특별자치도", "전북", "바람 담은 부채", .10f, .57f),
    ExplorationPlace("대전광역시", "대전", "호기심의 과학탑", .44f, .59f),
    ExplorationPlace("대구광역시", "대구", "봄날의 사과꽃", .73f, .62f),
    ExplorationPlace("울산광역시", "울산", "푸른 바다 고래", 1f, .73f),
    ExplorationPlace("전남광주통합특별시", "전남·광주", "초록빛 차 한 잔", .08f, .74f),
    ExplorationPlace("경상남도", "경남", "붉은 동백꽃", .43f, .77f),
    ExplorationPlace("부산광역시", "부산", "갈매기가 쉬는 다리", .73f, .88f),
    ExplorationPlace("제주특별자치도", "제주", "섬의 작은 귤", .10f, .96f)
)

@Composable
internal fun SouvenirImage(region: String, modifier: Modifier = Modifier, discovered: Boolean = true, slot: Int = 0, variantKey: String = region) {
    val index = explorationPlaces.indexOfFirst { it.region == region }
    if (index < 0) return
    val sheet = ImageBitmap.imageResource(R.drawable.exploration_souvenirs_v1)
    val silhouette = when (slot) { 1 -> "pixel gate"; 2 -> "pixel bottle"; 3 -> "pixel flower charm"; else -> null }
    Canvas(modifier.then(if (silhouette == null) Modifier else Modifier.semantics { contentDescription = silhouette })) {
        val cellWidth = sheet.width / 4
        val cellHeight = sheet.height / 4
        drawImage(sheet, srcOffset = IntOffset(index % 4 * cellWidth, index / 4 * cellHeight),
            srcSize = IntSize(cellWidth, cellHeight), dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = if (discovered) 1f else .62f,
            colorFilter = if (discovered) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
        if (slot in 1..3) {
            val ink = Color(0xFF493C36)
            val accent = listOf(Color(0xFFF6C85F), Color(0xFF79C8B0), Color(0xFFF28B73), Color(0xFFB8A0E2), Color(0xFF88B9E8))[
                (variantKey.hashCode() and Int.MAX_VALUE) % 5]
            val u = size.width / 12f
            val rect = { x: Int, y: Int, w: Int, h: Int, color: Color ->
                drawRect(color, Offset(x * u, y * u), Size(w * u, h * u))
            }
            when (slot) {
                1 -> { // little gate
                    rect(3, 3, 6, 2, ink); rect(2, 5, 2, 5, ink); rect(8, 5, 2, 5, ink)
                    rect(4, 5, 4, 4, accent); rect(5, 7, 2, 3, ink)
                }
                2 -> { // keepsake bottle
                    rect(5, 2, 2, 2, ink); rect(4, 4, 4, 1, ink); rect(3, 5, 6, 5, ink)
                    rect(4, 6, 4, 3, accent); rect(5, 6, 2, 1, Color(0xFFFFF2C2))
                }
                3 -> { // flower charm
                    rect(5, 5, 2, 2, accent); rect(5, 2, 2, 3, ink); rect(5, 7, 2, 3, ink)
                    rect(2, 5, 3, 2, ink); rect(7, 5, 3, 2, ink)
                    rect(3, 5, 2, 2, accent); rect(7, 5, 2, 2, accent); rect(5, 8, 2, 2, Color(0xFF62966D))
                }
            }
            // A tiny deterministic pixel ornament makes nearby neighborhoods distinct.
            val ornament = variantKey.hashCode().ushr(4) % 3
            rect(1 + ornament, 1, 1, 1, accent)
            rect(10 - ornament, 2, 1, 1, Color(0xFFFFF2C2))
            if (!discovered) drawRect(Color(0xFF69716A).copy(alpha = .55f), Offset.Zero, size)
        }
    }
}

@Composable
internal fun ExplorationMap(regions: Set<String>, selected: String?, onSelect: (String) -> Unit) {
    CompactExplorationMap(regions, selected, onSelect)
}

@Composable
private fun CompactExplorationMap(regions: Set<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp).testTag("exploration_map"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("동료의 여행책 · ${regions.size} / ${explorationPlaces.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("지역을 눌러 기념품을 살펴보세요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        explorationPlaces.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { place ->
                    val found = place.region in regions
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (selected == place.region) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .semantics(mergeDescendants = true) { contentDescription = "${place.label}, ${if (found) "발견한 지역" else "아직 방문 전"}, ${place.souvenir}" }
                        .clickable(role = Role.Button) { onSelect(place.region) }.testTag("exploration_place_${place.label}").padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        SouvenirImage(place.region, Modifier.size(56.dp), found)
                        Text(place.label, style = MaterialTheme.typography.labelSmall, color = if (found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

