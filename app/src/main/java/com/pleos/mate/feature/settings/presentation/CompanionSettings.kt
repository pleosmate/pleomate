package com.pleos.mate.feature.settings.presentation

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.CompanionDialog
import com.pleos.mate.core.designsystem.CompanionPageHeader
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.companion.presentation.PetWardrobe
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.journey.presentation.JourneyViewModel
import com.pleos.mate.feature.missions.domain.vehicleMissions

@Composable
internal fun CompanionSettings(session: CompanionSession, journeys: JourneyViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    var guides by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val canReset = session.loaded && journeys.loaded && session.parkedAndAllowed && !session.growthBusy() &&
        !session.vehicleMissionReading && !journeys.busy && !journeys.recording
    LaunchedEffect(canReset) { if (!canReset) confirming = false }
    CompanionPageHeader("설정", "동료의 모습과 목소리, 함께 쌓은 기록을 관리해요.")
    var wardrobe by rememberSaveable { mutableStateOf(false) }
    val companionControls: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Column(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("나의 동료", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("함께 자란 모습을 살펴보고 공간과 소품을 꾸며요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { wardrobe = !wardrobe }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (wardrobe) "꾸미기 접기" else "동료 꾸미기", fontWeight = FontWeight.Bold)
        }
    }
    if (wardrobe) PetWardrobe(session, journeys)
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().companionCardBackground().padding(20.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("동료 반응 음성", style = MaterialTheme.typography.titleMedium)
            Text("동료의 반응을 목소리로 들어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.Switch(checked = session.reactionVoiceEnabled,
            onCheckedChange = session::updateReactionVoiceEnabled, enabled = session.parkedAndAllowed)
    }
    OutlinedButton(onClick = { session.previewPetReaction(); onBack() }, enabled = session.parkedAndAllowed) {
        Text("홈에서 말풍선 미리보기")
    }
        }
    }
    val dataControls: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Column(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("기록 보관", style = MaterialTheme.typography.titleLarge)
        Text("이 차량의 앱 안에 펫·성장·기억·발견한 지역을 보관해요. 같은 차량을 사용하는 사람은 기록을 볼 수 있어요.")
        Text("탐험 지도는 지역 확인 시에만 위치를 사용하며, 좌표 대신 수집한 동네와 날짜만 저장해요. 앱 삭제나 초기화 시 기록도 삭제돼요.")
        journeys.editor?.let { Text("작성 중인 기억이 있어요. 도감에서 이어 쓸 수 있어요.") }
    }
    Column(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("차량 기능 안내", style = MaterialTheme.typography.titleLarge)
        Text("확인한 기능 ${session.seenVehicleGuides.size} / ${vehicleMissions.size} · 펫 레벨과 별도로 기억해요.")
        Text("한 번 확인한 기능은 반복 설명을 줄여요. 필요한 설명은 언제든 다시 볼 수 있어요.")
        OutlinedButton(onClick = { guides = !guides }) { Text(if (guides) "안내 접기" else "전체 안내 다시 보기") }
        if (guides) vehicleMissions.forEach {
            Text(it.title + if (session.hasSeenGuide(it.id)) " · 확인한 기능" else " · 첫 확인 전", style = MaterialTheme.typography.titleMedium)
            Text(it.detail)
        }
    }
    Column(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("데이터 초기화", style = MaterialTheme.typography.titleLarge)
        Text("펫 선택·이름, 레벨·경험치, 미션·배지, 모든 기억·탐험 지역·기념품과 작성 중인 내용, 차량 기능 안내 이력을 삭제해요. 삭제 후 복구할 수 없어요.")
        Text("초기화하면 앱이 종료됩니다. 다시 열면 첫 만남부터 시작해요.")
        if (!canReset) Text("주차 후 진행 중인 조회·저장을 마치고, 이동 관찰을 종료한 뒤 초기화할 수 있어요.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = { confirming = true }, enabled = canReset,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("reset_companion"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("모든 데이터 초기화") }
    }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 840.dp && !wardrobe) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1f)) { companionControls() }
                Box(Modifier.weight(1f)) { dataControls() }
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            companionControls()
            dataControls()
        }
    }
    if (confirming && canReset) CompanionDialog(
        modifier = Modifier.companionDialogBorder(), containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { confirming = false }, title = { Text("모든 기록을 초기화할까요?") },
        text = { Text("펫·성장·미션·기억·탐험 지도·기념품·작성 초안·안내 이력을 모두 지워요. 복구할 수 없으며 앱이 종료됩니다.") },
        confirmButton = { Button(onClick = {
            confirming = false
            val accepted = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            if (!accepted) error = "초기화를 시작하지 못했어요. 잠시 후 다시 시도해 주세요."
        }, modifier = Modifier.testTag("confirm_reset_companion")) { Text("전체 삭제 및 초기화") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("취소") } }
    )
}
