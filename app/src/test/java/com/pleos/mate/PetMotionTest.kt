package com.pleos.mate

import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionFrame
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PetMotionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun eachNewLevelUnlocksItsMotionAndKeepsEarlierOnes() {
        assertTrue(PetMotionRules.unlocked(1).isEmpty())
        assertEquals(listOf(PetMotion.WAVE), PetMotionRules.unlocked(2))
        assertEquals(listOf(PetMotion.WAVE, PetMotion.CURIOUS, PetMotion.JUMP), PetMotionRules.unlocked(4))
        assertEquals(4, PetMotionRules.unlocked(5).size)
        assertEquals(PetMotionRules.unlocked(5), PetMotionRules.unlocked(100))
        assertFalse(PetMotionRules.canPlay(4, PetMotion.CELEBRATE))
        assertTrue(PetMotionRules.canPlay(1, PetMotion.SMILE))
    }

    @Test fun naturalReactionsNeverSelectLockedMotions() {
        for (level in 1..20) {
            for (count in 1..8) assertTrue(PetMotionRules.canPlay(level, PetMotionRules.greeting(level, count)))
            assertTrue(PetMotionRules.canPlay(level, PetMotionRules.celebration(level)))
        }
        assertEquals(PetMotion.WAVE, PetMotionRules.greeting(3, 1))
        assertEquals(PetMotion.CURIOUS, PetMotionRules.greeting(3, 2))
        assertEquals(PetMotion.JUMP, PetMotionRules.celebration(4))
        assertEquals(PetMotion.CELEBRATE, PetMotionRules.celebration(5))
    }

    @Test fun levelJumpsAnnounceAllNewMotionsButLaterLevelsDoNotInventRewards() {
        assertEquals(listOf(PetMotion.CURIOUS, PetMotion.JUMP, PetMotion.CELEBRATE), PetMotionRules.newlyUnlocked(2, 5))
        assertTrue(PetMotionRules.levelUpMessage(4, 5).contains("특별 축하"))
        assertFalse(PetMotionRules.levelUpMessage(5, 6).contains("새 모션"))
    }

    @Test fun motionFramesHaveDistinctGesturesAndReturnToRest() {
        for (motion in PetMotion.entries) {
            assertEquals(PetMotionFrame(), PetMotionRules.frame(motion, 0f))
            assertEquals(PetMotionFrame(), PetMotionRules.frame(motion, 1f))
        }
        assertTrue(PetMotionRules.frame(PetMotion.WAVE, 0.5f).pawLift > 0f)
        assertTrue(PetMotionRules.frame(PetMotion.CURIOUS, 0.25f).headTilt > 0f)
        assertEquals(14f, PetMotionRules.frame(PetMotion.JUMP, 0.48f, PetCharacter.BEAR).jump, 0.001f)
        assertTrue(PetMotionRules.frame(PetMotion.CELEBRATE, 0.25f).sparkles > 0f)
        assertEquals(0f, PetMotionRules.frame(PetMotion.JUMP, 0.5f).sparkles, 0f)
    }

    @Test fun bearIsSlowerAndHeavierWhileRabbitHasEarFollowThrough() {
        for (motion in PetMotion.entries) {
            assertTrue(PetMotionRules.duration(PetCharacter.BEAR, motion) > PetMotionRules.duration(PetCharacter.RABBIT, motion))
            for (character in PetCharacter.entries) {
                assertEquals(PetMotionFrame(), PetMotionRules.frame(motion, 0f, character))
                assertEquals(PetMotionFrame(), PetMotionRules.frame(motion, 1f, character))
            }
        }
        val bear = PetMotionRules.frame(PetMotion.JUMP, 0.48f, PetCharacter.BEAR)
        val rabbit = PetMotionRules.frame(PetMotion.JUMP, 0.49f, PetCharacter.RABBIT)
        assertEquals(22f, rabbit.jump, 0.001f)
        assertTrue(bear.jump < rabbit.jump)
        assertEquals(0f, bear.earSway, 0f)
        assertTrue(kotlin.math.abs(rabbit.earSway) > 0f)
        assertTrue(PetMotionRules.frame(PetMotion.JUMP, 0.06f, PetCharacter.BEAR).squash >
            PetMotionRules.frame(PetMotion.JUMP, 0.06f, PetCharacter.RABBIT).squash)
    }

    @Test fun savedGrowthRestoresUnlocksAndLevelContinuesPastFive() {
        val dates = (0 until 22).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()).toString() }.toSet()
        val store = GrowthStore(folder.newFolder())
        store.save(GrowthProgress(dailyDoorCheckDates = dates))
        val restored = store.read()
        assertEquals(110, restored.totalXp)
        assertEquals(5, restored.level)
        assertTrue(PetMotionRules.canPlay(restored.level, PetMotion.CELEBRATE))
        val continued = restored.copy(dailyDoorCheckDates = (0 until 32).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()).toString() }.toSet())
        assertEquals(6, continued.level)
        assertEquals(210, continued.nextLevelXp)
    }
}
