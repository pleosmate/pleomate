package com.pleos.mate.feature.onboarding.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.CompanionPageHeader
import com.pleos.mate.core.designsystem.companionTextFieldColors
import com.pleos.mate.core.pet.model.PetCharacter

@Composable
internal fun CharacterSelectionScreen(
    selected: PetCharacter,
    name: String,
    changing: Boolean,
    allowed: Boolean,
    parkingMessage: String,
    level: Int,
    reconnect: () -> Unit,
    onSelect: (PetCharacter) -> Unit,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    CompanionPageHeader(stringResource(R.string.character_choose_title), stringResource(R.string.character_choose_hint))
    if (!allowed) {
        Text(parkingMessage)
        OutlinedButton(onClick = reconnect) { Text("차량 권한·연결 확인") }
    }
    CharacterOptions(selected, allowed, level, onSelect)
    OutlinedTextField(shape = RoundedCornerShape(32.dp), colors = companionTextFieldColors(),
        value = name, onValueChange = onNameChanged,
        enabled = allowed, singleLine = true, label = { Text("동료 이름 (선택)") }, placeholder = { Text("어떤 이름으로 부를까요?") }, modifier = Modifier.fillMaxWidth())
    Button(enabled = allowed, onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("confirm_character")) {
        Text(stringResource(if (!changing) R.string.character_start else R.string.character_confirm))
    }
    if (changing) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.character_cancel))
        }
    }
}
