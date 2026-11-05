package com.pleos.mate.feature.companion.presentation.session

import com.pleos.mate.feature.growth.domain.GrowthProgress

/** Immutable snapshot; session commands own all writes on the main thread. */
data class CompanionUiState(
    val seenVehicleGuides: Set<String> = emptySet(),
    val decorationRevision: Int = 0,
    val petReaction: String = "",
    val petReactionId: Int = 0,
    val reactionVoiceEnabled: Boolean = false,
    val progress: GrowthProgress = GrowthProgress(),
    val loaded: Boolean = false,
    val parkedAndAllowed: Boolean = false,
    val vehicleMoving: Boolean = false,
    val parkingMessage: String = "주차 상태를 확인하고 있어요.",
    val mission: CompanionSession.Mission = CompanionSession.Mission.IDLE,
    val message: String = "앞좌석 왼쪽 창문으로 바람을 느껴볼까요?",
    val rawPosition: Int? = null,
    val celebration: Int = 0,
    val memorySaving: Boolean = false,
    val memoryError: String? = null,
    val dailyDoorCheck: CompanionSession.DailyDoorCheck = CompanionSession.DailyDoorCheck.READY,
    val dailyDoorMessage: String = "주차 후 운전석 문이 닫혔는지 확인해요.",
    val experienceSaving: Boolean = false,
    val levelUpMessage: String? = null,
    val vehicleMissionId: String? = null,
    val vehicleMissionReading: Boolean = false,
    val vehicleMissionSaving: Boolean = false,
    val vehicleMissionValue: String? = null,
    val vehicleMissionMessage: String = "",
    val outfit: String = "none",
    val room: String = "none"
)
