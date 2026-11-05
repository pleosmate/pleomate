package com.pleos.mate.app.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pleos.mate.BuildConfig
import com.pleos.mate.core.designsystem.CompanionGlyphIcon
import com.pleos.mate.core.designsystem.Glyph
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.presentation.PetStage

@Composable
internal fun CompanionBottomBar(
    selectedTab: AppTab,
    character: PetCharacter,
    level: Int,
    onSelect: (AppTab) -> Unit,
    onOpenCompanion: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 480.dp
    val itemWidth = if (compact) 48.dp else 58.dp
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 26.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 368.dp).background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.24f))),
                RoundedCornerShape(52.dp)
            ),
            shape = RoundedCornerShape(52.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(Modifier.padding(if (compact) 6.dp else 10.dp), horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 12.dp)) {
                val tabs = buildList {
                    add(Glyph.Home to "홈")
                    add(Glyph.Missions to "미션")
                    add(Glyph.Collection to "도감")
                    add(Glyph.Settings to "설정")
                    if (BuildConfig.DEBUG) add(Glyph.Tools to "개발 도구")
                }
                tabs.forEachIndexed { index, (icon, label) ->
                    val active = selectedTab == AppTab.entries[index]
                    Surface(
                        modifier = Modifier.size(width = itemWidth, height = 56.dp),
                        shape = RoundedCornerShape(50),
                        color = if (active) Color.White.copy(alpha = 0.12f) else Color.Transparent
                    ) {
                        Box(
                            Modifier.fillMaxSize().semantics { contentDescription = label }.selectable(
                                selected = active, role = Role.Tab,
                                onClick = { onSelect(AppTab.entries[index]) }
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            CompanionGlyphIcon(icon, Modifier.size(30.dp), Color.White.copy(alpha = if (active) 1f else 0.78f))
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.padding(start = if (compact) 8.dp else 16.dp).size(if (compact) 60.dp else 80.dp)
                .semantics { contentDescription = "동료 선택" }
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.2f))), RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenCompanion
                ),
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
                PetStage(character, active = false, level = level, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
}
