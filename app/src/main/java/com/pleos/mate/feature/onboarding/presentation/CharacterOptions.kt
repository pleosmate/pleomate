package com.pleos.mate.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.presentation.PetStage
import com.pleos.mate.core.pet.presentation.descriptionResource
import com.pleos.mate.core.pet.presentation.labelResource

@Composable
internal fun CharacterOptions(selected: PetCharacter, enabled: Boolean, level: Int, onSelect: (PetCharacter) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 760.dp
        val columns = if (maxWidth >= 500.dp) 3 else 2
        val preview: @Composable () -> Unit = {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh)),
                    RoundedCornerShape(28.dp)
                ).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("당신의 새로운 동료", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                PetStage(selected, active = false, level = level, modifier = Modifier.fillMaxWidth().height(if (wide) 260.dp else 210.dp))
                Text(stringResource(selected.labelResource), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(selected.descriptionResource), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val choices: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("마음에 드는 동료를 골라요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                PetCharacter.entries.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { character ->
                            val active = selected == character
                            Column(
                                Modifier.weight(1f).testTag("character_${character.id}")
                                    .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                                    .border(2.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(20.dp))
                                    .selectable(selected = active, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(character) })
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PetStage(character, active = false, level = level, modifier = Modifier.fillMaxWidth().height(86.dp))
                                Text(stringResource(character.labelResource), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(stringResource(if (active) R.string.character_selected else R.string.character_select), style = MaterialTheme.typography.bodySmall,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(1f)) { preview() }
            Box(Modifier.weight(1f)) { choices() }
        } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { preview(); choices() }
    }
}
