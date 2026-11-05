package com.pleos.mate.feature.missions.domain

internal object DoorMissionRules {
    fun canStart(active: Boolean, loaded: Boolean, parked: Boolean, checking: Boolean, busy: Boolean) =
        active && loaded && parked && !checking && !busy

    fun earnsWeeklyBonus(hasBonus: Boolean, completedDaysThisWeek: Int) =
        !hasBonus && completedDaysThisWeek == 2
}
