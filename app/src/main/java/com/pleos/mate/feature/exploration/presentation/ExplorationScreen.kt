package com.pleos.mate.feature.exploration.presentation

import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.FlatCard
import com.pleos.mate.core.pet.presentation.PetStage
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.exploration.data.Area
import com.pleos.mate.feature.exploration.data.ExplorationAreas
import com.pleos.mate.feature.missions.data.location.ExperienceLocationReader
import java.util.concurrent.Executors

@Composable
internal fun ExplorationScreen(session: CompanionSession) {
    val parked = session.parkedAndAllowed
    val context = LocalContext.current
    var candidate by remember { mutableStateOf<Area?>(null) }
    var selected by remember { mutableStateOf<Area?>(null) }
    var status by remember { mutableStateOf("주차한 곳에서 현재 동네를 확인하고 기념 조각을 모아 보세요.") }
    var busy by remember { mutableStateOf(false) }
    var checkedAt by remember { mutableLongStateOf(0L) }
    var generation by remember { mutableIntStateOf(0) }
    val reader = remember { ExperienceLocationReader(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val resolver = remember { lazy { ExplorationAreas(context.applicationContext) } }
    fun check() {
        if (!session.parkedAndAllowed || busy || session.growthBusy()) return
        candidate = null; busy = true; status = "현재 동네를 확인하고 있어요."
        val token = ++generation
        reader.readCoordinates({ lat, lon ->
            if (token == generation && !executor.isShutdown) {
                val receivedAt = SystemClock.elapsedRealtime()
                executor.execute {
                    val result = runCatching { resolver.value.resolve(lat, lon) }
                    handler.post {
                        if (token == generation) {
                            busy = false; candidate = result.getOrNull(); checkedAt = receivedAt
                            val area = candidate
                            status = when {
                                result.isFailure -> "지역 자료를 읽지 못했어요. 잠시 후 다시 확인해 주세요."
                                area == null -> "동네를 확정하기 어려워요. 경계 근처이거나 지원 지역 밖일 수 있어요."
                                session.progress.nextExplorationSouvenir(area.id, java.time.LocalDate.now().toString()) == null -> "오늘은 이미 조각을 모았거나 이 동네의 조각을 모두 모았어요."
                                else -> "${area.district} ${area.name}에 도착했어요! 오늘의 조각을 모아 보세요."
                            }
                            if (area != null) selected = area
                        }
                    }
                }
            }
        }, { error -> if (token == generation) { busy = false; status = error } })
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) check() else status = "위치 권한을 허용한 뒤 다시 시도해 주세요. 기존 여행책은 볼 수 있어요."
    }
    LaunchedEffect(parked) { if (!parked) { generation++; reader.cancel(); busy = false; candidate = null } }
    DisposableEffect(Unit) { onDispose { generation++; reader.cancel(); executor.shutdownNow() } }
    val discovery: @Composable () -> Unit = {
        FlatCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val area = selected
                if (area != null) {
                    Text(if (area.id.length == 10) "${area.district} ${area.name}" else area.name, style = MaterialTheme.typography.titleLarge)
                    if (area.id.length == 10) {
                        val records = session.progress.explorationSouvenirs.filter { it.startsWith("${area.id}|") }
                        Text("동네 기념 조각 ${records.size} / 3", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..3).forEach { slot ->
                                val found = records.any { it.split('|')[1] == slot.toString() }
                                Column(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    SouvenirImage(area.province, Modifier.size(48.dp), found)
                                    Text(if (found) listOf("첫 만남", "다시 만남", "소중한 추억")[slot - 1] else "? · $slot", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text("같은 동네에서도 하루에 한 조각씩 모을 수 있어요.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        if (area.id.length == 2) {
                            val place = explorationPlaces.firstOrNull { it.region == area.province }
                            if (place != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SouvenirImage(area.province, Modifier.size(56.dp), area.province in session.progress.exploredRegions)
                                    Text("${place.souvenir} · ${if (area.province in session.progress.exploredRegions) "발견 완료" else "아직 발견 전"}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Text("지도를 눌러 동네까지 들어가 보세요.", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                }
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_mission_pin), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(candidate?.let { "${it.district} ${it.name}" } ?: "오늘은 어디에 도착했나요?", style = MaterialTheme.typography.titleLarge)
                Text(if (parked) status else "주차 후 현재 지역을 확인할 수 있어요.", Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("exploration_status"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                val current = candidate
                if (current != null && session.progress.nextExplorationSouvenir(current.id, java.time.LocalDate.now().toString()) != null) {
                    Button(enabled = parked && !busy && !session.growthBusy(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("save_exploration"), onClick = {
                        if (SystemClock.elapsedRealtime() - checkedAt > 60000) { candidate = null; status = "시간이 지나 위치를 다시 확인해 주세요." }
                        else {
                            busy = true; val token = generation
                            session.collectExploration(current.id, current.province, { SystemClock.elapsedRealtime() - checkedAt <= 60000 }) { message ->
                                if (token == generation) { busy = false; candidate = null; status = message }
                            }
                        }
                    }) { Text(if (busy) "저장 중…" else if (current.province in session.progress.exploredRegions) "오늘의 조각 모으기 · +5 XP" else "첫 지역과 조각 모으기 · +10 XP") }
                }
                OutlinedButton(enabled = parked && !busy && !session.growthBusy(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("check_exploration"), onClick = {
                    if (context.checkSelfPermission("pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED) check() else permission.launch("pleos.car.permission.FUSED_LOCATION")
                }) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(painterResource(R.drawable.ic_mission_pin), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text(if (busy) "확인하고 있어요" else "현재 동네 확인")
                }
                Text("확인할 때만 위치를 사용해요. 좌표나 이동 경로는 보관하지 않고, 수집한 동네와 날짜만 이 차량에 남겨요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("우리의 여행 지도", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("함께 모은 동네 조각 ${session.progress.explorationSouvenirs.size}개", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PetStage(session.character, active = session.parkedAndAllowed, level = session.progress.level,
                outfit = session.outfit, modifier = Modifier.size(96.dp))
        }
        GeographicExplorationMap { selected = it }
        discovery()
        Text("행정구역 자료: 통계청 SGIS · 가공 vuski/admdongkor · CC BY 4.0 · 2026.10", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
