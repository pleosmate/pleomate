package com.pleos.mate.feature.companion.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.presentation.PetStage
import com.pleos.mate.feature.companion.domain.PetCollection
import com.pleos.mate.feature.companion.domain.PetDecoration
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.journey.presentation.JourneyViewModel

@Composable
internal fun PetWardrobe(session: CompanionSession, journeys: JourneyViewModel) {
    val memories = session.progress.windMemoryAt != null || session.progress.companionMemories.isNotEmpty() || journeys.archive.memories.isNotEmpty()
    var previewGait by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var previewMotion by remember { mutableStateOf(PetMotion.SMILE) }
    var previewRequest by remember { mutableIntStateOf(0) }
    var appearancePreview by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    val character = session.character
    val previewLevel = if (appearancePreview == 0) session.progress.level else appearancePreview
    session.decorationRevision
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 840.dp
        val preview: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(Modifier.fillMaxWidth().height(280.dp).background(
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh)), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center) {
    PetStage(character, active = session.parkedAndAllowed,
        running = previewGait == 2 && session.parkedAndAllowed, walking = previewGait == 1 && session.parkedAndAllowed,
        previewMotion = previewMotion, previewRequest = previewRequest,
        level = previewLevel, outfit = session.outfit, room = session.room,
        modifier = Modifier.fillMaxWidth().height(260.dp))
    }
    Text("성장 모습 · Lv.1 → Lv.5 → Lv.15", style = MaterialTheme.typography.titleMedium)
    listOf(0, 1, 5, 15).chunked(2).forEach { levels ->
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { level ->
            OutlinedButton(onClick = { appearancePreview = level }, enabled = session.parkedAndAllowed,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (appearancePreview == level) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)) {
                Text((if (appearancePreview == level) "✓ " else "") + if (level == 0) "현재" else "Lv.$level")
            }
        }
    }
    }
    Text(if (appearancePreview == 0) "현재 외형: ${PetCollection.stage(session.progress.level)}"
        else "Lv.$previewLevel 모습과 모션을 미리 보고 있어요. 실제 레벨과 보상은 바뀌지 않아요.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("쉬기", "걷기", "달리기").forEachIndexed { index, label ->
            OutlinedButton(onClick = { previewGait = index }, enabled = session.parkedAndAllowed,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (previewGait == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)) { Text((if (previewGait == index) "✓ " else "") + label) }
        }
    }
    (listOf(PetMotion.SMILE) + PetMotionRules.unlocked(previewLevel)).chunked(3).forEach { motions ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            motions.forEach { motion ->
                OutlinedButton(onClick = { previewGait = 0; previewMotion = motion; previewRequest++ },
                    enabled = session.parkedAndAllowed, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(motion.label) }
            }
        }
    }
    Text("달리기는 주차 중 감상하는 모션이에요. 차량 주행 중 표시 기능은 검토 중이에요.")
            }
        }
        val decorations: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("동료 꾸미기", style = MaterialTheme.typography.titleLarge)
    Text("미션과 기억으로 얻은 소품이에요. 곰과 토끼가 함께 사용할 수 있어요.")
    if (character.isPixel) {
        Text("도트 동료는 전용 기본 모습을 사용해요. 곰·토끼의 스카프와 리본 선택은 보관돼요.")
    } else PetDecorationOptions("의상", PetCollection.outfits, session.outfit, session.parkedAndAllowed,
        { PetCollection.outfitUnlocked(it, session.progress, memories) }) { session.selectDecoration("outfit", it) }
    PetDecorationOptions("공간", PetCollection.rooms, session.room, session.parkedAndAllowed,
        { PetCollection.roomUnlocked(it, session.progress, memories) }) { session.selectDecoration("room", it) }            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Box(Modifier.weight(1f)) { preview() }
                Box(Modifier.weight(1f)) { decorations() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                preview()
                decorations()
            }
        }
    }
}

@Composable
private fun PetDecorationOptions(title: String, items: List<PetDecoration>, selected: String, allowed: Boolean,
    unlocked: (String) -> Boolean, choose: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    items.forEach { item ->
        val available = unlocked(item.id)
        OutlinedButton(onClick = { choose(item.id) }, enabled = allowed && available,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected == item.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)) {
            Text((if (selected == item.id) "✓ " else "") + item.title + if (!available) " · ${item.requirement}" else "")
        }
    }
}
