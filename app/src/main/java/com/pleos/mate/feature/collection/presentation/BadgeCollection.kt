package com.pleos.mate.feature.collection.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.vehicleMissions
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private data class CompanionBadge(
    val id: String,
    val title: String,
    val description: String,
    val requirement: String,
    val image: Int,
    val earned: Boolean,
    val earnedAt: Long? = null
)

@Composable
internal fun BadgeCollection(progress: GrowthProgress, archive: JourneyArchive) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<CompanionBadge?>(null) }
    val badges = remember(progress, archive) { createBadges(progress, archive) }
    val earnedCount = badges.count { it.earned }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("모은 배지", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$earnedCount / ${badges.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 720.dp) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                badges.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { badge ->
                            BadgeTile(badge, Modifier.weight(1f)) { selected = badge }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    selected?.let { badge ->
        AlertDialog(
            modifier = Modifier.companionDialogBorder(),
            onDismissRequest = { selected = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(badge.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(badge.image),
                        contentDescription = badge.title,
                        modifier = Modifier.size(176.dp).alpha(if (badge.earned) 1f else 0.38f)
                    )
                    Text(if (badge.earned) "획득한 배지" else "아직 잠겨 있어요", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(badge.description)
                    Text("획득 방법", style = MaterialTheme.typography.titleMedium)
                    Text(badge.requirement, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (badge.earned && badge.earnedAt != null) {
                        Text("획득일 · ${DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.KOREAN).format(Date(badge.earnedAt))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Row {
                    if (badge.earned) TextButton(onClick = { shareBadge(context, badge) }) { Text("자랑하기") }
                    TextButton(onClick = { selected = null }) { Text("닫기") }
                }
            }
        )
    }
}

@Composable
private fun BadgeTile(badge: CompanionBadge, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxWidth().companionCardBackground().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ).padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            Image(
                painter = painterResource(badge.image),
                contentDescription = badge.title,
                modifier = Modifier.size(112.dp).alpha(if (badge.earned) 1f else 0.28f)
            )
            Text(badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (badge.earned) "획득 완료" else "잠김 · 눌러서 확인",
                color = if (badge.earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
    }
}

private fun createBadges(progress: GrowthProgress, archive: JourneyArchive): List<CompanionBadge> {
    val hasMemory = progress.companionMemories.isNotEmpty() || progress.windCompletedAt != null || archive.firstSavedAt != null || archive.memories.isNotEmpty()
    val memoryAt = progress.companionMemories.firstOrNull()?.date?.let { java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } ?: progress.windCompletedAt ?: archive.firstSavedAt ?: archive.memories.firstOrNull()?.endedAt
    val hasJourney = archive.firstSavedAt != null
    val journeyAt = archive.firstSavedAt
    return listOf(
        CompanionBadge(
            "first_companion", "첫 인연", "처음으로 나만의 동료를 만났어요.",
            "마음에 드는 동료를 선택하면 열려요.", R.drawable.badge_first_companion, earned = true
        ),
        CompanionBadge(
            "first_breeze", "첫 바람", "차량 기능을 동료와 처음 경험했어요.",
            "지원되는 차량에서 ‘바람 느끼기’ 미션을 완료해요.", R.drawable.badge_first_breeze,
            earned = progress.windCompletedAt != null, earnedAt = progress.windCompletedAt
        ),
        CompanionBadge(
            "level_two", "한 뼘 성장", "첫 경험으로 동료가 한 단계 자랐어요.",
            "미션을 완료해 레벨 2에 도달해요.", R.drawable.badge_level_two,
            earned = progress.level >= 2, earnedAt = progress.windCompletedAt ?: progress.dailyDoorCheckDates.minOfOrNull { java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }.takeIf { progress.level >= 2 }
        ),
        CompanionBadge(
            "first_memory", "첫 기억", "동료와의 첫 순간을 기억으로 남겼어요.",
            "한마디·장소·목적지 기억 또는 첫 바람·이동 기억을 남겨요.", R.drawable.badge_first_memory,
            earned = hasMemory, earnedAt = memoryAt
        ),
        CompanionBadge(
            "shared_journey", "함께한 길", "동료와 함께한 이동을 기록했어요.",
            "장소 또는 목적지 기억을 남기거나, 지원되는 이동 구간을 기억으로 저장해요.", R.drawable.badge_shared_journey,
            earned = hasJourney || progress.companionMemories.any { it.kind == ExperienceKind.PLACE || it.kind == ExperienceKind.DESTINATION }, earnedAt = journeyAt
        ),
        CompanionBadge("first_note", "작은 한마디", "동료에게 마음을 전했어요.", "오늘의 한마디를 직접 입력하거나 말해서 저장해요.", R.drawable.badge_first_memory, progress.companionMemories.any { it.kind == ExperienceKind.NOTE }),
        CompanionBadge("first_greeting", "반가운 목소리", "동료와 처음 인사했어요.", "동료와 첫 인사 미션을 완료해요.", R.drawable.badge_first_companion, progress.hasExperience(ExperienceKind.GREETING)),
        CompanionBadge("door_day", "오늘의 안심", "주차 후 문 상태를 함께 확인했어요.", "운전석 문 닫힘 일일 미션을 한 번 완료해요.", R.drawable.badge_first_companion, progress.dailyDoorCheckDates.isNotEmpty()),
        CompanionBadge("door_week", "차와 함께한 한 주", "서로 다른 날에도 함께했어요.", "같은 주 서로 다른 3일에 문 닫힘 확인을 완료해요.", R.drawable.badge_shared_journey, progress.weeklyDoorBonusWeeks.isNotEmpty()),
        CompanionBadge("memory_week", "추억 한 페이지", "한 주의 기억을 채웠어요.", "같은 주 서로 다른 3일에 일일 기억을 남겨요. 한마디만으로도 가능해요.", R.drawable.badge_first_memory, ExperienceRules.bonusWeeks(progress.companionMemories).isNotEmpty()),
        CompanionBadge("vehicle_first", "차를 알아가는 사이", "차량 정보를 처음 살펴봤어요.", "차량 조회 미션 중 하나를 완료해요.", R.drawable.badge_first_breeze, progress.completedVehicleMissions.any { id -> vehicleMissions.any { it.id == id } }),
        CompanionBadge("vehicle_three", "호기심 많은 동료", "서로 다른 차량 기능 세 가지를 알아봤어요.", "서로 다른 차량 조회 미션 3개를 완료해요.", R.drawable.badge_level_two, progress.completedVehicleMissions.count { id -> vehicleMissions.any { it.id == id } } >= 3),
        CompanionBadge("level_four", "든든한 우리", "작은 경험들이 쌓여 동료가 자랐어요.", "일일 미션과 기억으로 레벨 4에 도달해요.", R.drawable.badge_level_two, progress.level >= 4)
    )
}

private fun shareBadge(context: Context, badge: CompanionBadge) {
    val directory = File(context.cacheDir, "badge-shares").apply { mkdirs() }
    val imageFile = File(directory, "${badge.id}.png")
    if (!imageFile.exists()) {
        context.resources.openRawResource(badge.image).use { input -> imageFile.outputStream().use { output -> input.copyTo(output) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "PleosMate에서 ‘${badge.title}’ 배지를 모았어요!\n${badge.description}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "배지 자랑하기"))
}
