package com.pleos.mate

import com.pleos.mate.feature.journey.data.local.JourneyStore
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JourneyMetadataTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun metadataSurvivesSaveAndDeletionKeepsConsumedId() {
        val store = JourneyStore(folder.newFolder())
        val journey = Journey("trip", 1000, 2000, 500.0, 1, "나들이", "즐거운 하루")
        val archive = JourneyArchive().observe(journey).savePending(journey.id)
        store.save(archive)
        assertEquals(archive, store.read())
        store.save(store.read().deleteMemory(journey.id))
        assertEquals(emptyList<Journey>(), store.read().memories)
        assertEquals(setOf(journey.id), store.read().handledIds)
        assertEquals(store.read(), store.read().observe(journey))
    }
}
