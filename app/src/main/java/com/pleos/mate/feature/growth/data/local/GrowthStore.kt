package com.pleos.mate.feature.growth.data.local

import com.pleos.mate.feature.growth.data.GrowthDataSource
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.CompanionMemory
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.persistedVehicleMissionIds
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** A single durable record makes the mission claim and its XP inseparable. */
class GrowthStore(directory: File) : GrowthDataSource {
    private val record = File(directory, "companion-growth-v1")

    override fun read(): GrowthProgress = if (record.exists()) {
        val lines = record.readText().trim().lines()
        if (lines.firstOrNull() in listOf("2", "3", "4", "5", "6", "7", "8")) {
            require(lines.size >= 5)
            val time = lines[1].toLong().takeIf { it > 0 }
            val visibility = lines[2]
            require(visibility == "visible" || visibility == "hidden")
            var index = 3
            val dailyCount = lines[index++].toInt().also { require(it in 0..10000) }
            val dates = lines.subList(index, index + dailyCount).toSet().also { values ->
                require(values.size == dailyCount)
                values.forEach { java.time.LocalDate.parse(it) }
            }
            index += dailyCount
            val weeklyCount = lines[index++].toInt().also { require(it in 0..10000) }
            val weeks = lines.subList(index, index + weeklyCount).toSet()
            index += weeklyCount
            val missions = if (lines.first() in listOf("3", "4", "5", "6", "7", "8")) {
                val count = lines[index++].toInt().also { require(it in 0..persistedVehicleMissionIds.size) }
                lines.subList(index, index + count).toSet().also { values ->
                    require(values.size == count && values.all { id -> id in persistedVehicleMissionIds })
                    index += count
                }
            } else emptySet()
            val memories = if (lines.first() in listOf("5", "6", "7", "8")) {
                val count = lines[index++].toInt().also { require(it in 0..10000) }
                lines.subList(index, index + count).map { line ->
                    val parts = line.split('|')
                    require(parts.size == 3)
                    val kind = ExperienceKind.valueOf(parts[0])
                    java.time.LocalDate.parse(parts[1])
                    val text = String(java.util.Base64.getDecoder().decode(parts[2]), Charsets.UTF_8)
                    require(text.isNotBlank() && text.length <= ExperienceRules.MAX_TEXT)
                    CompanionMemory(kind, parts[1], text)
                }.also { values ->
                    require(values.map { it.claimKey }.distinct().size == count)
                    index += count
                }
            } else emptyList()
            val windDates = if (lines.first() in listOf("6", "7", "8")) {
                val count = lines[index++].toInt().also { require(it in 0..10000) }
                lines.subList(index, index + count).toSet().also { dates ->
                    require(dates.size == count)
                    dates.forEach { java.time.LocalDate.parse(it) }
                    index += count
                }
            } else emptySet()
            val regions = if (lines.first() in listOf("7", "8")) {
                val count = lines[index++].toInt().also { require(it in 0..100) }
                lines.subList(index, index + count).toSet().also { values ->
                    require(values.size == count && values.all { it.isNotBlank() && it.length <= 50 })
                    index += count
                }
            } else emptySet()
            val souvenirs = if (lines.first() == "8") {
                val count = lines[index++].toInt().also { require(it in 0..20000) }
                lines.subList(index, index + count).toSet().also { values ->
                    require(values.size == count && values.all { it.matches(Regex("[0-9]{10}\\|[1-3]\\|[0-9]{4}-[0-9]{2}-[0-9]{2}")) })
                    values.forEach { java.time.LocalDate.parse(it.substringAfterLast('|')) }
                    index += count
                }
            } else emptySet()
            require(lines.size == index)
            require(weeks.size == weeklyCount && weeks.all { it.matches(Regex("\\d{4}-W\\d{2}")) })
            GrowthProgress(time, windMemoryHidden = visibility == "hidden", dailyDoorCheckDates = dates, weeklyDoorBonusWeeks = weeks, completedVehicleMissions = if (lines.first() in listOf("4", "5", "6", "7", "8")) missions else emptySet(), companionMemories = memories, dailyWindDates = windDates, exploredRegions = regions, explorationSouvenirs = souvenirs)
        } else {
            require(lines.size in 1..2)
            val time = lines.first().toLong().also { require(it > 0) }
            val visibility = lines.getOrNull(1) ?: "visible" // Existing timestamp-only records remain valid.
            require(visibility == "visible" || visibility == "hidden")
            GrowthProgress(time, windMemoryHidden = visibility == "hidden")
        }
    } else GrowthProgress()

    override fun save(progress: GrowthProgress) {
        val pending = File(record.parentFile, "companion-growth-v1.pending")
        pending.outputStream().use { stream ->
            val visibility = if (progress.windMemoryHidden) "hidden" else "visible"
            val lines = buildList {
                add("8")
                add((progress.windCompletedAt ?: 0L).toString())
                add(visibility)
                add(progress.dailyDoorCheckDates.size.toString())
                addAll(progress.dailyDoorCheckDates.sorted())
                add(progress.weeklyDoorBonusWeeks.size.toString())
                addAll(progress.weeklyDoorBonusWeeks.sorted())
                add(progress.completedVehicleMissions.size.toString())
                addAll(progress.completedVehicleMissions.sorted())
                add(progress.companionMemories.size.toString())
                addAll(progress.companionMemories.map { memory ->
                    "${memory.kind.name}|${memory.date}|${java.util.Base64.getEncoder().encodeToString(memory.text.toByteArray(Charsets.UTF_8))}"
                })
                add(progress.dailyWindDates.size.toString())
                addAll(progress.dailyWindDates.sorted())
                add(progress.exploredRegions.size.toString())
                addAll(progress.exploredRegions.sorted())
                add(progress.explorationSouvenirs.size.toString())
                addAll(progress.explorationSouvenirs.sorted())
            }
            stream.write(lines.joinToString("\n").toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        Files.move(pending.toPath(), record.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
