package com.pleos.mate

import com.pleos.mate.feature.companion.domain.PetCollection
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.ExperienceKind
import org.junit.Assert.*
import org.junit.Test

class PetCollectionTest {
    @Test fun decorationsRequireActualAchievements() {
        val fresh = GrowthProgress()
        assertFalse(PetCollection.outfitUnlocked("breeze", fresh, false))
        assertTrue(PetCollection.outfitUnlocked("breeze", fresh.completeWind(1), false))
        assertFalse(PetCollection.outfitUnlocked("bow", fresh, false))
        assertTrue(PetCollection.outfitUnlocked("bow", fresh, true))
        assertFalse(PetCollection.roomUnlocked("cushion", fresh, false))
        assertTrue(PetCollection.roomUnlocked("cushion", fresh.completeWind(1), false))
        assertFalse(PetCollection.roomUnlocked("unknown", fresh, true))
    }
    @Test fun reunionNeverInventsOrRestoresDeletedMemories() {
        val fresh = GrowthProgress()
        assertTrue(PetCollection.reunion(fresh, emptyList(), 0).contains("다시 만나서"))
        val wind = fresh.completeWind(1)
        assertTrue(PetCollection.reunion(wind, emptyList(), 0).contains("첫 바람"))
        assertFalse(PetCollection.reunion(wind.hideWindMemory(), emptyList(), 0).contains("첫 바람"))
        val note = fresh.completeExperience(ExperienceKind.NOTE, "2026-10-30", "행복한 날")
        assertTrue(PetCollection.reunion(note, emptyList(), Int.MIN_VALUE).contains("행복한 날"))
        assertFalse(PetCollection.reunion(fresh, emptyList(), 0).contains("행복한 날"))
    }
    @Test fun stageChangesAtPromisedLevels() {
        assertEquals("작은 동료", PetCollection.stage(1))
        assertEquals("작은 동료", PetCollection.stage(4))
        assertEquals("든든한 동료", PetCollection.stage(5))
        assertEquals("든든한 동료", PetCollection.stage(14))
        assertEquals("빛나는 동료", PetCollection.stage(15))
    }
}
