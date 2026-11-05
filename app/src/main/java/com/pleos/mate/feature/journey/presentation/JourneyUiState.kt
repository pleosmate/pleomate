package com.pleos.mate.feature.journey.presentation

import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyEditor

data class JourneyUiState(
    val editor: JourneyEditor? = null,
    val archive: JourneyArchive = JourneyArchive(),
    val loaded: Boolean = false,
    val recording: Boolean = false,
    val busy: Boolean = false,
    val message: String = "다음 이동을 동료와 함께 남겨볼까요?",
    val memoryReaction: Int = 0
)
