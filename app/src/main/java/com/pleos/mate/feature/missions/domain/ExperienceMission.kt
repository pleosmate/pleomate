package com.pleos.mate.feature.missions.domain

import kotlin.math.*

enum class ExperienceKind(val title: String, val xp: Int) {
    PLACE("오늘의 장소 기억하기", 5),
    DESTINATION("함께한 목적지 남기기", 10),
    NOTE("오늘의 한마디 남기기", 5),
    GREETING("동료와 첫 인사", 5)
}

data class CompanionMemory(val kind: ExperienceKind, val date: String, val text: String) {
    val claimKey: String get() = if (kind == ExperienceKind.GREETING) kind.name else "${kind.name}:$date"
}

internal object ExperienceRules {
    const val MAX_TEXT = 200
    const val LOCATION_MAX_AGE = 120000L
    fun validCoordinate(latitude: Double, longitude: Double) =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun fresh(timestamp: Long, now: Long) = timestamp > 0 && now - timestamp in 0..LOCATION_MAX_AGE
    fun near(latitude: Double, longitude: Double, targetLatitude: Double, targetLongitude: Double): Boolean {
        if (!validCoordinate(latitude, longitude) || !validCoordinate(targetLatitude, targetLongitude)) return false
        val lat = Math.toRadians(targetLatitude - latitude)
        val lon = Math.toRadians(targetLongitude - longitude)
        val a = sin(lat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(targetLatitude)) * sin(lon / 2).pow(2)
        return 6371000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0))) <= 150.0
    }
    fun weeklyDays(memories: List<CompanionMemory>, week: String): Int = memories
        .filter { it.kind != ExperienceKind.GREETING && MissionSchedule.week(it.date) == week }.map { it.date }.distinct().size
    fun bonusWeeks(memories: List<CompanionMemory>): Set<String> = memories.filter { it.kind != ExperienceKind.GREETING }
        .groupBy { MissionSchedule.week(it.date) }.filterValues { records -> records.map { it.date }.distinct().size >= 3 }.keys
}
