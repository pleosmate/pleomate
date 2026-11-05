package com.pleos.mate

import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.WindMissionRules
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WindMissionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun onlyCalibratedBoundsAreSupported() {
        assertTrue(WindMissionRules.supported(0, 10))
        assertFalse(WindMissionRules.supported(0, 100))
        assertFalse(WindMissionRules.supported(null, null))
    }

    @Test fun passiveWindowDetectionAcceptsOnlyValidOpenReadings() {
        for (raw in listOf(null, -1, 0, 1, 11)) assertFalse(WindMissionRules.isOpen(raw))
        for (raw in 2..10) assertTrue(WindMissionRules.isOpen(raw))
    }

    @Test fun requiresSelectionParkingAndValidOpenValue() {
        assertFalse(WindMissionRules.canComplete(false, true, 10))
        assertFalse(WindMissionRules.canComplete(true, false, 10))
        for (raw in listOf(null, -1, 0, 1, 11)) assertFalse(WindMissionRules.canComplete(true, true, raw))
        for (raw in 2..10) assertTrue(WindMissionRules.canComplete(true, true, raw))
    }

    @Test fun alreadyOpenWindowCanCompleteWithoutAnotherManipulation() {
        assertTrue(WindMissionRules.canComplete(true, true, 7))
    }

    @Test fun repeatedCompletionsNeverIncreaseXpOrReplaceFirstTime() {
        val first = GrowthProgress().completeWind(1234)
        var replayed = first
        repeat(100) { replayed = replayed.completeWind(5678) }
        assertEquals(first, replayed)
        assertEquals(10, replayed.totalXp)
        assertEquals(2, replayed.level)
        assertEquals(1234L, replayed.windCompletedAt)
    }

    @Test fun freshAndCompletedProgressHaveExpectedLevelBoundaries() {
        val fresh = GrowthProgress()
        assertEquals(0, fresh.totalXp)
        assertEquals(1, fresh.level)
        assertEquals(10, fresh.nextLevelXp)
        assertEquals(30, fresh.completeWind(1).nextLevelXp)
    }

    @Test fun durableRecordRestoresMissionAndXpTogether() {
        val directory = folder.newFolder()
        val store = GrowthStore(directory)
        assertEquals(GrowthProgress(), store.read())
        val earned = store.read().completeWind(1234)
        store.save(earned)
        val restored = GrowthStore(directory).read()
        assertEquals(earned, restored)
        assertEquals(10, restored.totalXp)
        assertEquals(earned, restored.completeWind(9999))
    }

    @Test fun interruptedUncommittedWriteDoesNotAwardAnything() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1.pending").writeText("1234")
        assertEquals(GrowthProgress(), GrowthStore(directory).read())
    }

    @Test fun interruptedReplacementKeepsPreviousRewardRecord() {
        val directory = folder.newFolder()
        val store = GrowthStore(directory)
        store.save(GrowthProgress().completeWind(1234))
        File(directory, "companion-growth-v1.pending").writeText("9999")
        assertEquals(1234L, GrowthStore(directory).read().windCompletedAt)
    }

    @Test(expected = NumberFormatException::class) fun corruptRecordFailsInsteadOfResettingRewards() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("damaged")
        GrowthStore(directory).read()
    }

    @Test fun legacyRewardBecomesFirstMemoryWithoutAwardingAgain() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("1234")
        val restored = GrowthStore(directory).read()
        assertEquals(1234L, restored.windMemoryAt)
        assertTrue(restored.canReuniteWithWind)
        assertEquals(restored, restored.completeWind(9999))
        assertEquals(10, restored.totalXp)
    }

    @Test fun noMemoryOrReunionBeforeARealCompletion() {
        val fresh = GrowthProgress()
        assertNull(fresh.windMemoryAt)
        assertFalse(fresh.canReuniteWithWind)
        assertEquals(fresh, fresh.hideWindMemory())
    }

    @Test fun deletingMemoryPreservesRewardsAndExcludesReunionAfterRestart() {
        val directory = folder.newFolder()
        val store = GrowthStore(directory)
        store.save(GrowthProgress().completeWind(1234))
        store.save(store.read().hideWindMemory())
        val restored = GrowthStore(directory).read()
        assertNull(restored.windMemoryAt)
        assertFalse(restored.canReuniteWithWind)
        assertEquals(2, restored.level)
        assertEquals(10, restored.totalXp)
        assertEquals(restored, restored.completeWind(9999))
        assertNull(restored.completeWind(9999).windMemoryAt)
    }

    @Test fun viewingRestoredMemoryKeepsOriginalDateAndXp() {
        val directory = folder.newFolder()
        val store = GrowthStore(directory)
        store.save(GrowthProgress().completeWind(1234))
        repeat(10) {
            assertEquals(1234L, store.read().windMemoryAt)
            assertEquals(10, store.read().totalXp)
        }
    }

    @Test fun failedDeletionLeavesTheCommittedMemoryAvailable() {
        val directory = folder.newFolder()
        val store = GrowthStore(directory)
        val earned = GrowthProgress().completeWind(1234)
        store.save(earned)
        File(directory, "companion-growth-v1.pending").mkdir()
        try {
            store.save(earned.hideWindMemory())
            fail("Writing to a directory should fail")
        } catch (_: java.io.IOException) { }
        assertEquals(earned, store.read())
        assertTrue(store.read().canReuniteWithWind)
    }

    @Test(expected = IllegalArgumentException::class) fun unknownMemoryStateDoesNotResetTheRecord() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("1234\ninvalid")
        GrowthStore(directory).read()
    }


    @Test fun temporaryMissionChecksDoNotChangeTheCompanionRecord() {
        val companion = GrowthStore(folder.newFolder("companion"))
        val temporary = GrowthStore(folder.newFolder("mission-check"))
        val earned = GrowthProgress().completeWind(1234)
        companion.save(earned)
        assertEquals(GrowthProgress(), temporary.read())
        temporary.save(GrowthProgress().completeWind(9999).hideWindMemory())
        assertEquals(earned, companion.read())
        assertTrue(companion.read().canReuniteWithWind)
        assertEquals(10, companion.read().totalXp)
    }

}
