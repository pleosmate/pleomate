package com.pleos.mate.app.navigation

import com.pleos.mate.core.pet.model.PetCharacter
import org.junit.Assert.*
import org.junit.Test

class AppNavigationTest {
    @Test fun firstMeetingRequiresAnExplicitParkedConfirmation() {
        val first = AppUiState()
        assertTrue(first.choosing)
        assertNull(first.characterId)
        val draft = first.reduce(AppAction.SelectCharacter(PetCharacter.RABBIT), true, false)
            .reduce(AppAction.EditName("  달이  "), true, false)
        assertNull(draft.characterId)
        assertEquals(draft, draft.reduce(AppAction.ConfirmCharacter, false, false))
        val confirmed = draft.reduce(AppAction.ConfirmCharacter, true, false)
        assertEquals("rabbit", confirmed.characterId)
        assertEquals("달이", confirmed.petName)
        assertFalse(confirmed.choosing)
        assertEquals(confirmed, confirmed.reduce(AppAction.ConfirmCharacter, true, false))
    }

    @Test fun cancellingAChangePreservesTheSavedCompanionAndName() {
        val saved = AppUiState(characterId = "bear", petName = "곰이")
        val editing = saved.reduce(AppAction.OpenCharacterSelection, true, false)
            .reduce(AppAction.SelectCharacter(PetCharacter.CLOUD), true, false)
            .reduce(AppAction.EditName("구름이"), true, false)
        val cancelled = editing.reduce(AppAction.Back, true, false)
        assertEquals(saved, cancelled)
        assertEquals("bear", cancelled.characterId)
        assertEquals("곰이", cancelled.petName)
    }

    @Test fun vehicleRestrictionsBlockCompanionEdits() {
        val state = AppUiState(characterId = "bear")
        assertEquals(state, state.reduce(AppAction.OpenCharacterSelection, false, true))
        val choosing = state.copy(choosing = true)
        listOf(AppAction.SelectCharacter(PetCharacter.SEED), AppAction.EditName("금지된 변경"), AppAction.ConfirmCharacter).forEach {
            assertEquals(choosing, choosing.reduce(it, false, true))
        }
    }

    @Test fun releaseCannotNavigateToDeveloperTools() {
        val state = AppUiState(characterId = "bear")
        assertEquals(state, state.reduce(AppAction.SelectTab(AppTab.DEVELOPER), true, false))
        assertEquals(AppTab.DEVELOPER, state.reduce(AppAction.SelectTab(AppTab.DEVELOPER), true, true).tab)
    }

    @Test fun backReturnsFromAProductTabAndKeepsTheCompanion() {
        val state = AppUiState(characterId = "rover", petName = "로버")
            .reduce(AppAction.SelectTab(AppTab.SETTINGS), true, false)
        val home = state.reduce(AppAction.Back, true, false)
        assertEquals(AppTab.HOME, home.tab)
        assertEquals("rover", home.characterId)
        assertEquals("로버", home.petName)
    }

    @Test fun draftNameIsBoundedAndBackCannotSkipFirstMeeting() {
        val first = AppUiState()
        assertEquals(first, first.reduce(AppAction.Back, true, false))
        val edited = first.reduce(AppAction.EditName("가".repeat(30)), true, false)
        assertEquals(20, edited.pendingName.length)
    }
}
