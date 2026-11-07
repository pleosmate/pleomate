package com.pleos.mate

import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.pleos.mate.core.designsystem.CompanionTheme
import com.pleos.mate.feature.exploration.presentation.GeographicExplorationMap
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExplorationHierarchyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun canNavigateFromProvinceToNeighborhood() {
        var selected: String? = null
        compose.activity.runOnUiThread {
            compose.activity.setContent { CompanionTheme { GeographicExplorationMap { selected = it?.name } } }
        }
        fun choose(name: String) {
            compose.waitUntil(15000) {
                try {
                    compose.onNodeWithTag("exploration_area_canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions].any { it.label == name }
                } catch (_: AssertionError) { false }
            }
            val action = compose.onNodeWithTag("exploration_area_canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == name }
            compose.runOnIdle { action.action() }
        }
        choose("서울")
        compose.waitUntil(15000) {
            try { compose.onNodeWithTag("exploration_area_canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions].any { it.label == "강남구" } }
            catch (_: AssertionError) { false }
        }
        compose.onNodeWithTag("map_zoom_in").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("150%").assertExists()
        choose("강남구")
        compose.waitUntil(15000) { compose.onAllNodes(androidx.compose.ui.test.hasText("100%")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("100%").assertExists()
        choose("역삼1동")
        compose.runOnIdle { assertEquals("역삼1동", selected) }
    }
}
