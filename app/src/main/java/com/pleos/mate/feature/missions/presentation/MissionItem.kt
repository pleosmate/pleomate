package com.pleos.mate.feature.missions.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.CompanionGlyphIcon
import com.pleos.mate.core.designsystem.Glyph
import com.pleos.mate.core.designsystem.companionCardBackground

@Composable
internal fun MissionItem(title: String, detail: String, reward: String, badge: String, enabled: Boolean, onClick: () -> Unit, @DrawableRes icon: Int, buttonTag: String? = null, buttonLabel: String = "미션 시작") {
    val highlighted = enabled || badge in setOf("완료", "오늘 완료", "주간 완료")
    Row(Modifier.fillMaxWidth().companionCardBackground().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
                if (badge in setOf("완료", "오늘 완료", "주간 완료")) {
                    Box(Modifier.align(Alignment.BottomEnd).size(20.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        CompanionGlyphIcon(Glyph.Check, Modifier.size(12.dp), MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (badge in setOf("완료", "오늘 완료", "주간 완료")) {
                        Image(
                            painter = painterResource(R.drawable.mission_completed_stamp),
                            contentDescription = badge,
                            modifier = Modifier.size(48.dp).rotate(-12f)
                        )
                    } else {
                        Text(
                            badge,
                            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
            }
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(reward, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                if (enabled) {
                    val baseModifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    Button(onClick = onClick, modifier = if (buttonTag == null) baseModifier else baseModifier.testTag(buttonTag), shape = RoundedCornerShape(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompanionGlyphIcon(if (badge == "오늘 완료" || badge == "완료") Glyph.Check else Glyph.ChevronRight, Modifier.size(20.dp), MaterialTheme.colorScheme.onPrimary)
                            Text(buttonLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
    }
}

