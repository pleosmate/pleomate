package com.pleos.mate.core.pet.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules

@Composable
internal fun PetMotionControls(level: Int, enabled: Boolean, expanded: Boolean, onToggle: () -> Unit, onPlay: (PetMotion) -> Unit) {
    Box {
        TextButton(onClick = onToggle, enabled = enabled, modifier = Modifier.testTag("pet_motion_controls")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_motion_smile), contentDescription = null, modifier = Modifier.size(22.dp))
                Text("함께 놀기")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onToggle) {
            PetMotion.entries.forEach { motion ->
                val available = PetMotionRules.canPlay(level, motion)
                val icon = when (motion) {
                    PetMotion.SMILE -> R.drawable.ic_motion_smile
                    PetMotion.WAVE -> R.drawable.ic_motion_wave
                    PetMotion.CURIOUS -> R.drawable.ic_motion_curious
                    PetMotion.JUMP -> R.drawable.ic_motion_jump
                    PetMotion.CELEBRATE -> R.drawable.ic_motion_celebrate
                }
                DropdownMenuItem(
                    text = { Text(if (available) motion.label else "${motion.label} · Lv.${motion.unlockLevel}") },
                    leadingIcon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp)) },
                    onClick = { onPlay(motion); onToggle() }, enabled = enabled && available,
                    modifier = Modifier.testTag("pet_motion_${motion.name.lowercase()}"))
            }
        }
    }
}
