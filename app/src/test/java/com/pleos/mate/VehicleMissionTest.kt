package com.pleos.mate

import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.VehicleMissionRules
import com.pleos.mate.feature.missions.domain.vehicleMissions
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VehicleMissionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun eachMissionAwardsOnlyOnceAndAdvancesLevel() {
        var progress = GrowthProgress().completeWind(123)
        vehicleMissions.forEach { item ->
            val before = progress.totalXp
            progress = progress.completeVehicleMission(item.id)
            assertEquals(before + 5, progress.totalXp)
            assertEquals(progress, progress.completeVehicleMission(item.id))
        }
        assertEquals(60, progress.totalXp)
        assertEquals(4, progress.level)
    }

    @Test fun rewardsSurviveRestartAlongsideExistingProgress() {
        val store = GrowthStore(folder.newFolder())
        val progress = GrowthProgress().completeWind(123).completeDailyDoorCheck("2026-10-27")
            .completeVehicleMission("tire").completeVehicleMission("hvac")
        store.save(progress)
        assertEquals(progress, store.read())
        assertEquals(25, store.read().totalXp)
    }

    @Test fun dailyDoorChecksAwardWeeklyBonusOnThirdDistinctDayOnly() {
        val first = GrowthProgress().completeDailyDoorCheck("2026-10-26")
        assertEquals(5, first.totalXp)
        assertEquals(1, first.doorChecksThisWeek("2026-W44"))
        assertFalse(first.hasWeeklyDoorBonus("2026-W44"))

        val second = first.completeDailyDoorCheck("2026-10-27")
        assertEquals(10, second.totalXp)
        assertEquals(2, second.doorChecksThisWeek("2026-W44"))
        assertFalse(second.hasWeeklyDoorBonus("2026-W44"))

        val third = second.completeDailyDoorCheck("2026-10-28")
        assertEquals(30, third.totalXp)
        assertEquals(3, third.doorChecksThisWeek("2026-W44"))
        assertTrue(third.hasWeeklyDoorBonus("2026-W44"))

        assertEquals(third, third.completeDailyDoorCheck("2026-10-28"))
        val fourth = third.completeDailyDoorCheck("2026-10-29")
        assertEquals(35, fourth.totalXp)
        assertEquals(setOf("2026-W44"), fourth.weeklyDoorBonusWeeks)
    }

    @Test fun doorWeeklyProgressDoesNotMixIsoWeeksAndPersistsTheBonus() {
        var progress = GrowthProgress().completeDailyDoorCheck("2026-10-25")
            .completeDailyDoorCheck("2026-10-26")
        assertEquals(1, progress.doorChecksThisWeek("2026-W44"))
        progress = progress.completeDailyDoorCheck("2026-10-27")
        assertFalse(progress.hasWeeklyDoorBonus("2026-W44"))
        progress = progress.completeDailyDoorCheck("2026-10-28")
        assertTrue(progress.hasWeeklyDoorBonus("2026-W44"))

        val store = GrowthStore(folder.newFolder())
        store.save(progress)
        val restored = store.read()
        assertTrue(restored.hasWeeklyDoorBonus("2026-W44"))
        assertEquals(3, restored.doorChecksThisWeek("2026-W44"))
        val nextDay = restored.completeDailyDoorCheck("2026-10-29")
        assertEquals(40, nextDay.totalXp)
        assertEquals(setOf("2026-W44"), nextDay.weeklyDoorBonusWeeks)
    }

    @Test fun readsPreviousVersionWithoutLosingRewards() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("2\n123\nvisible\n1\n2026-10-27\n0")
        val store = GrowthStore(directory)
        assertEquals(15, store.read().totalXp)
        val migrated = store.read().completeVehicleMission("tire")
        store.save(migrated)
        assertEquals(migrated, store.read())
    }

    @Test fun discardsOldLearningClaimsInsteadOfCountingThemAsVehicleChecks() {
        val directory = folder.newFolder()
        File(directory, "companion-growth-v1").writeText("3\n123\nvisible\n0\n0\n1\ntire")
        assertEquals(10, GrowthStore(directory).read().totalXp)
        assertTrue(GrowthStore(directory).read().completedVehicleMissions.isEmpty())
    }

    @Test fun rejectsInvalidVehicleNumbers() {
        assertFalse(VehicleMissionRules.validNonNegative(null))
        assertFalse(VehicleMissionRules.validNonNegative(Float.NaN))
        assertFalse(VehicleMissionRules.validNonNegative(Float.POSITIVE_INFINITY))
        assertFalse(VehicleMissionRules.validNonNegative(-1f))
        assertTrue(VehicleMissionRules.validNonNegative(0f))
        assertFalse(VehicleMissionRules.validPercent(101))
        assertFalse(VehicleMissionRules.validPercent(-1))
        assertTrue(VehicleMissionRules.validPercent(0))
        assertTrue(VehicleMissionRules.validPercent(100))
        assertFalse(VehicleMissionRules.validTemperature(100f))
        assertTrue(VehicleMissionRules.validTemperature(22f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownMissionsCannotAwardXp() {
        GrowthProgress().completeVehicleMission("unknown")
    }
}
