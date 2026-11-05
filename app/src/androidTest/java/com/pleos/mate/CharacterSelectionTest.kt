package com.pleos.mate

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Run on a parked Pleos test emulator with vehicle read permission; changes saved selection. */
class CharacterSelectionTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun chooseRabbit_restore_thenChangeToBear() {
        openCharacterSelection()
        compose.onNodeWithTag("character_rabbit").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("confirm_character").performScrollTo().performClick()
        compose.onNodeWithTag("home_character").assertIsDisplayed()
        assertEquals("rabbit", compose.activity.getSharedPreferences("companion", Context.MODE_PRIVATE).getString("character_id", null))

        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("home_character").assertIsDisplayed()
        openCharacterSelection()
        compose.onNodeWithTag("character_bear").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("confirm_character").performScrollTo().performClick()
        compose.onNodeWithTag("home_character").assertIsDisplayed()
        assertEquals("bear", compose.activity.getSharedPreferences("companion", Context.MODE_PRIVATE).getString("character_id", null))
    }

    private fun openCharacterSelection() {
        if (compose.onAllNodesWithTag("home_character").fetchSemanticsNodes().isNotEmpty()) {
            waitForParking("change_character")
            compose.onNodeWithTag("change_character").performScrollTo().performClick()
        } else waitForParking("confirm_character")
    }

    private fun waitForParking(tag: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            runCatching { compose.onNodeWithTag(tag).assertIsEnabled(); true }.getOrDefault(false)
        }
    }

}
