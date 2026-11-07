package com.pleos.mate.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.CompanionDialog
import com.pleos.mate.core.designsystem.CompanionGlyphIcon
import com.pleos.mate.core.designsystem.FlatCard
import com.pleos.mate.core.designsystem.Glyph
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.presentation.PetMotionControls
import com.pleos.mate.core.pet.presentation.PetStage
import com.pleos.mate.feature.companion.domain.PetCollection
import com.pleos.mate.feature.companion.presentation.PetReactionBubble
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.journey.presentation.JourneyViewModel

@Composable
internal fun CompanionHome(character: PetCharacter, session: CompanionSession, journeys: JourneyViewModel, reconnect: () -> Unit, onOpenMissions: () -> Unit, onOpenCollection: () -> Unit, onOpenExploration: () -> Unit, onChange: () -> Unit, petName: String, viewportHeight: Dp) {
    val context = LocalContext.current
    val progress = session.progress
    val celebrationId = session.celebration
    var greeting by remember { mutableStateOf(false) }
    var motionsExpanded by rememberSaveable { mutableStateOf(false) }
    var previewMotion by remember { mutableStateOf(PetMotion.SMILE) }
    var previewRequest by remember { mutableIntStateOf(0) }
    val greetingText = when {
        session.mission == CompanionSession.Mission.COMPLETE && progress.windMemoryAt != null -> R.string.wind_complete_hello
        journeys.archive.memories.isNotEmpty() -> R.string.journey_reunion_hello
        progress.canReuniteWithWind -> R.string.wind_reunion_hello
        else -> R.string.pet_home_hello
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 600.dp
    val wide = maxWidth >= 840.dp
    val petHeight = (viewportHeight * if (wide) 0.61f else 0.46f).coerceIn(260.dp, if (wide) 700.dp else 440.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("PLEOS MATE  /  OUR LITTLE JOURNEY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text("함께라서 좋은 오늘", color = MaterialTheme.colorScheme.onSurface, style = (if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge).copy(fontWeight = FontWeight.Bold))
            }
            if (session.parkedAndAllowed) Text(stringResource(R.string.home_parked), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onChange, enabled = session.parkedAndAllowed,
                modifier = Modifier.size(56.dp).testTag("change_character")) {
                CompanionGlyphIcon(Glyph.Companion, Modifier.size(30.dp))
            }
        }
        if (!session.parkedAndAllowed) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(session.parkingMessage, modifier = Modifier.weight(1f))
                TextButton(onClick = reconnect, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.home_reconnect)) }
            }
        }
        val petContent: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlatCard(Modifier.fillMaxWidth().height(petHeight).testTag("home_character")) {
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF526660), Color(0xFF34423F)))).clickable(
                    enabled = session.parkedAndAllowed,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { greeting = !greeting; session.previewPetReaction() },
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.companion_landscape_v2), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    Color(0xAA263A31), Color.Transparent, Color(0x66304135)
                ))))
                Column(Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        petName.ifBlank { stringResource(R.string.companion_home_title) } + "의 쉼터",
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (session.loaded) PetMotionControls(progress.level, session.parkedAndAllowed, motionsExpanded,
                        onToggle = { motionsExpanded = !motionsExpanded },
                        onPlay = { previewMotion = it; previewRequest++ })
                    }
                    PetStage(character, active = session.parkedAndAllowed,
                        greet = greeting, level = progress.level, showRoomBackground = false,
                        previewMotion = previewMotion, previewRequest = previewRequest,
                        outfit = session.outfit,
                        room = session.room,
                        celebration = celebrationId + journeys.memoryReaction,
                        celebrateOnEntry = session.hasPendingCelebration,
                        onCelebrationPlayed = { session.markCelebrationPlayed(celebrationId) },
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("pet_artwork"))

                }
            }
        }
        Box(Modifier.fillMaxWidth().testTag("pet_dialogue_slot"), contentAlignment = Alignment.CenterStart) {
            PetReactionBubble(session, character)
        }
        Text(
            PetCollection.reunion(progress, journeys.archive.memories, remember { (System.currentTimeMillis() % 10000).toInt() }), modifier = Modifier.fillMaxWidth().testTag("companion_greeting"),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), textAlign = androidx.compose.ui.text.style.TextAlign.Start
        )
            }
        }
        val activityContent: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("우리의 여정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FlatCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpenExploration).padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .15f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    CompanionGlyphIcon(Glyph.Explore, Modifier.size(30.dp), MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("우리의 여행 지도", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("발견한 지역 ${progress.exploredRegions.size}곳", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CompanionGlyphIcon(Glyph.ChevronRight, Modifier.size(24.dp), MaterialTheme.colorScheme.primary)
            }
        }
        if (session.loaded) {
            val floor = progress.levelStartXp
            FlatCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("함께 자라는 중", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Lv. ${progress.level} · ${PetCollection.stage(progress.level)}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("${progress.totalXp - floor} / ${progress.nextLevelXp - floor} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { ((progress.totalXp - floor).toFloat() / (progress.nextLevelXp - floor).coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                )
                Text("다음 성장까지 ${(progress.nextLevelXp - progress.totalXp).coerceAtLeast(0)} XP", modifier = Modifier.testTag("growth_progress"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }
        }

        val busy = session.mission in setOf(CompanionSession.Mission.CHECKING, CompanionSession.Mission.WAITING, CompanionSession.Mission.VERIFYING, CompanionSession.Mission.SAVING) ||
            session.dailyDoorCheck == CompanionSession.DailyDoorCheck.CHECKING
        FlatCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("오늘 함께할 일", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (progress.hasDailyDoorCheck()) "오늘 완료" else "+5 XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_mission_lock), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("운전석 문 닫힘 확인", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (progress.hasDailyDoorCheck()) "오늘 운전석 문 닫힘을 확인했어요" else "주차하면 운전석 문 상태를 확인해요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onOpenMissions, enabled = session.parkedAndAllowed && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("open_mission_list"), shape = RoundedCornerShape(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("미션 둘러보기", fontWeight = FontWeight.Bold)
                        CompanionGlyphIcon(Glyph.ChevronRight, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        if (busy || session.mission == CompanionSession.Mission.ERROR) {
            Text(if (session.dailyDoorCheck == CompanionSession.DailyDoorCheck.CHECKING) session.dailyDoorMessage else session.message, modifier = Modifier.testTag("mission_status"))
            if (busy && session.mission in setOf(CompanionSession.Mission.CHECKING, CompanionSession.Mission.WAITING, CompanionSession.Mission.VERIFYING)) TextButton(onClick = { session.cancelMission("다음에 함께해도 괜찮아요.") }, modifier = Modifier.fillMaxWidth().testTag("cancel_wind_mission")) {
                Text(stringResource(R.string.home_mission_later))
            }
        }
        if (session.loaded) {
            val memoryCount = progress.companionMemories.size + journeys.archive.memories.size + if (progress.windMemoryAt != null) 1 else 0
            FlatCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("함께 쌓은 순간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (memoryCount > 0) "지금까지 기억 ${memoryCount}개를 함께 남겼어요." else "첫 경험을 남기면 여기에 함께한 순간이 쌓여요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("다음 성장까지 ${ (progress.nextLevelXp - progress.totalXp).coerceAtLeast(0) } XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        FlatCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenCollection
                ).padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.memory_section), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("도감 보기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompanionGlyphIcon(Glyph.ChevronRight, Modifier.size(20.dp))
                }
            }
        }            }
        }
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Box(Modifier.weight(1.45f)) { petContent() }
                Box(Modifier.weight(0.9f)) { activityContent() }
            }
        } else {
            petContent()
            activityContent()
        }
    }
    if (session.levelUpMessage != null && session.parkedAndAllowed) CompanionDialog(
        modifier = Modifier.companionDialogBorder(),
        onDismissRequest = session::dismissLevelUp,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("동료가 성장했어요") },
        text = { Text(session.levelUpMessage.orEmpty()) },
        confirmButton = { TextButton(onClick = {
            session.dismissLevelUp()
            motionsExpanded = true
            previewMotion = PetMotionRules.unlocked(progress.level).lastOrNull() ?: PetMotion.SMILE
            previewRequest++
        }) { Text("성장 모션 보기") } }
    )
}
}
