package com.pleos.mate.feature.collection.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.feature.journey.presentation.JourneyCard
import com.pleos.mate.feature.journey.presentation.JourneyViewModel

@Composable
internal fun CollectionScreen(session: CompanionSession, journeys: JourneyViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("동료 배지 도감", style = MaterialTheme.typography.headlineMedium)
        Text("함께한 경험을 모으고, 마음에 드는 배지를 자랑해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        BadgeCollection(session.progress, journeys.archive)
        GrowthStoryBook(session.progress)
        Text("함께한 기억", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        FirstMemoryCard(session)
        JourneyCard(journeys, session.parkedAndAllowed)
        ExperienceMemories(session.progress)
    }
}

