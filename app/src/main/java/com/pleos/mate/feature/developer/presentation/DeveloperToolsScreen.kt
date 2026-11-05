package com.pleos.mate.feature.developer.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.feature.companion.presentation.session.CompanionSession

@Composable
internal fun DeveloperToolsScreen(session: CompanionSession) {
    val context = LocalContext.current
    val tools = remember(context) {
        listOf(
            "MissionSafetyActivity" to R.string.open_mission_safety,
            "OdometerValidationActivity" to R.string.open_odometer_probe,
            "GlbValidationActivity" to R.string.open_glb_probe,
            "PetMotionPreviewActivity" to R.string.open_pet_motion,
            "WindowValidationActivity" to R.string.open_window_probe,
            "DoorValidationActivity" to R.string.open_door_probe
        ).mapNotNull { (name, label) ->
            val intent = Intent().setClassName(context, "com.pleos.mate.feature.developer.presentation.$name")
            if (intent.resolveActivity(context.packageManager) != null) intent to label else null
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.developer_tools), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.developer_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
        tools.forEach { (intent, label) ->
            OutlinedButton(onClick = { context.startActivity(intent) }, enabled = session.parkedAndAllowed,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(label)) }
        }
    }
}

