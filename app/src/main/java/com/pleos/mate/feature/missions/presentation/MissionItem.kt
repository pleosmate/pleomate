package com.pleos.mate.feature.missions.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.CompanionGlyphIcon
import com.pleos.mate.core.designsystem.FlatCard
import com.pleos.mate.core.designsystem.Glyph

@Composable
internal fun MissionItem(title: String, detail: String, reward: String, badge: String, enabled: Boolean, onClick: () -> Unit, @DrawableRes icon: Int, buttonTag: String? = null, buttonLabel: String = "미션 시작") {
    val completed = badge in setOf("완료", "오늘 완료", "주간 완료")
    FlatCard(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(20.dp)) {
            val wide = maxWidth >= 620.dp
            val info: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(reward, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (completed) CompanionGlyphIcon(Glyph.Check, Modifier.size(24.dp), MaterialTheme.colorScheme.tertiary)
                }
            }
            val action: @Composable (Modifier) -> Unit = { modifier ->
                if (enabled) {
                    val base = modifier.heightIn(min = 56.dp)
                    Button(onClick = onClick, modifier = if (buttonTag == null) base else base.testTag(buttonTag)) {
                        Text(buttonLabel, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        CompanionGlyphIcon(Glyph.ChevronRight, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { info() }
                action(Modifier.widthIn(min = 180.dp))
            } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                info()
                action(Modifier.fillMaxWidth())
            }
        }
    }
}
