package com.pleos.mate.feature.companion.presentation.session

import com.pleos.mate.feature.missions.domain.ExperienceKind

internal data class ExperienceUiState(
    val selected: ExperienceKind? = null,
    val text: String = "",
    val status: String = "",
    val busy: Boolean = false
)
