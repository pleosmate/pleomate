package com.pleos.mate

import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExperienceMissionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun dailyClaimsCannotBeRepeatedButNextDayCanEarnAgain() {
        val first = GrowthProgress().completeExperience(ExperienceKind.NOTE, "2026-10-27", "좋은 하루")
        assertEquals(5, first.totalXp)
        assertEquals(first, first.completeExperience(ExperienceKind.NOTE, "2026-10-27", "다른 내용"))
        assertEquals(10, first.completeExperience(ExperienceKind.NOTE, "2026-10-28", "두 번째 날").totalXp)
        assertEquals("좋은 하루", first.companionMemories.single().text)
    }

    @Test fun greetingIsOnceForTheCompanionAcrossDates() {
        val first = GrowthProgress().completeExperience(ExperienceKind.GREETING, "2026-10-27", "안녕")
        assertEquals(first, first.completeExperience(ExperienceKind.GREETING, "2026-10-28", "또 안녕"))
        assertTrue(first.hasExperience(ExperienceKind.GREETING, "2027-01-01"))
    }

    @Test fun threeDifferentDaysAutomaticallyAwardOneWeeklyBonus() {
        var progress = GrowthProgress()
        for (kind in listOf(ExperienceKind.PLACE, ExperienceKind.NOTE, ExperienceKind.DESTINATION)) {
            progress = progress.completeExperience(kind, "2026-10-26", kind.name)
        }
        assertEquals(20, progress.totalXp)
        progress = progress.completeExperience(ExperienceKind.NOTE, "2026-10-27", "둘째 날")
        assertEquals(25, progress.totalXp)
        progress = progress.completeExperience(ExperienceKind.PLACE, "2026-10-28", "셋째 날")
        assertEquals(45, progress.totalXp)
        assertEquals(setOf("2026-W44"), ExperienceRules.bonusWeeks(progress.companionMemories))
        assertEquals(50, progress.completeExperience(ExperienceKind.NOTE, "2026-10-29", "넷째 날").totalXp)
    }

    @Test fun greetingDoesNotCountAsADayOfMemoryAndWeeksDoNotMix() {
        val progress = GrowthProgress().completeExperience(ExperienceKind.GREETING, "2026-10-26", "안녕")
            .completeExperience(ExperienceKind.PLACE, "2026-10-27", "공원")
            .completeExperience(ExperienceKind.NOTE, "2026-10-28", "좋았어")
            .completeExperience(ExperienceKind.NOTE, "2026-11-02", "다음 주")
        assertEquals(20, progress.totalXp)
        assertTrue(ExperienceRules.bonusWeeks(progress.companionMemories).isEmpty())
    }

    @Test fun staleFutureAndInvalidLocationCannotComplete() {
        assertTrue(ExperienceRules.fresh(1000, 121000))
        assertFalse(ExperienceRules.fresh(1000, 121001))
        assertFalse(ExperienceRules.fresh(2000, 1000))
        assertFalse(ExperienceRules.fresh(0, 1000))
        assertFalse(ExperienceRules.validCoordinate(Double.NaN, 127.0))
        assertFalse(ExperienceRules.validCoordinate(91.0, 127.0))
        assertFalse(ExperienceRules.validCoordinate(37.0, 181.0))
    }

    @Test fun destinationRequiresProximityInsteadOfRouteCancellation() {
        assertTrue(ExperienceRules.near(37.0, 127.0, 37.0001, 127.0))
        assertFalse(ExperienceRules.near(37.0, 127.0, 37.01, 127.0))
        assertFalse(ExperienceRules.near(37.0, 127.0, Double.NaN, 127.0))
    }

    @Test fun storesTextAndClaimsAtomicallyWithExistingRewards() {
        val store = GrowthStore(folder.newFolder())
        val progress = GrowthProgress().completeWind(123).completeVehicleMission("tire")
            .completeExperience(ExperienceKind.NOTE, "2026-10-27", "한글 | 기호\n여러 줄도 저장해요")
        store.save(progress)
        assertEquals(progress, store.read())
        assertEquals(20, store.read().totalXp)
        assertEquals(progress, store.read().completeExperience(ExperienceKind.NOTE, "2026-10-27", "중복"))
    }

    @Test fun readsPreviousVehicleMissionRecords() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("4\n123\nvisible\n0\n0\n1\ntire")
        val store = GrowthStore(directory)
        assertEquals(15, store.read().totalXp)
        assertEquals(setOf("tire"), store.read().completedVehicleMissions)
        store.save(store.read().completeExperience(ExperienceKind.PLACE, "2026-10-27", "집 근처"))
        assertEquals(20, store.read().totalXp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankMemoriesDoNotEarnRewards() {
        GrowthProgress().completeExperience(ExperienceKind.NOTE, "2026-10-27", "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedMemoriesDoNotEarnRewards() {
        GrowthProgress().completeExperience(ExperienceKind.NOTE, "2026-10-27", "a".repeat(201))
    }
}
