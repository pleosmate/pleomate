package com.pleos.mate

import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExplorationGrowthTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun regionAndRewardSurviveReloadWithoutDuplicateXp() {
        val directory = folder.newFolder()
        val progress = GrowthProgress(exploredRegions = setOf("서울특별시"))
        GrowthStore(directory).save(progress)
        val restored = GrowthStore(directory).read()
        assertEquals(5, restored.totalXp)
        GrowthStore(directory).save(restored.copy(exploredRegions = restored.exploredRegions + "서울특별시"))
        assertEquals(5, GrowthStore(directory).read().totalXp)
        GrowthStore(directory).save(restored.copy(exploredRegions = restored.exploredRegions + "부산광역시"))
        assertEquals(10, GrowthStore(directory).read().totalXp)
    }


    @Test fun neighborhoodSouvenirsPersistAndAllowOnePerDay() {
        val directory = folder.newFolder()
        val first = GrowthProgress(exploredRegions = setOf("서울특별시"), explorationSouvenirs = setOf("1168064000|1|2026-11-01"))
        GrowthStore(directory).save(first)
        val restored = GrowthStore(directory).read()
        assertEquals(first, restored)
        assertEquals(10, restored.totalXp)
        assertEquals(null, restored.nextExplorationSouvenir("1168064000", "2026-11-01"))
        assertEquals(2, restored.nextExplorationSouvenir("1168064000", "2026-11-02"))
        assertEquals(1, restored.nextExplorationSouvenir("1168065000", "2026-11-01"))
        val complete = restored.copy(explorationSouvenirs = restored.explorationSouvenirs + setOf("1168064000|2|2026-11-02", "1168064000|3|2026-11-03"))
        assertEquals(null, complete.nextExplorationSouvenir("1168064000", "2026-11-04"))
    }

    @Test fun versionSevenRecordsRemainReadable() {
        val directory = folder.newFolder()
        java.io.File(directory, "companion-growth-v1").writeText("7\n0\nvisible\n0\n0\n0\n0\n0\n1\n서울특별시")
        val restored = GrowthStore(directory).read()
        assertEquals(setOf("서울특별시"), restored.exploredRegions)
        assertEquals(5, restored.totalXp)
        assertEquals(emptySet<String>(), restored.explorationSouvenirs)
    }

    @Test fun removingStoredRecordResetsRegionAndXp() {
        val directory = folder.newFolder()
        GrowthStore(directory).save(GrowthProgress(exploredRegions = setOf("서울특별시")))
        directory.listFiles()!!.forEach { it.delete() }
        assertEquals(GrowthProgress(), GrowthStore(directory).read())
    }
}
