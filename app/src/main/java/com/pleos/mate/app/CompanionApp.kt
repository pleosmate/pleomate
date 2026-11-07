package com.pleos.mate.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.mate.BuildConfig
import com.pleos.mate.app.navigation.AppAction
import com.pleos.mate.app.navigation.AppTab
import com.pleos.mate.app.navigation.CompanionBottomBar
import com.pleos.mate.core.designsystem.CompanionTheme
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.feature.collection.presentation.CollectionScreen
import com.pleos.mate.feature.exploration.presentation.ExplorationScreen
import com.pleos.mate.feature.developer.presentation.DeveloperToolsScreen
import com.pleos.mate.feature.home.presentation.CompanionHome
import com.pleos.mate.feature.journey.presentation.JourneyViewModel
import com.pleos.mate.feature.missions.presentation.MissionCatalog
import com.pleos.mate.feature.onboarding.presentation.CharacterSelectionScreen
import com.pleos.mate.feature.settings.presentation.CompanionSettings

@Composable
internal fun CompanionApp(model: CompanionAppViewModel, journeys: JourneyViewModel, reconnect: () -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val session = model.session
    LaunchedEffect(session.parkedAndAllowed) {
        journeys.setParkedAllowed(session.parkedAndAllowed)
    }
    val onAction = model::onAction
    val homeScroll = rememberScrollState()
    val missionScroll = rememberScrollState()
    val explorationScroll = rememberScrollState()
    val collectionScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    val toolsScroll = rememberScrollState()
    val background = if (isSystemInDarkTheme()) listOf(Color(0xFF30443B), Color(0xFF1E2D28))
        else listOf(Color(0xFFEDF3E9), Color(0xFFF8F6ED))
    BackHandler(enabled = (state.choosing && state.characterId != null) || (!state.choosing && state.tab != AppTab.HOME)) {
        onAction(AppAction.Back)
    }
    CompanionTheme {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(background))) {
            val showBottomBar = state.characterId != null && !state.choosing
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                val viewportHeight = maxHeight
                val compact = maxWidth < 600.dp
                val horizontalPadding = if (compact) 16.dp else 32.dp
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = if (state.tab == AppTab.HOME && !state.choosing) 1600.dp else 1200.dp).fillMaxWidth()
                            .verticalScroll(if (state.choosing) homeScroll else when (state.tab) {
                                AppTab.HOME -> homeScroll
                                AppTab.EXPLORE -> explorationScroll
                                AppTab.MISSIONS -> missionScroll
                                AppTab.COLLECTION -> collectionScroll
                                AppTab.SETTINGS -> settingsScroll
                                AppTab.DEVELOPER -> toolsScroll
                            })
                            .padding(start = horizontalPadding, end = horizontalPadding, top = 24.dp, bottom = if (showBottomBar) 180.dp else 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (state.choosing) CharacterSelectionScreen(
                            PetCharacter.fromId(state.pendingCharacterId) ?: PetCharacter.BEAR,
                            state.pendingName, state.characterId != null,
                            session.parkedAndAllowed, session.parkingMessage, session.progress.level, reconnect,
                            { onAction(AppAction.SelectCharacter(it)) }, { onAction(AppAction.EditName(it)) },
                            { onAction(AppAction.ConfirmCharacter) }, { onAction(AppAction.Back) }
                        ) else when (state.tab) {
                            AppTab.HOME -> CompanionHome(state.character, session, journeys, reconnect,
                                { onAction(AppAction.SelectTab(AppTab.MISSIONS)) },
                                { onAction(AppAction.SelectTab(AppTab.COLLECTION)) },
                                { onAction(AppAction.SelectTab(AppTab.EXPLORE)) },
                                { onAction(AppAction.OpenCharacterSelection) }, state.petName, viewportHeight)
                            AppTab.EXPLORE -> ExplorationScreen(session)
                            AppTab.MISSIONS -> MissionCatalog(session, { onAction(AppAction.Back) }, session::startMission)
                            AppTab.COLLECTION -> CollectionScreen(session, journeys)
                            AppTab.SETTINGS -> CompanionSettings(session, journeys) { onAction(AppAction.Back) }
                            AppTab.DEVELOPER -> if (BuildConfig.DEBUG) DeveloperToolsScreen(session)
                        }
                    }
                }
                if (showBottomBar) {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp).background(
                        Brush.verticalGradient(listOf(Color.Transparent, background.last().copy(alpha = 0.65f), background.last().copy(alpha = 0.96f)))
                    ))
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        CompanionBottomBar(state.tab, state.character, session.progress.level,
                            { onAction(AppAction.SelectTab(it)) }, { onAction(AppAction.OpenCharacterSelection) })
                    }
                }
            }
        }
    }
}
