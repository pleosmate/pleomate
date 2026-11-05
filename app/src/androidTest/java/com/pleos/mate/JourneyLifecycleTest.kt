package com.pleos.mate

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.feature.journey.data.local.JourneyDraftStore
import com.pleos.mate.feature.journey.data.local.JourneyRepository
import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.domain.JourneyObservation
import com.pleos.mate.feature.journey.domain.JourneyObservationFactory
import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.journey.domain.OdometerSource
import com.pleos.mate.feature.journey.presentation.JourneyViewModel
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses isolated files/preferences and never changes the user's pet or journey archive. */
@RunWith(AndroidJUnit4::class)
class JourneyLifecycleTest {
    private class IsolatedApplication(context: Context, private val key: String) : Application() {
        init { attachBaseContext(context) }
        override fun getFilesDir() = File(baseContext.filesDir, key).apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences("${key}_$name", mode)
    }

    @Test fun draftSurvivesRecreationAndRestrictionThenSaveEditDelete() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = IsolatedApplication(instrumentation.targetContext, "journey_test_${UUID.randomUUID()}")
        fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
        fun await(condition: () -> Boolean) {
            val end = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < end) {
                var ready = false
                onMain { ready = condition() }
                if (ready) return
                Thread.sleep(20)
            }
            fail("Timed out waiting for persisted journey")
        }
        lateinit var first: JourneyViewModel
        onMain { first = createJourneyViewModel(app) }
        await { first.loaded }
        onMain {
            first.setParkedAllowed(true)
            first.beginEditor()
            first.updateEditor("나들이", "복원할 메모")
            first.setParkedAllowed(false)
            first.updateEditor("일상", "금지된 입력")
            first.saveEditor()
            assertEquals("복원할 메모", first.editor!!.memo)
            assertTrue(first.archive.memories.isEmpty())
        }
        lateinit var restored: JourneyViewModel
        onMain { restored = createJourneyViewModel(app) }
        await { restored.loaded }
        onMain {
            assertEquals(first.editor, restored.editor)
            restored.setParkedAllowed(true)
            restored.saveEditor()
        }
        await { !restored.busy && restored.archive.memories.size == 1 }
        var id = ""
        onMain {
            assertNull(restored.editor)
            val memory = restored.archive.memories.single()
            id = memory.id
            assertEquals(OdometerSource.MANUAL, memory.source)
            assertNull(restored.archive.firstSavedAt)
            restored.beginEditor(id)
            restored.updateEditor("함께한 시간", "수정한 기억")
            restored.saveEditor()
        }
        await { !restored.busy && restored.editor == null }
        onMain {
            assertEquals("함께한 시간", restored.archive.memories.single().theme)
            assertEquals("수정한 기억", restored.archive.memories.single().memo)
            restored.deleteMemory(id)
        }
        await { !restored.busy && restored.archive.memories.isEmpty() }
        assertTrue(id in JourneyStore(app.filesDir).read().handledIds)
        assertEquals(0, GrowthStore(app.filesDir).read().totalXp)
        assertNull(app.getSharedPreferences("journey_editor", Context.MODE_PRIVATE).getString("id", null))
        app.filesDir.deleteRecursively()
    }

    private fun createJourneyViewModel(context: Context) = JourneyViewModel(
        JourneyRepository(JourneyStore(context.filesDir)),
        JourneyDraftStore(context),
        JourneyObservationFactory { _, _ ->
            object : JourneyObservation {
                override fun start() = Unit
                override fun requestSample() = Unit
                override fun stop() = Unit
            }
        }
    )
}
