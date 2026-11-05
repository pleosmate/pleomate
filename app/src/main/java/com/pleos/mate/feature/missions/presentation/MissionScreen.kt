package com.pleos.mate.feature.missions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.missions.domain.MissionSchedule
import com.pleos.mate.feature.missions.domain.vehicleMissions

@Composable
internal fun MissionCatalog(session: CompanionSession, onBack: () -> Unit, onStartWind: () -> Unit) {
    var section by rememberSaveable { mutableStateOf(0) }
    val completed = session.progress.hasDailyWind()
    val enabled = session.parkedAndAllowed
    val todayKey = MissionSchedule.today()
    val todayDoorCompleted = session.progress.hasDailyDoorCheck(todayKey)
    val weeklyDoorCompleted = session.progress.hasWeeklyDoorBonus()
    val weeklyDoorChecks = session.progress.doorChecksThisWeek()
    val busy = session.mission in setOf(CompanionSession.Mission.CHECKING, CompanionSession.Mission.WAITING, CompanionSession.Mission.VERIFYING, CompanionSession.Mission.SAVING) ||
        session.dailyDoorCheck == CompanionSession.DailyDoorCheck.CHECKING
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 52.dp)) { Text("‹  홈으로") }
        Text("차량 미션", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("일일·주간·한 번만 달성하는 미션으로 동료와 함께 성장해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("차와 함께", "기억 남기기", "차량 알아보기").forEachIndexed { index, label ->
                FilterChip(selected = section == index, onClick = { section = index }, label = { Text(label) })
            }
        }
        if (section == 0) {
        Text("오늘의 미션 · 하루 한 번", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MissionItem(
            title = "운전석 문 닫힘 확인",
            icon = R.drawable.ic_mission_lock,
            detail = if (session.dailyDoorCheck == CompanionSession.DailyDoorCheck.READY) "주차하면 운전석 문이 닫혔는지 자동으로 확인해요. 확인이 안 되면 여기서 다시 확인할 수 있어요." else session.dailyDoorMessage,
            reward = if (todayDoorCompleted) "오늘의 +5 XP 획득 완료" else "+5 XP · 하루 한 번",
            badge = when {
                todayDoorCompleted -> "오늘 완료"
                session.dailyDoorCheck == CompanionSession.DailyDoorCheck.CHECKING -> "확인 중"
                session.dailyDoorCheck == CompanionSession.DailyDoorCheck.NEEDS_ACTION -> "조치 후 확인"
                session.dailyDoorCheck == CompanionSession.DailyDoorCheck.ERROR -> "다시 확인"
                else -> "일일"
            },
            enabled = enabled && !todayDoorCompleted && !busy,
            onClick = session::startDailyDoorCheck,
            buttonTag = "start_daily_door_mission",
            buttonLabel = if (session.dailyDoorCheck in setOf(CompanionSession.DailyDoorCheck.NEEDS_ACTION, CompanionSession.DailyDoorCheck.ERROR)) "다시 확인" else "직접 확인"
        )

        Text("이번 주 · 서로 다른 3일", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MissionItem(
            title = "차와 함께한 한 주",
            icon = R.drawable.ic_mission_calendar,
            detail = "운전석 문 닫힘 확인을 서로 다른 3일 완료하면 주간 보너스를 받아요.",
            reward = if (weeklyDoorCompleted) "주간 +15 XP 획득 완료" else "$weeklyDoorChecks / 3일 · 달성 시 +15 XP",
            badge = if (weeklyDoorCompleted) "주간 완료" else "주간",
            enabled = false,
            onClick = {}
        )
        Text("매일 함께하는 창문 경험", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        MissionItem(
            title = "바람 느끼기",
            icon = R.drawable.ic_mission_wind,
            detail = "창문 열림은 자동 감지해요. 주행 중 감지되면 주차 후 첫 경험과 XP를 저장해요.",
            reward = if (completed) "오늘 완료" else if (session.progress.windCompletedAt == null) "첫 달성 +10 XP" else "+2 XP · 하루 한 번",
            badge = if (completed) "완료" else "참여 가능",
            enabled = enabled && !completed && !busy,
            onClick = onStartWind,
            buttonTag = "start_wind_mission",
            buttonLabel = "직접 확인"
        )
        if (session.mission in setOf(CompanionSession.Mission.CHECKING, CompanionSession.Mission.WAITING, CompanionSession.Mission.VERIFYING, CompanionSession.Mission.SAVING, CompanionSession.Mission.ERROR)) {
            Text(session.message, modifier = Modifier.testTag("mission_status"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy && session.mission != CompanionSession.Mission.SAVING) TextButton(
                onClick = { session.cancelMission("다음에 함께해도 괜찮아요.") }, modifier = Modifier.fillMaxWidth().testTag("cancel_wind_mission")
            ) { Text(stringResource(R.string.home_mission_later)) }
        }
        }
        if (section == 1) ExperienceMissions(session)
        if (section == 2) {
        Text("지원 차량 미션 · 첫 달성", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("내 차의 기능을 알아보세요. 지원 여부는 차량에 따라 달라요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        vehicleMissions.forEach { item ->
            val done = item.id in session.progress.completedVehicleMissions
            MissionItem(item.title, if (session.hasSeenGuide(item.id)) "이전에 확인한 기능이에요. 현재 상태를 다시 확인하거나 설명을 다시 볼 수 있어요." else item.detail, if (done) "+5 XP 획득 완료" else "+5 XP · 최초 1회",
                if (done) "완료" else "차량 조회", enabled = enabled && session.loaded && !busy && !session.vehicleMissionSaving && !session.experienceSaving && !session.memorySaving,
                onClick = { session.startVehicleMission(item.id) }, buttonLabel = if (done) "다시 조회" else "차량 정보 확인", buttonTag = "vehicle_mission_${item.id}", icon = vehicleMissionIcon(item.id))
        }
        }
        vehicleMissions.find { it.id == session.vehicleMissionId }?.let { item ->
            val done = item.id in session.progress.completedVehicleMissions
            var showGuide by rememberSaveable(item.id) { mutableStateOf(!session.hasSeenGuide(item.id)) }
            AlertDialog(
                modifier = Modifier.companionDialogBorder(),
                onDismissRequest = { if (!session.vehicleMissionSaving) session.cancelVehicleMission() },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(item.title) },
                text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (showGuide) Text(item.detail)
                    TextButton(onClick = { showGuide = !showGuide }) { Text(if (showGuide) "설명 접기" else "설명 다시 보기") }
                    session.vehicleMissionValue?.let { Text(it) }
                    Text(session.vehicleMissionMessage, modifier = Modifier.testTag("vehicle_mission_status"))
                } },
                confirmButton = {
                    if (done) TextButton(onClick = session::cancelVehicleMission) { Text("닫기") }
                    else TextButton(
                        enabled = session.vehicleMissionValue != null && !session.vehicleMissionReading && !session.vehicleMissionSaving,
                        onClick = session::completeVehicleMission
                    ) { Text(if (session.vehicleMissionSaving) "저장 중" else "확인 완료 · +5 XP") }
                },
                dismissButton = { Row {
                    TextButton(enabled = !session.vehicleMissionReading && !session.vehicleMissionSaving,
                        onClick = { session.startVehicleMission(item.id) }) { Text("다시 조회") }
                    if (!done) TextButton(enabled = !session.vehicleMissionSaving, onClick = session::cancelVehicleMission) { Text("닫기") }
                } }
            )
        }
    }
}

