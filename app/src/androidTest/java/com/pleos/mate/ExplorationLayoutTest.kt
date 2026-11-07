package com.pleos.mate

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.CompanionTheme
import com.pleos.mate.feature.exploration.presentation.ExplorationMap
import com.pleos.mate.feature.exploration.presentation.explorationPlaces
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated atlas layout: does not change the user's saved exploration progress. */
class ExplorationLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun narrowAtlasHasSeparateTouchTargetsAndSelectsTheCorrectRegion() {
        var selected: String? = null
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                CompanionTheme {
                    Box(Modifier.width(320.dp)) {
                        ExplorationMap(setOf("서울특별시"), null) { selected = it }
                    }
                }
            }
        }
        compose.waitForIdle()
        val nodes = explorationPlaces.map { compose.onNodeWithTag("exploration_place_${it.label}").fetchSemanticsNode() }
        nodes.forEachIndexed { i, node ->
            assertTrue("${explorationPlaces[i].label} needs a touch target", node.boundsInRoot.width >= 48f)
            nodes.drop(i + 1).forEach { other ->
                assertFalse("Atlas touch targets must not overlap", node.boundsInRoot.overlaps(other.boundsInRoot))
            }
        }
        compose.onNodeWithTag("exploration_place_서울").performClick()
        compose.runOnIdle { assertEquals("서울특별시", selected) }
    }
}
