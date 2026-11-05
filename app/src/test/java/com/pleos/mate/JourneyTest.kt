package com.pleos.mate

import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyTracker
import com.pleos.mate.feature.journey.domain.OdometerSample
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class JourneyTest {
    private fun sample(meters: Double = 1000.0, minutes: Int = 20, tick: Long = 1000) =
        OdometerSample(meters, minutes, 1_000_000 + tick, tick)
    private val trip = Journey("one", 1000, 9000, 5000.0, 8)

    @Test fun baselineDoesNotImportEarlierTravel() {
        val tracker = JourneyTracker("one")
        assertTrue(tracker.accept(sample()))
        assertNull(tracker.segment)
    }

    @Test fun recordsOnlyObservedMetersAndMinutes() {
        val tracker = JourneyTracker("one")
        tracker.accept(sample())
        tracker.accept(sample(6000.0, 28, 6000))
        val segment = requireNotNull(tracker.segment)
        assertEquals(5000.0, segment.meters, 0.0)
        assertEquals(8, segment.minutes)
        assertEquals(1_001_000, segment.startedAt)
        assertEquals(1_006_000, segment.endedAt)
    }

    @Test fun stationarySamplesDoNotCreateJourney() {
        val tracker = JourneyTracker("one")
        tracker.accept(sample())
        tracker.accept(sample(minutes = 21, tick = 6000))
        assertNull(tracker.segment)
    }

    @Test fun movementWithinSameMinuteIsAllowed() {
        val tracker = JourneyTracker("one")
        tracker.accept(sample())
        tracker.accept(sample(1010.0, tick = 6000))
        assertEquals(0, tracker.segment?.minutes)
    }

    @Test fun invalidValuesCannotStartObservation() {
        listOf(sample(Double.NaN), sample(Double.POSITIVE_INFINITY), sample(-1.0), sample(minutes = -1),
            sample().copy(wallMillis = 0), sample().copy(elapsedMillis = -1)).forEach {
            val tracker = JourneyTracker("one")
            assertFalse(tracker.accept(it))
            assertNull(tracker.segment)
            assertTrue(tracker.ended)
        }
    }

    private fun assertEndsBefore(bad: OdometerSample) {
        val tracker = JourneyTracker("one")
        tracker.accept(sample())
        tracker.accept(sample(1500.0, 21, 6000))
        val prefix = tracker.segment
        assertFalse(tracker.accept(bad))
        assertTrue(tracker.ended)
        assertEquals(prefix, tracker.segment)
        assertFalse(tracker.accept(sample(9000.0, 30, 30000)))
        assertEquals(prefix, tracker.segment)
    }

    @Test fun distanceResetPreservesOnlyKnownPrefix() = assertEndsBefore(sample(0.0, 22, 11000))
    @Test fun timeResetPreservesOnlyKnownPrefix() = assertEndsBefore(sample(1600.0, 0, 11000))
    @Test fun silentGapCannotBridgeUnknownTravel() = assertEndsBefore(sample(9000.0, 30, 21001))
    @Test fun outOfOrderSampleCannotRewritePrefix() = assertEndsBefore(sample(1600.0, 22, 5000))
    @Test fun duplicateTimestampEndsObservation() = assertEndsBefore(sample(1600.0, 22, 6000))
    @Test fun wallClockRollbackEndsObservation() = assertEndsBefore(sample(1600.0, 22, 11000).copy(wallMillis = 1))

    @Test fun saveConsumesPendingExactlyOnce() {
        val draft = JourneyArchive().observe(trip)
        val saved = draft.savePending(trip.id)
        assertNull(saved.pending)
        assertEquals(listOf(trip), saved.memories)
        assertEquals(saved, saved.savePending(trip.id).observe(trip))
    }

    @Test fun skippingConsumesWithoutCreatingMemory() {
        val skipped = JourneyArchive().observe(trip).skipPending(trip.id)
        assertNull(skipped.pending)
        assertTrue(skipped.memories.isEmpty())
        assertEquals(skipped, skipped.observe(trip))
    }

    @Test fun deletingMemoryDoesNotAllowReplay() {
        val removed = JourneyArchive().observe(trip).savePending(trip.id).deleteMemory(trip.id)
        assertTrue(removed.memories.isEmpty())
        assertTrue(trip.id in removed.handledIds)
        assertEquals(removed, removed.observe(trip).savePending(trip.id))
    }

    @Test fun unrelatedActionDoesNotConsumePending() {
        val draft = JourneyArchive().observe(trip)
        assertEquals(draft, draft.savePending("other").skipPending("other").deleteMemory("other"))
    }

    @Test fun differentJourneyCannotReplaceUnreviewedPending() {
        val draft = JourneyArchive().observe(trip)
        assertThrows(IllegalArgumentException::class.java) { draft.observe(trip.copy(id = "two")) }
    }

    private fun withStore(block: (JourneyStore, File) -> Unit) {
        val dir = Files.createTempDirectory("pleos-journey-test").toFile()
        try { block(JourneyStore(dir), dir) } finally { dir.deleteRecursively() }
    }

    @Test fun emptyStoreHasNoSyntheticJourney() = withStore { store, _ -> assertEquals(JourneyArchive(), store.read()) }

    @Test fun pendingSurvivesStoreRecreation() = withStore { store, dir ->
        val draft = JourneyArchive().observe(trip)
        store.save(draft)
        assertEquals(draft, JourneyStore(dir).read())
    }

    @Test fun savedAndDeletedIdsSurviveRecreation() = withStore { store, dir ->
        val second = trip.copy(id = "two")
        val archive = JourneyArchive().observe(trip).savePending(trip.id).observe(second).savePending(second.id).deleteMemory(trip.id)
        store.save(archive)
        val restored = JourneyStore(dir).read()
        assertEquals(archive, restored)
        assertEquals(restored, restored.observe(trip))
    }

    @Test fun interruptedTemporaryWriteCannotReplaceCommittedArchive() = withStore { store, dir ->
        val archive = JourneyArchive().observe(trip)
        store.save(archive)
        File(dir, "journeys-v1.pending").writeBytes(byteArrayOf(0, 1, 2))
        assertEquals(archive, store.read())
        store.save(archive.savePending(trip.id))
        assertEquals(listOf(trip), store.read().memories)
    }

    @Test fun uncommittedFirstWriteIsNotRecoveredAsFact() = withStore { store, dir ->
        File(dir, "journeys-v1.pending").writeText("unfinished")
        assertEquals(JourneyArchive(), store.read())
    }

    @Test fun corruptRecordIsNotSilentlyReset() = withStore { store, dir ->
        val record = File(dir, "journeys-v1").apply { writeText("broken") }
        assertThrows(Exception::class.java) { store.read() }
        assertEquals("broken", record.readText())
    }

    @Test fun truncatedRecordIsRejected() = withStore { store, dir ->
        store.save(JourneyArchive().observe(trip))
        val record = File(dir, "journeys-v1")
        record.writeBytes(record.readBytes().dropLast(3).toByteArray())
        assertThrows(Exception::class.java) { store.read() }
    }
}
