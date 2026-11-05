package com.pleos.mate.feature.missions.domain

internal data class VehicleMission(val id: String, val title: String, val detail: String)

internal val vehicleMissions = listOf(
    VehicleMission("odometer", "차량 상태 돌아보기", "현재 누적 주행거리를 확인해요. 더 운전할 필요는 없어요."),
    VehicleMission("tire", "타이어 기준 알아보기", "차량이 제공하는 타이어 저압 기준값을 확인해요. 현재 공기압 진단은 아니에요."),
    VehicleMission("battery", "충전 상태 확인하기", "전기차 배터리 잔량을 확인해요. 충전을 시작할 필요는 없어요."),
    VehicleMission("light", "차 안의 빛 알아보기", "실내 조명 상태를 확인해요. 조명을 바꿀 필요는 없어요."),
    VehicleMission("hvac", "차 안 온도 살펴보기", "차량이 제공하는 실제 실내 온도를 확인해요."),
    VehicleMission("wiper", "와이퍼 상태 알아보기", "현재 와이퍼 상태를 확인해요. 작동할 필요는 없어요."),
    VehicleMission("seat", "내 자리 살펴보기", "차량이 제공하는 좌석 착석 상태를 확인해요. 자리를 옮길 필요는 없어요."),
    VehicleMission("brake", "브레이크 상태 살펴보기", "현재 브레이크 페달 상태를 확인해요. 페달을 조작할 필요는 없어요."),
    VehicleMission("turn_signal", "방향 지시등 알아보기", "현재 방향 지시등 상태를 확인해요. 켤 필요는 없어요."),
    VehicleMission("mirror", "사이드미러 상태 살펴보기", "차량이 제공하는 미러 열선 상태를 확인해요. 열선을 켤 필요는 없어요.")
)

/** IDs from the previous catalog remain readable so existing growth records survive updates. */
internal val persistedVehicleMissionIds = vehicleMissions.map { it.id }.toSet() +
    setOf("safety", "driving_mode", "fuel", "car_info")

internal object VehicleMissionRules {
    fun canStart(active: Boolean, loaded: Boolean, parked: Boolean, busy: Boolean, knownMission: Boolean) =
        active && loaded && parked && !busy && knownMission

    fun readingIsFresh(readAt: Long, now: Long, maxAgeMillis: Long = 60_000L) =
        now - readAt <= maxAgeMillis

    fun validNonNegative(value: Float?) = value != null && value.isFinite() && value >= 0f
    fun validPercent(value: Int?) = value != null && value in 0..100
    fun validTemperature(value: Float?) = value != null && value.isFinite() && value in -50f..80f
}
