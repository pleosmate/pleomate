package com.pleos.mate.feature.missions.presentation

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.CompanionDialog
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.core.designsystem.companionTextFieldColors
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.MissionSchedule

@Composable
internal fun ExperienceMissions(session: CompanionSession) {
    val experiences = session.experiences
    val context = LocalContext.current
    var pendingLocationKind by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionResult by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pendingPlaceName by rememberSaveable { mutableStateOf("") }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionResult = granted
    }
    LaunchedEffect(session.loaded, session.parkedAndAllowed, session.growthBusy(), pendingLocationKind, permissionResult) {
        val kind = pendingLocationKind?.let(ExperienceKind::valueOf)
        val granted = permissionResult
        if (kind != null && granted != null && session.loaded && session.parkedAndAllowed && !session.growthBusy()) {
            experiences.open(kind)
            if (kind == ExperienceKind.PLACE) experiences.edit(pendingPlaceName)
            if (granted) experiences.queryLocation()
            else experiences.problem("위치 권한이 없어 확인할 수 없어요. 설정에서 권한을 허용한 뒤 다시 시도해 주세요.")
            pendingLocationKind = null
            permissionResult = null
            pendingPlaceName = ""
        }
    }
    Text("일일 미션 · 오늘의 기억", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    ExperienceKind.entries.filter { it != ExperienceKind.GREETING }.forEach { kind ->
        val done = session.progress.hasExperience(kind)
        val detail = when (kind) {
            ExperienceKind.PLACE -> "주차 후 현재 위치를 확인하고 장소 이름을 남겨요."
            ExperienceKind.DESTINATION -> "목적지 근처에서 주차 후 내비게이션과 위치를 함께 확인해요."
            ExperienceKind.NOTE -> "오늘의 한마디를 말하거나 직접 입력해서 기억으로 남겨요."
            ExperienceKind.GREETING -> "첫 만남 미션에서 동료의 목소리를 듣고 인사해요."
        }
        MissionItem(kind.title, detail,
            if (done) "+${kind.xp} XP 획득 완료" else "+${kind.xp} XP · 하루 한 번",
            if (done) "오늘 완료" else "일일",
            enabled = session.loaded && session.parkedAndAllowed && !session.growthBusy(),
            onClick = { experiences.open(kind) }, buttonLabel = if (done) "기억 확인" else "함께하기", buttonTag = "experience_${kind.name.lowercase()}", icon = experienceMissionIcon(kind))
    }
    Text("첫 만남 · 한 번만", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    val greetingDone = session.progress.hasExperience(ExperienceKind.GREETING)
    MissionItem(ExperienceKind.GREETING.title, "동료의 인사를 듣고 마이크로 인사해요.",
        if (greetingDone) "+5 XP 획득 완료" else "+5 XP · 최초 1회", if (greetingDone) "완료" else "첫 인사",
        enabled = session.loaded && session.parkedAndAllowed && !session.growthBusy(),
        onClick = { experiences.open(ExperienceKind.GREETING) }, buttonLabel = if (greetingDone) "기억 확인" else "함께 인사하기",
        buttonTag = "experience_greeting", icon = experienceMissionIcon(ExperienceKind.GREETING))
    val week = MissionSchedule.week(MissionSchedule.today())
    val days = ExperienceRules.weeklyDays(session.progress.companionMemories, week)
    val weeklyDone = week in ExperienceRules.bonusWeeks(session.progress.companionMemories)
    Text("주간 미션 · 서로 다른 3일", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    MissionItem("이번 주 추억 모으기", "일일 기억 미션 중 하나를 서로 다른 3일에 완료하면 자동으로 달성돼요.",
        if (weeklyDone) "주간 +15 XP 획득 완료" else "${days.coerceAtMost(3)} / 3일 · +15 XP", if (weeklyDone) "주간 완료" else "주간",
        enabled = false, onClick = {}, icon = R.drawable.ic_mission_memory)

    experiences.selected?.let { kind ->
        val done = session.progress.hasExperience(kind)
        val memory = session.progress.companionMemories.lastOrNull { it.kind == kind && (kind == ExperienceKind.GREETING || it.date == MissionSchedule.today()) }
        CompanionDialog(
            modifier = Modifier.companionDialogBorder(),
            onDismissRequest = { if (!session.experienceSaving) experiences.cancel() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(kind.title) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (done) {
                        memory?.let { Text(it.text); Text(it.date) }
                        Text("경험치를 받은 기억이에요. 도감에서도 다시 볼 수 있어요.")
                    } else {
                        Text(experiences.status, modifier = Modifier.testTag("experience_status"))
                        if (kind in listOf(ExperienceKind.PLACE, ExperienceKind.NOTE)) {
                            OutlinedTextField(shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                                colors = companionTextFieldColors(), value = experiences.text, onValueChange = experiences::edit,
                                enabled = !experiences.busy && !session.experienceSaving,
                                placeholder = { Text(if (kind == ExperienceKind.PLACE) "장소 이름" else "오늘의 한마디") },
                                supportingText = { Text("${experiences.text.length} / ${ExperienceRules.MAX_TEXT}") },
                                modifier = Modifier.fillMaxWidth().testTag("experience_text"))
                        } else if (experiences.text.isNotBlank()) Text(experiences.text)
                        if (kind in listOf(ExperienceKind.PLACE, ExperienceKind.DESTINATION)) {
                            TextButton(enabled = !experiences.busy && !session.experienceSaving, onClick = {
                                if (context.checkSelfPermission("pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED) experiences.queryLocation()
                                else {
                                    pendingLocationKind = kind.name
                                    pendingPlaceName = experiences.text
                                    permissionResult = null
                                    locationPermission.launch("pleos.car.permission.FUSED_LOCATION")
                                }
                            }) { Text(if (kind == ExperienceKind.DESTINATION) "목적지 도착 확인" else "현재 위치 확인") }
                        } else {
                            TextButton(enabled = !experiences.busy && !session.experienceSaving, onClick = experiences::startVoice) {
                                Text(if (kind == ExperienceKind.GREETING) "인사 듣고 말하기" else "마이크로 말하기")
                            }
                        }
                        if (experiences.busy) TextButton(onClick = { experiences.open(kind) }) { Text("확인 중단") }
                    }
                }
            },
            confirmButton = {
                if (done) TextButton(onClick = experiences::cancel) { Text("닫기") }
                else TextButton(enabled = experiences.canSave, onClick = experiences::save) {
                    Text(if (session.experienceSaving) "저장 중" else "기억 저장 · +${kind.xp} XP")
                }
            },
            dismissButton = { if (!done) TextButton(enabled = !session.experienceSaving, onClick = experiences::cancel) { Text("나중에") } }
        )
    }
}
