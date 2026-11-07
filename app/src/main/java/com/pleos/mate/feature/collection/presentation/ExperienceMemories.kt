package com.pleos.mate.feature.collection.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.CompanionDialog
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.CompanionMemory

@Composable
internal fun ExperienceMemories(progress: GrowthProgress) {
    var viewing by remember { mutableStateOf(false) }
    val memories = progress.companionMemories
    Text("동료와 만든 추억", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (memories.isEmpty()) Text("미션에서 장소와 한마디를 기억으로 남겨 보세요.")
    memories.takeLast(3).asReversed().forEach { MemoryTile(it) }
    if (memories.isNotEmpty()) TextButton(onClick = { viewing = true }) { Text("추억 모두 보기 · ${memories.size}개") }
    if (viewing) CompanionDialog(
        modifier = Modifier.companionDialogBorder(),
        onDismissRequest = { viewing = false },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("동료와 만든 추억") },
        text = { LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(memories.asReversed(), key = { it.claimKey }) { MemoryTile(it) }
        } },
        confirmButton = { TextButton(onClick = { viewing = false }) { Text("닫기") } }
    )
}

@Composable
private fun MemoryTile(memory: CompanionMemory) {
    Column(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(memory.kind.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(memory.text)
        Text(memory.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
