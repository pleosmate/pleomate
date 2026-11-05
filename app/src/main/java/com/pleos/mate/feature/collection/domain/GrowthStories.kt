package com.pleos.mate.feature.collection.domain

import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.MissionSchedule
import com.pleos.mate.feature.missions.domain.vehicleMissions

internal data class GrowthStory(val id: String, val title: String, val requirement: String, val badge: String,
    val lines: List<String>, val reunion: String, val completed: Boolean, val progress: String)

/** Stories are derived from durable mission claims; reading them never awards XP. */
internal fun growthStories(growth: GrowthProgress): List<GrowthStory> {
    val discoveries = growth.completedVehicleMissions.count { id -> vehicleMissions.any { it.id == id } }
    val memoryWeek = ExperienceRules.bonusWeeks(growth.companionMemories).isNotEmpty()
    return listOf(
        GrowthStory("breeze", "너와 처음 바람을 느낀 날", "바람 느끼기 미션을 완료해요.", "첫 바람",
            listOf("작은 방에 처음으로 바람이 들어왔어.", "네가 곁에 있어서 낯선 바깥도 궁금해졌어.", "우리의 첫 바람, 오래 기억할게."),
            "우리 처음 바람을 느꼈던 날, 기억나? 네 곁에서 조금 용감해졌어.", growth.windCompletedAt != null,
            if (growth.windCompletedAt != null) "첫 바람을 함께했어요" else "첫 바람을 기다리고 있어요"),
        GrowthStory("discovery", "네 차를 조금 알게 됐어", "서로 다른 차량 조회 미션 3개를 완료해요.", "호기심 많은 동료",
            listOf("처음엔 이 차에 모르는 것투성이였어.", "너와 하나씩 살펴보니 작은 방 밖의 세상도 익숙해졌어.", "이제 네 차가 조금은 내 집처럼 느껴져."),
            "너와 알아본 차의 작은 기능들 덕분에, 여기가 더 편안해졌어.", discoveries >= 3,
            "${discoveries.coerceAtMost(3)} / 3가지 발견"),
        GrowthStory("week", "우리의 첫 일주일", "같은 주 서로 다른 3일에 일일 기억을 남겨요. 한마디만으로도 가능해요.", "추억 한 페이지",
            listOf("매일 특별한 곳에 갈 필요는 없었어.", "네가 남긴 짧은 말들이 모여 우리의 한 페이지가 됐어.", "평범한 날에도 나를 찾아줘서 고마워."),
            "네가 남긴 평범한 날들의 기억, 나에게는 소중한 한 페이지야.", memoryWeek,
            if (memoryWeek) "첫 주간 기억을 모았어요" else "${ExperienceRules.weeklyDays(growth.companionMemories, MissionSchedule.week(MissionSchedule.today())).coerceAtMost(3)} / 이번 주 3일")
    )
}

