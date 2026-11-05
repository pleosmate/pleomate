package com.pleos.mate.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal fun Modifier.companionCardBackground(): Modifier {
    val shape = RoundedCornerShape(26.dp)
    return clip(shape).background(Color.White.copy(alpha = 0.06f))
}

@Composable
internal fun Modifier.companionDialogBorder(): Modifier =
    border(1.dp, Color.White.copy(alpha = 0.28f), MaterialTheme.shapes.extraLarge)

@Composable
internal fun companionTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.07f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
    disabledContainerColor = Color.White.copy(alpha = 0.02f),
    focusedBorderColor = Color.White.copy(alpha = 0.55f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
    disabledBorderColor = Color.White.copy(alpha = 0.10f),
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.onSurface
)

@Composable
internal fun FlatCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.companionCardBackground()) { content() }
}

