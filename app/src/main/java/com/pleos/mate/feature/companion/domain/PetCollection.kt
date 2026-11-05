package com.pleos.mate.feature.companion.domain

import com.pleos.mate.core.pet.model.PetAppearance
import com.pleos.mate.feature.collection.domain.growthStories
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.missions.domain.ExperienceKind

internal data class PetDecoration(val id: String, val title: String, val requirement: String)
internal object PetCollection {
    val outfits = listOf(PetDecoration("none", "기본 모습", "처음부터"), PetDecoration("breeze", "바람 스카프", "첫 바람 달성"), PetDecoration("bow", "추억 리본", "기억 1개 저장"))
    val rooms = listOf(PetDecoration("none", "기본 공간", "처음부터"), PetDecoration("cushion", "포근한 쿠션", "레벨 2"), PetDecoration("plant", "추억의 화분", "기억 1개 저장"))
    fun outfitUnlocked(id: String, progress: GrowthProgress, memories: Boolean) = when (id) {
        "none" -> true
        "breeze" -> progress.windCompletedAt != null
        "bow" -> memories
        else -> false
    }
    fun roomUnlocked(id: String, progress: GrowthProgress, memories: Boolean) = when (id) {
        "none" -> true
        "cushion" -> progress.level >= 2
        "plant" -> memories
        else -> false
    }
    fun stage(level: Int) = when (PetAppearance.stageLevel(level)) { 15 -> "빛나는 동료"; 5 -> "든든한 동료"; else -> "작은 동료" }
    fun reunion(progress: GrowthProgress, journeys: List<Journey>, variant: Int): String {
        val memories = progress.companionMemories
        val lines = buildList {
            addAll(growthStories(progress).filter { it.completed && (it.id != "breeze" || progress.canReuniteWithWind) }.map { it.reunion })
            journeys.lastOrNull { it.theme.isNotBlank() }?.let { add("우리 ‘${it.theme}’ 기억, 아직도 소중하게 간직하고 있어.") }
            memories.lastOrNull { it.kind == ExperienceKind.PLACE || it.kind == ExperienceKind.DESTINATION }?.let { add("함께 남긴 ‘${it.text}’, 기억나? 또 이야기하고 싶어.") }
            memories.lastOrNull { it.kind == ExperienceKind.NOTE }?.let { add("네가 남긴 ‘${it.text}’, 잘 간직하고 있어.") }
            if (progress.canReuniteWithWind) add(0, "함께 느꼈던 첫 바람, 기분 좋았지?")
        }
        return if (lines.isEmpty()) "다시 만나서 반가워. 오늘은 어떤 기억이 생길까?" else lines[Math.floorMod(variant, lines.size)]
    }
}
