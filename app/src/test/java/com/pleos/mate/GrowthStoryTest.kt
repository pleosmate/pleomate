package com.pleos.mate

import com.pleos.mate.feature.collection.domain.growthStories
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.ExperienceKind
import org.junit.Assert.*
import org.junit.Test

class GrowthStoryTest {
    @Test fun storiesUseExistingClaimsWithoutAddingRewards() {
        val progress = GrowthProgress().completeWind(123).completeVehicleMission("tire")
            .completeVehicleMission("battery").completeVehicleMission("hvac")
        assertEquals(listOf(true, true, false), growthStories(progress).map { it.completed })
        assertEquals(25, progress.totalXp)
        assertEquals(growthStories(progress), growthStories(progress))
    }
    @Test fun notesOnThreeDifferentDaysUnlockWeekStory() {
        var progress = GrowthProgress()
        for (day in listOf("2026-10-26", "2026-10-27")) progress = progress.completeExperience(ExperienceKind.NOTE, day, "함께한 날")
        assertFalse(growthStories(progress).last().completed)
        progress = progress.completeExperience(ExperienceKind.NOTE, "2026-10-28", "세 번째 날")
        assertTrue(growthStories(progress).last().completed)
        assertTrue(growthStories(progress).last().reunion.isNotBlank())
    }
    @Test fun retiredMissionsAndDifferentWeeksDoNotUnlockStories() {
        val progress = GrowthProgress(completedVehicleMissions = setOf("safety", "fuel", "car_info"))
            .completeExperience(ExperienceKind.NOTE, "2026-10-25", "하루")
            .completeExperience(ExperienceKind.NOTE, "2026-10-26", "다음 주")
            .completeExperience(ExperienceKind.NOTE, "2026-10-27", "이틀")
        assertTrue(growthStories(progress).none { it.completed })
    }
}
