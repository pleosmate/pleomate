package com.pleos.mate.feature.collection.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.collection.domain.growthStories
import com.pleos.mate.feature.growth.domain.GrowthProgress

@Composable
internal fun GrowthStoryBook(progress: GrowthProgress) {
    val stories = growthStories(progress)
    var selectedId by remember { mutableStateOf<String?>(null) }
    Text("우리의 성장 이야기 · ${stories.count { it.completed }} / ${stories.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("함께한 미션이 동료의 이야기가 돼요. 열린 이야기는 언제든 다시 읽을 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    stories.forEachIndexed { index, story ->
        Column(Modifier.fillMaxWidth().companionCardBackground().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("이야기 ${index + 1} · ${if (story.completed) "열림" else "기다리는 중"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(story.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(story.progress)
            TextButton(onClick = { selectedId = story.id }) { Text(if (story.completed) "이야기 읽기" else "열리는 방법") }
        }
    }
    stories.find { it.id == selectedId }?.let { story ->
        AlertDialog(onDismissRequest = { selectedId = null }, modifier = Modifier.companionDialogBorder(),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(story.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (story.completed) story.lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                else Text("이 이야기는 아직 함께 만들어가는 중이에요.")
                Text(story.requirement, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("함께 모으는 배지 · ${story.badge}", style = MaterialTheme.typography.labelLarge)
            } }, confirmButton = { TextButton(onClick = { selectedId = null }) { Text("닫기") } })
    }
}
