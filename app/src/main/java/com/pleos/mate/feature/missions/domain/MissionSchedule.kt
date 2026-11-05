package com.pleos.mate.feature.missions.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Uses the vehicle's local calendar: one daily claim per date, weekly progress on ISO weeks. */
object MissionSchedule {
    fun today(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()

    fun week(dateKey: String): String {
        val date = LocalDate.parse(dateKey)
        val fields = WeekFields.ISO
        return "%04d-W%02d".format(Locale.ROOT, date.get(fields.weekBasedYear()), date.get(fields.weekOfWeekBasedYear()))
    }
}
