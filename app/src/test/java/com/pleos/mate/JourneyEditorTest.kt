package com.pleos.mate

import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyEditor
import com.pleos.mate.feature.journey.domain.JourneyEditorMode
import com.pleos.mate.feature.journey.domain.OdometerSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JourneyEditorTest {
    private val measured = Journey("trip", 1000, 2000, 800.0, 2, "나들이", "원래 기록", OdometerSource.TRIP)

    @Test fun storedModesStayCompatibleAndUnknownDraftModeDefaultsToManual() {
        assertEquals(JourneyEditorMode.PENDING, JourneyEditorMode.fromStorage("pending"))
        assertEquals(JourneyEditorMode.EDIT, JourneyEditorMode.fromStorage("edit"))
        assertEquals(JourneyEditorMode.MANUAL, JourneyEditorMode.fromStorage("manual"))
        assertEquals(JourneyEditorMode.MANUAL, JourneyEditorMode.fromStorage("unknown"))
    }

    @Test fun editorOwnsTextLimitsAndManualJourneyCreation() {
        val editor = JourneyEditor("manual", JourneyEditorMode.MANUAL)
            .updateText("a".repeat(40), "b".repeat(220))
        assertEquals(30, editor.theme.length)
        assertEquals(200, editor.memo.length)
        assertEquals(Journey("manual", 3000, 3000, 0.0, null, editor.theme, editor.memo, OdometerSource.MANUAL), editor.toManualJourney(3000))
        assertThrows(IllegalArgumentException::class.java) {
            JourneyEditor("pending", JourneyEditorMode.PENDING).toManualJourney(3000)
        }
    }

    @Test fun archiveEditChangesOnlyUserAuthoredMetadata() {
        val archive = JourneyArchive(pending = null, memories = listOf(measured), handledIds = setOf(measured.id))
        val editor = JourneyEditor(measured.id, JourneyEditorMode.EDIT, "새 제목", "  새 메모  ")
        val edited = archive.editMemory(editor)
        assertEquals(measured.copy(theme = "새 제목", memo = "새 메모"), edited.memories.single())
        assertEquals(setOf(measured.id), edited.handledIds)
        assertThrows(IllegalArgumentException::class.java) {
            archive.editMemory(JourneyEditor("trip", JourneyEditorMode.MANUAL))
        }
    }
}
