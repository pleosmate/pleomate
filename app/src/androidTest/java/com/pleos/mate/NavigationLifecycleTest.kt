package com.pleos.mate

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.app.CompanionAppViewModel
import com.pleos.mate.app.navigation.AppAction
import com.pleos.mate.app.navigation.AppTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies host/ViewModel lifecycle without waiting for the pet renderer to become idle. */
@RunWith(AndroidJUnit4::class)
class NavigationLifecycleTest {
    @Test fun selectedTabSurvivesActivityRecreationWithoutChangingTheProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("companion", Context.MODE_PRIVATE)
        assumeTrue("Requires an existing companion; onboarding is left untouched", preferences.contains("character_id"))
        val profileKeys = setOf("character_id", "pet_name", "outfit", "room")
        val profile = preferences.all.filterKeys { it in profileKeys }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var original: CompanionAppViewModel
            scenario.onActivity {
                original = ViewModelProvider(it)[CompanionAppViewModel::class.java]
                original.onAction(AppAction.SelectTab(AppTab.MISSIONS))
                assertEquals(AppTab.MISSIONS, original.uiState.value.tab)
            }
            scenario.recreate()
            scenario.onActivity {
                val restored = ViewModelProvider(it)[CompanionAppViewModel::class.java]
                assertSame(original, restored)
                assertEquals(AppTab.MISSIONS, restored.uiState.value.tab)
                assertEquals(profile, preferences.all.filterKeys { key -> key in profileKeys })
            }
        }
    }
}
