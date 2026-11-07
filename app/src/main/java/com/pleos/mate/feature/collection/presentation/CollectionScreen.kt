package com.pleos.mate.feature.collection.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.core.designsystem.CompanionPageHeader
import com.pleos.mate.core.designsystem.CompanionOverview
import com.pleos.mate.core.designsystem.CompanionSections
import com.pleos.mate.feature.journey.presentation.JourneyCard
import com.pleos.mate.feature.journey.presentation.JourneyViewModel

@Composable
internal fun CollectionScreen(session: CompanionSession, journeys: JourneyViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    val navigation: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            CompanionPageHeader("우리의 기록", "함께한 순간을 천천히 다시 꺼내 보세요.")
            CompanionOverview("OUR STORY", "함께한 시간이 쌓이는 곳",
                "Lv.${session.progress.level} · 발견한 지역 ${session.progress.exploredRegions.size}곳")
            CompanionSections(listOf("배지", "성장 이야기", "기억"), section.coerceIn(0, 2)) { section = it }
        }
    }
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (section.coerceIn(0, 2)) {
                0 -> BadgeCollection(session.progress, journeys.archive)
                1 -> GrowthStoryBook(session.progress)
                2 -> {
                    Text("함께한 기억", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FirstMemoryCard(session)
                    JourneyCard(journeys, session.parkedAndAllowed)
                    ExperienceMemories(session.progress)
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 840.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Box(Modifier.weight(0.8f)) { navigation() }
                Box(Modifier.weight(1.2f)) { content() }
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(24.dp)) { navigation(); content() }
    }
}
