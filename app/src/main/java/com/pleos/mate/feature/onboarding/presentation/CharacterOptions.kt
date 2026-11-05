package com.pleos.mate.feature.onboarding.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.presentation.PetStage
import com.pleos.mate.core.pet.presentation.descriptionResource
import com.pleos.mate.core.pet.presentation.labelResource

@Composable
internal fun CharacterOptions(selected: PetCharacter, enabled: Boolean, level: Int, onSelect: (PetCharacter) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        if (maxWidth >= 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PetCharacter.entries.chunked(3).forEach { characters ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        characters.forEach { character ->
                            Box(Modifier.weight(1f)) { CharacterOption(character, selected == character, enabled, level, onSelect) }
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PetCharacter.entries.forEach { CharacterOption(it, selected == it, enabled, level, onSelect) }
            }
        }
    }
}

@Composable
private fun CharacterOption(character: PetCharacter, selected: Boolean, enabled: Boolean, level: Int, onSelect: (PetCharacter) -> Unit) {
    Column(
        Modifier.fillMaxWidth().testTag("character_${character.id}")
            .companionCardBackground()
            .border(2.dp, if (selected) Color.White.copy(alpha = 0.9f) else Color.Transparent, RoundedCornerShape(26.dp))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(character) })
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            // Static thumbnails; only the confirmed companion animates on the home screen.
            PetStage(character, active = false, level = level, modifier = Modifier.fillMaxWidth().height(170.dp))
            Text(stringResource(character.labelResource), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(character.descriptionResource), modifier = Modifier.heightIn(min = 56.dp))
            Text(stringResource(if (selected) R.string.character_selected else R.string.character_select),
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

