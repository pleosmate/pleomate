package com.pleos.mate

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.designsystem.CompanionTheme
import com.pleos.mate.feature.exploration.presentation.ExplorationMap
import com.pleos.mate.feature.exploration.presentation.ExplorationAdaptiveLayout
import com.pleos.mate.feature.exploration.presentation.SouvenirImage
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

    @Test fun souvenirSlotsExposeThreeDistinctPixelSilhouettes() {
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                CompanionTheme {
                    Row {
                        (1..3).forEach { slot ->
                            SouvenirImage("서울특별시", Modifier.size(72.dp).testTag("souvenir_slot_$slot"), true, slot)
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val rendered = (1..3).map { slot ->
            val image = compose.onNodeWithTag("souvenir_slot_$slot").captureToImage().toPixelMap()
            buildList {
                for (y in 0 until image.height step 3) for (x in 0 until image.width step 3) add(image[x, y].hashCode())
            }
        }
        assertNotEquals("Each slot should render a different pixel-art silhouette", rendered[0], rendered[1])
        assertNotEquals("Each slot should render a different pixel-art silhouette", rendered[1], rendered[2])
        val names = listOf("pixel gate", "pixel bottle", "pixel flower charm")
        (1..3).forEach {
            compose.onNodeWithTag("souvenir_slot_$it").fetchSemanticsNode()
            compose.onNodeWithTag("souvenir_slot_$it").assertContentDescriptionEquals(names[it - 1])
        }
    }

    @Test fun narrowAndWideWindowsKeepMapAndSelectedProgressInTheSameViewport() {
        listOf(400, 1280).forEach { widthDp ->
            var compact = false
            compose.activity.runOnUiThread {
                compose.activity.setContent {
                    CompanionTheme {
                        Box(Modifier.width(widthDp.dp).height(720.dp)) {
                            ExplorationAdaptiveLayout(
                                map = { isCompact, modifier ->
                                    compact = isCompact
                                    Box(modifier.height(260.dp).testTag("adaptive_map"))
                                },
                                details = {
                                    Column(Modifier.testTag("selected_summary")) {
                                        androidx.compose.material3.Text("강남구 역삼동")
                                        (1..3).forEach { slot -> androidx.compose.material3.Text("조각 $slot · 미발견") }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            compose.waitForIdle()
            val map = compose.onNodeWithTag("adaptive_map").fetchSemanticsNode().boundsInRoot
            val summary = compose.onNodeWithTag("selected_summary").fetchSemanticsNode().boundsInRoot
            val expectedCompact = widthDp < 720
            compose.runOnIdle { assertEquals(expectedCompact, compact) }
            if (expectedCompact) assertTrue("Narrow layout puts the summary below the map", summary.top >= map.bottom)
            else assertTrue("Wide layout puts the summary beside the map", summary.left >= map.right)
            listOf("강남구 역삼동", "조각 1 · 미발견", "조각 2 · 미발견", "조각 3 · 미발견").forEach { label ->
                val bounds = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
                assertTrue("$label should fit in the 720dp viewport", bounds.bottom <= 720f)
            }
        }
    }
}
