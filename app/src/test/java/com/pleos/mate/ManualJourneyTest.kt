package com.pleos.mate

import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyEditor
import com.pleos.mate.feature.journey.domain.JourneyEditorMode
import com.pleos.mate.feature.journey.domain.OdometerSource
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManualJourneyTest {
    @get:Rule val folder = TemporaryFolder()
    private val manual = Journey("manual", 1000, 1000, 0.0, null, "나들이", "직접 남긴 기억", OdometerSource.MANUAL)
    private val manualEditor = JourneyEditor("manual", JourneyEditorMode.MANUAL, "나들이", "직접 남긴 기억")

    @Test fun manualMemoryCannotEarnMeasuredJourneyBadgeOrReplacePending() {
        val measured = Journey("measured", 1000, 2000, 1000.0, null, source = OdometerSource.TOTAL)
        val archive = JourneyArchive().observe(measured).saveManual(manualEditor, 1000)
        assertEquals(measured, archive.pending)
        assertNull(archive.firstSavedAt)
        assertEquals(archive, archive.saveManual(manualEditor, 1000))
        val store = JourneyStore(folder.newFolder())
        store.save(archive)
        assertEquals(archive, store.read())
        val savedMeasured = archive.savePending(measured.id)
        assertEquals(2000L, savedMeasured.firstSavedAt)
    }

    @Test fun manualMemoryWithMeasurementsIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { manual.copy(meters = 100.0) }
        assertThrows(IllegalArgumentException::class.java) { manual.copy(minutes = 1) }
    }

    @Test fun deletionDoesNotPermitDuplicateClaim() {
        val deleted = JourneyArchive().saveManual(manualEditor, 1000).deleteMemory(manual.id)
        assertEquals(deleted, deleted.saveManual(manualEditor, 1000))
        assertTrue(deleted.memories.isEmpty())
        assertNull(deleted.firstSavedAt)
    }
}
