package com.pleos.mate.core.pet.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.rendering.PetMotionView

@Composable
internal fun PetStage(character: PetCharacter, active: Boolean, modifier: Modifier = Modifier, greet: Boolean = false, celebration: Int = 0,
    level: Int = 1, showRoomBackground: Boolean = false, previewMotion: PetMotion = PetMotion.SMILE, previewRequest: Int = 0,
    running: Boolean = false, outfit: String = "none", room: String = "none", celebrateOnEntry: Boolean = false, walking: Boolean = false, onCelebrationPlayed: () -> Unit = {}) {
    val owner = LocalLifecycleOwner.current
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var lastGreeting by remember { mutableStateOf(greet) }
    var lastCelebration by remember { mutableIntStateOf(if (celebrateOnEntry) -1 else celebration) }
    var lastPreview by remember { mutableIntStateOf(previewRequest) }
    AndroidView(
        modifier = modifier,
        factory = { context -> PetMotionView(context) },
        update = { view ->
            view.showRoomBackground = showRoomBackground
            view.character = character
            view.level = level
            view.outfit = outfit
            view.room = room
            view.walking = walking
            view.runningDirection = if (running || walking) PetRunDirection.FRONT else null
            view.contentDescription = view.context.getString(character.labelResource)
            view.setHostActive(active && resumed)
            if (lastCelebration != celebration) view.post {
                if (lastCelebration != celebration && view.celebrate()) {
                    lastCelebration = celebration
                    onCelebrationPlayed()
                }
            }
            if (lastGreeting != greet) { view.greet(); lastGreeting = greet }
            if (lastPreview != previewRequest) view.post {
                if (lastPreview != previewRequest && view.playMotion(previewMotion)) lastPreview = previewRequest
            }
        },
        onRelease = { it.setHostActive(false) }
    )
}

