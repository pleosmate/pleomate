package com.pleos.mate

import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyTracker
import com.pleos.mate.feature.journey.domain.OdometerSample
import com.pleos.mate.feature.journey.domain.OdometerSource
import java.io.DataOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JourneyFallbackTest {
    @get:Rule val folder = TemporaryFolder()
    private fun total(km: Double, tick: Long) = OdometerSample(km * 1000, null, 100000 + tick, tick, OdometerSource.TOTAL)

    @Test fun totalDistanceUsesDifferenceAndKeepsTimeUnknown() {
        val tracker = JourneyTracker("total")
        assertTrue(tracker.accept(total(100.0, 1000)))
        assertNull(tracker.segment)
        assertTrue(tracker.accept(total(101.0, 6000)))
        assertEquals(1000.0, tracker.segment!!.meters, 0.0)
        assertNull(tracker.segment!!.minutes)
        val store = JourneyStore(folder.newFolder())
        val archive = JourneyArchive().observe(tracker.segment!!).savePending("total")
        store.save(archive)
        assertEquals(archive, store.read())
    }

    @Test fun sourceChangesAndResetsNeverJoinIntervals() {
        val tracker = JourneyTracker("reset")
        tracker.accept(total(100.0, 1000))
        tracker.accept(total(101.0, 6000))
        assertFalse(tracker.accept(total(10.0, 11000)))
        assertEquals(1000.0, tracker.segment!!.meters, 0.0)
        val switched = JourneyTracker("switch")
        switched.accept(total(100.0, 1000))
        assertFalse(switched.accept(OdometerSample(101000.0, 1, 106000, 6000)))
        assertNull(switched.segment)
    }

    @Test fun versionThreeMemoryRetainsMetadataAndTime() {
        val dir = folder.newFolder()
        DataOutputStream(File(dir, "journeys-v1").outputStream()).use {
            it.writeInt(3); it.writeBoolean(false); it.writeInt(1)
            it.writeUTF("old"); it.writeLong(1000); it.writeLong(2000); it.writeDouble(500.0); it.writeInt(1)
            it.writeUTF("나들이"); it.writeUTF("기억")
            it.writeInt(1); it.writeUTF("old"); it.writeBoolean(true); it.writeLong(2000)
        }
        val store = JourneyStore(dir)
        val old = store.read()
        assertEquals("나들이", old.memories.single().theme)
        assertEquals(OdometerSource.TRIP, old.memories.single().source)
        assertEquals(1, old.memories.single().minutes)
        store.save(old)
        assertEquals(old, store.read())
    }
}
