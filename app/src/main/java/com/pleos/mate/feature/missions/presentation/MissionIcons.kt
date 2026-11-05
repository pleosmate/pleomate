package com.pleos.mate.feature.missions.presentation

import androidx.annotation.DrawableRes
import com.pleos.mate.R
import com.pleos.mate.feature.missions.domain.ExperienceKind

@DrawableRes
internal fun vehicleMissionIcon(id: String): Int = when (id) {
    "odometer" -> R.drawable.ic_mission_gauge
    "tire" -> R.drawable.ic_mission_tire
    "safety" -> R.drawable.ic_mission_shield
    "battery" -> R.drawable.ic_mission_battery
    "light" -> R.drawable.ic_mission_bulb
    "hvac" -> R.drawable.ic_mission_thermometer
    "wiper" -> R.drawable.ic_mission_wiper
    "seat" -> R.drawable.ic_mission_seat
    "driving_mode" -> R.drawable.ic_mission_drive
    "fuel" -> R.drawable.ic_mission_fuel
    "brake" -> R.drawable.ic_mission_brake
    "turn_signal" -> R.drawable.ic_mission_turn
    "mirror" -> R.drawable.ic_mission_mirror
    else -> R.drawable.ic_mission_car
}

@DrawableRes
internal fun experienceMissionIcon(kind: ExperienceKind): Int = when (kind) {
    ExperienceKind.PLACE -> R.drawable.ic_mission_pin
    ExperienceKind.DESTINATION -> R.drawable.ic_mission_flag
    ExperienceKind.NOTE -> R.drawable.ic_mission_mic
    ExperienceKind.GREETING -> R.drawable.ic_mission_voice
}
