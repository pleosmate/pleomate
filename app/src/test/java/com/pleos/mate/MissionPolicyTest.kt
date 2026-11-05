package com.pleos.mate

import com.pleos.mate.feature.missions.domain.DoorMissionRules
import com.pleos.mate.feature.missions.domain.VehicleMissionRules
import com.pleos.mate.feature.missions.domain.WindMissionRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionPolicyTest {
    @Test fun windMissionRequiresAnActiveLoadedSessionAndNoPriorCompletionOrConflict() {
        assertTrue(WindMissionRules.canStart(active = true, loaded = true, alreadyCompleted = false, busy = false))
        assertFalse(WindMissionRules.canStart(active = false, loaded = true, alreadyCompleted = false, busy = false))
        assertFalse(WindMissionRules.canStart(active = true, loaded = false, alreadyCompleted = false, busy = false))
        assertFalse(WindMissionRules.canStart(active = true, loaded = true, alreadyCompleted = true, busy = false))
        assertFalse(WindMissionRules.canStart(active = true, loaded = true, alreadyCompleted = false, busy = true))
    }

    @Test fun doorMissionRequiresParkedIdleSessionAndAwardsWeeklyBonusOnlyOnThirdDay() {
        assertTrue(DoorMissionRules.canStart(active = true, loaded = true, parked = true, checking = false, busy = false))
        assertFalse(DoorMissionRules.canStart(active = true, loaded = true, parked = false, checking = false, busy = false))
        assertFalse(DoorMissionRules.canStart(active = true, loaded = true, parked = true, checking = true, busy = false))
        assertFalse(DoorMissionRules.canStart(active = true, loaded = true, parked = true, checking = false, busy = true))
        assertTrue(DoorMissionRules.earnsWeeklyBonus(hasBonus = false, completedDaysThisWeek = 2))
        assertFalse(DoorMissionRules.earnsWeeklyBonus(hasBonus = true, completedDaysThisWeek = 2))
        assertFalse(DoorMissionRules.earnsWeeklyBonus(hasBonus = false, completedDaysThisWeek = 1))
        assertFalse(DoorMissionRules.earnsWeeklyBonus(hasBonus = false, completedDaysThisWeek = 3))
    }

    @Test fun vehicleMissionRequiresKnownMissionAndFreshReading() {
        assertTrue(VehicleMissionRules.canStart(active = true, loaded = true, parked = true, busy = false, knownMission = true))
        assertFalse(VehicleMissionRules.canStart(active = true, loaded = true, parked = true, busy = false, knownMission = false))
        assertFalse(VehicleMissionRules.canStart(active = true, loaded = true, parked = true, busy = true, knownMission = true))
        assertTrue(VehicleMissionRules.readingIsFresh(readAt = 10_000L, now = 70_000L))
        assertFalse(VehicleMissionRules.readingIsFresh(readAt = 10_000L, now = 70_001L))
    }
}
