package com.pleos.mate.feature.growth.domain

import com.pleos.mate.feature.missions.domain.CompanionMemory
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.MissionSchedule
import com.pleos.mate.feature.missions.domain.vehicleMissions

/** One shared companion progression, retained when the selected species changes. */
data class GrowthProgress(
    val windCompletedAt: Long? = null,
    val windMemoryHidden: Boolean = false,
    val dailyDoorCheckDates: Set<String> = emptySet(),
    val weeklyDoorBonusWeeks: Set<String> = emptySet(),
    val completedVehicleMissions: Set<String> = emptySet(),
    val companionMemories: List<CompanionMemory> = emptyList(),
    val dailyWindDates: Set<String> = emptySet(),
    val exploredRegions: Set<String> = emptySet(),
    val explorationSouvenirs: Set<String> = emptySet()
) {
    val totalXp: Int get() = (exploredRegions.size + explorationSouvenirs.size) * 5 + (if (windCompletedAt == null) 0 else 10) + dailyWindDates.size * 2 + dailyDoorCheckDates.size * 5 + weeklyDoorBonusWeeks.size * 15 + completedVehicleMissions.size * 5 + companionMemories.sumOf { it.kind.xp } + ExperienceRules.bonusWeeks(companionMemories).size * 15
    val level: Int get() = when {
        totalXp < 10 -> 1
        totalXp < 30 -> 2
        totalXp < 60 -> 3
        else -> 4 + (totalXp - 60) / 50
    }
    val levelStartXp: Int get() = when (level) {
        1 -> 0
        2 -> 10
        3 -> 30
        else -> 60 + (level - 4) * 50
    }
    val nextLevelXp: Int get() = when (level) {
        1 -> 10
        2 -> 30
        3 -> 60
        else -> levelStartXp + 50
    }
    val windMemoryAt: Long? get() = windCompletedAt?.takeUnless { windMemoryHidden }
    val canReuniteWithWind: Boolean get() = windMemoryAt != null
    fun nextExplorationSouvenir(areaId: String, date: String): Int? {
        val records = explorationSouvenirs.filter { it.startsWith("$areaId|") }
        if (records.any { it.substringAfterLast('|') == date }) return null
        return (1..3).firstOrNull { slot -> records.none { it.split('|')[1] == slot.toString() } }
    }

    fun hasDailyDoorCheck(dateKey: String = MissionSchedule.today()) = dateKey in dailyDoorCheckDates
    fun doorChecksThisWeek(weekKey: String = MissionSchedule.week(MissionSchedule.today())) =
        dailyDoorCheckDates.count { MissionSchedule.week(it) == weekKey }
    fun hasWeeklyDoorBonus(weekKey: String = MissionSchedule.week(MissionSchedule.today())) = weekKey in weeklyDoorBonusWeeks

    fun completeWind(now: Long): GrowthProgress =
        if (windCompletedAt != null) this else copy(windCompletedAt = now)

    fun hasDailyWind(date: String = MissionSchedule.today()): Boolean = date in dailyWindDates ||
        windCompletedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() == date } == true

    fun completeDailyWind(now: Long, date: String = MissionSchedule.today()): GrowthProgress {
        java.time.LocalDate.parse(date)
        if (hasDailyWind(date)) return this
        return if (windCompletedAt == null) completeWind(now)
        else copy(dailyWindDates = dailyWindDates + date)
    }

    fun completeDailyDoorCheck(dateKey: String): GrowthProgress {
        if (dateKey in dailyDoorCheckDates) return this
        val checks = dailyDoorCheckDates + dateKey
        val weekKey = MissionSchedule.week(dateKey)
        val bonuses = if (checks.count { MissionSchedule.week(it) == weekKey } >= 3) weeklyDoorBonusWeeks + weekKey else weeklyDoorBonusWeeks
        return copy(dailyDoorCheckDates = checks, weeklyDoorBonusWeeks = bonuses)
    }

    fun completeVehicleMission(id: String): GrowthProgress {
        require(vehicleMissions.any { it.id == id })
        return if (id in completedVehicleMissions) this
        else copy(completedVehicleMissions = completedVehicleMissions + id)
    }

    fun hasExperience(kind: ExperienceKind, date: String = MissionSchedule.today()) =
        companionMemories.any { it.kind == kind && (kind == ExperienceKind.GREETING || it.date == date) }

    fun completeExperience(kind: ExperienceKind, date: String, text: String): GrowthProgress {
        java.time.LocalDate.parse(date)
        require(text.isNotBlank() && text.length <= ExperienceRules.MAX_TEXT)
        if (hasExperience(kind, date)) return this
        return copy(companionMemories = companionMemories + CompanionMemory(kind, date, text.trim()))
    }

    fun hideWindMemory(): GrowthProgress =
        if (windCompletedAt == null) this else copy(windMemoryHidden = true)
}

