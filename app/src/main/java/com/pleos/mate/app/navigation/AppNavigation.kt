package com.pleos.mate.app.navigation

import com.pleos.mate.core.pet.model.PetCharacter

internal enum class AppTab { HOME, MISSIONS, COLLECTION, SETTINGS, DEVELOPER }

internal data class AppUiState(
    val characterId: String? = null,
    val petName: String = "",
    val choosing: Boolean = characterId == null,
    val pendingCharacterId: String = characterId ?: PetCharacter.BEAR.id,
    val pendingName: String = petName,
    val tab: AppTab = AppTab.HOME
) {
    val character: PetCharacter get() = PetCharacter.fromId(characterId) ?: PetCharacter.BEAR
}

internal sealed interface AppAction {
    data class SelectTab(val tab: AppTab) : AppAction
    data object OpenCharacterSelection : AppAction
    data class SelectCharacter(val character: PetCharacter) : AppAction
    data class EditName(val name: String) : AppAction
    data object ConfirmCharacter : AppAction
    data object Back : AppAction
}

/** Navigation decisions stay independent of Compose, Android and the vehicle SDK. */
internal fun AppUiState.reduce(action: AppAction, allowed: Boolean, debug: Boolean): AppUiState = when (action) {
    is AppAction.SelectTab -> if (action.tab == AppTab.DEVELOPER && !debug) this else copy(tab = action.tab)
    AppAction.OpenCharacterSelection -> if (!allowed) this else copy(
        choosing = true, pendingCharacterId = character.id, pendingName = petName
    )
    is AppAction.SelectCharacter -> if (!allowed || !choosing) this else copy(pendingCharacterId = action.character.id)
    is AppAction.EditName -> if (!allowed || !choosing) this else copy(pendingName = action.name.take(20))
    AppAction.ConfirmCharacter -> if (!allowed || !choosing) this else copy(
        characterId = pendingCharacterId, petName = pendingName.trim(), choosing = false
    )
    AppAction.Back -> when {
        choosing && characterId != null -> copy(choosing = false, pendingCharacterId = characterId, pendingName = petName)
        choosing -> this
        else -> copy(tab = AppTab.HOME)
    }
}
