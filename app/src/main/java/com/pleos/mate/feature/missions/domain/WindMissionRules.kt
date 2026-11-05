package com.pleos.mate.feature.missions.domain

/** Calibrated only for the user's Pleos emulator: slider 0% -> 0, 100% -> 10. */
object WindMissionRules {
    fun canStart(active: Boolean, loaded: Boolean, alreadyCompleted: Boolean, busy: Boolean) =
        active && loaded && !alreadyCompleted && !busy

    fun supported(min: Int?, max: Int?) = min == 0 && max == 10
    fun isOpen(raw: Int?) = raw != null && raw in 2..10
    fun canComplete(selected: Boolean, parkedAndAllowed: Boolean, raw: Int?) =
        selected && parkedAndAllowed && isOpen(raw)
}
