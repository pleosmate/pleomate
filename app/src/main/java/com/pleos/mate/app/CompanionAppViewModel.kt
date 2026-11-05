package com.pleos.mate.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.pleos.mate.BuildConfig
import com.pleos.mate.app.navigation.AppAction
import com.pleos.mate.app.navigation.AppTab
import com.pleos.mate.app.navigation.AppUiState
import com.pleos.mate.app.navigation.reduce
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.data.local.CompanionPreferences
import com.pleos.mate.data.PetProfileDataSource
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
internal class CompanionAppViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val preferences: CompanionPreferences,
    private val profile: PetProfileDataSource = preferences,
    val session: CompanionSession
) : ViewModel() {
    private val initialCharacter = PetCharacter.fromId(profile.characterId)?.id
    private val initialState = AppUiState(
        characterId = initialCharacter,
        petName = profile.petName,
        choosing = savedState["choosing"] ?: (initialCharacter == null),
        pendingCharacterId = PetCharacter.fromId(savedState["pending_character"])?.id ?: initialCharacter ?: PetCharacter.BEAR.id,
        pendingName = savedState["pending_name"] ?: profile.petName,
        tab = AppTab.entries.firstOrNull { it.name == savedState.get<String>("tab") }
            ?.takeUnless { it == AppTab.DEVELOPER && !BuildConfig.DEBUG } ?: AppTab.HOME
    )
    private val mutableState = MutableStateFlow(initialState)
    val uiState = mutableState.asStateFlow()

    fun onAction(action: AppAction) {
        val previous = mutableState.value
        val next = previous.reduce(action, session.parkedAndAllowed, BuildConfig.DEBUG)
        if (next == previous) return
        if (action == AppAction.ConfirmCharacter) profile.saveProfile(next.characterId!!, next.petName)
        if (action == AppAction.OpenCharacterSelection) session.cancelMission()
        if (next.choosing || next.tab != AppTab.MISSIONS) {
            session.cancelVehicleMission()
            session.experiences.cancel()
        }
        savedState["choosing"] = next.choosing
        savedState["pending_character"] = next.pendingCharacterId
        savedState["pending_name"] = next.pendingName
        savedState["tab"] = next.tab.name
        mutableState.value = next
    }

    override fun onCleared() { session.close() }
}
