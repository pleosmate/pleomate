package com.pleos.mate.feature.companion.presentation

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.presentation.labelResource
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import java.util.Locale
import kotlinx.coroutines.delay

/** Short parked-only reaction; leaving Home or losing parked access cancels speech. */
@Composable
internal fun PetReactionBubble(session: CompanionSession, character: PetCharacter, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    var consumed by rememberSaveableReactionId()
    LaunchedEffect(session.petReactionId, session.parkedAndAllowed) {
        shown = false
        if (session.parkedAndAllowed && session.petReactionId > consumed) {
            consumed = session.petReactionId
            shown = true
            delay(5500)
            shown = false
        }
    }
    DisposableEffect(shown, session.parkedAndAllowed, session.reactionVoiceEnabled) {
        var speaker: TextToSpeech? = null
        var active = true
        if (shown && session.parkedAndAllowed && session.reactionVoiceEnabled) {
            try {
                val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
                speaker = tts
                tts.initialize(object : OnInitializeListener {
                    override fun onSuccess() {
                        if (active) try { tts.setLocale(Locale.KOREA); tts.speak(session.petReaction) } catch (_: Exception) { }
                    }
                    override fun onFailure(errorType: ErrorType) = Unit
                })
            } catch (_: Exception) { }
        }
        onDispose {
            active = false
            try { speaker?.stop() } catch (_: Exception) { }
            try { speaker?.release() } catch (_: Exception) { }
        }
    }
    if (shown && session.parkedAndAllowed) {
        PetDialogue(character, session.petReaction, modifier)
    }
}

/** Placed in its own slot below the pet, never on top of the artwork. */
@Composable
internal fun PetDialogue(character: PetCharacter, text: String, modifier: Modifier = Modifier) {
    val pixel = character.isPixel
    val background = when (character) {
        PetCharacter.ROVER -> Color(0xFF233840)
        PetCharacter.CLOUD -> Color(0xFF343B50)
        PetCharacter.SEED, PetCharacter.PIXEL -> Color(0xFF263B33)
        else -> Color(0xFFF5F1E8)
    }
    val foreground = if (pixel) Color(0xFFF2F3DF) else Color(0xFF3F433B)
    Column(modifier.widthIn(max = 420.dp).fillMaxWidth().testTag("pet_dialogue"), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!pixel) Canvas(Modifier.size(width = 16.dp, height = 6.dp)) {
            drawPath(Path().apply {
                moveTo(0f, size.height)
                quadraticTo(size.width * .35f, size.height * .7f, size.width * .5f, 0f)
                quadraticTo(size.width * .65f, size.height * .7f, size.width, size.height)
                close()
            }, background)
        }
        Surface(
            color = background,
            shape = if (pixel) PixelDialogueShape else RoundedCornerShape(18.dp),
            border = if (pixel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFAABF9C)) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(stringResource(character.labelResource), color = if (pixel) Color(0xFFBED1A8) else Color(0xFF77796D),
                    fontFamily = if (pixel) FontFamily.Monospace else FontFamily.Default,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(text, modifier = Modifier.padding(top = 3.dp), color = foreground,
                    fontFamily = if (pixel) FontFamily.Monospace else FontFamily.Default,
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Square pixel steps rather than a rounded Material card. */
private object PixelDialogueShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val step = with(density) { 4.dp.toPx() }.coerceAtMost(minOf(size.width, size.height) / 4)
        return Outline.Generic(Path().apply {
            moveTo(step * 2, 0f); lineTo(size.width - step * 2, 0f)
            lineTo(size.width - step * 2, step); lineTo(size.width - step, step)
            lineTo(size.width - step, step * 2); lineTo(size.width, step * 2)
            lineTo(size.width, size.height - step * 2); lineTo(size.width - step, size.height - step * 2)
            lineTo(size.width - step, size.height - step); lineTo(size.width - step * 2, size.height - step)
            lineTo(size.width - step * 2, size.height); lineTo(step * 2, size.height)
            lineTo(step * 2, size.height - step); lineTo(step, size.height - step)
            lineTo(step, size.height - step * 2); lineTo(0f, size.height - step * 2)
            lineTo(0f, step * 2); lineTo(step, step * 2); lineTo(step, step)
            lineTo(step * 2, step); close()
        })
    }
}

@Composable
private fun rememberSaveableReactionId() = androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
