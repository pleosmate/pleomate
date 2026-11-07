package com.pleos.mate.feature.collection.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pleos.mate.R
import com.pleos.mate.core.designsystem.CompanionDialog
import com.pleos.mate.core.designsystem.companionCardBackground
import com.pleos.mate.core.designsystem.companionDialogBorder
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import java.text.DateFormat
import java.util.Date

@Composable
internal fun FirstMemoryCard(session: CompanionSession) {
    if (!session.loaded) return
    val time = session.progress.windMemoryAt
    val date = remember(time) { time?.let { DateFormat.getDateInstance(DateFormat.LONG).format(Date(it)) } }
    var viewing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(session.parkedAndAllowed, time) {
        if (!session.parkedAndAllowed || time == null) { viewing = false; deleting = false }
    }
    Column(Modifier.fillMaxWidth().testTag("first_memory_card").companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.memory_section), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            if (time != null) {
                Text(stringResource(R.string.wind_memory_title), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(requireNotNull(date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.wind_memory_summary))
                OutlinedButton(
                    onClick = { viewing = true }, enabled = session.parkedAndAllowed && !session.memorySaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("view_first_memory")
                ) { Text(stringResource(R.string.memory_view)) }
            } else {
                Text(stringResource(if (session.progress.windMemoryHidden) R.string.memory_deleted else R.string.memory_empty))
            }
            session.memoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (viewing && time != null && session.parkedAndAllowed && !deleting) {
        CompanionDialog(
            modifier = Modifier.companionDialogBorder(),
            onDismissRequest = { viewing = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.wind_memory_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).testTag("first_memory_detail")) {
                    Text(requireNotNull(date))
                    Text(stringResource(R.string.wind_memory_detail))
                    Text(stringResource(R.string.memory_reward_retained), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { viewing = false }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.memory_close)) } },
            dismissButton = { TextButton(onClick = { deleting = true }, modifier = Modifier.heightIn(min = 56.dp).testTag("delete_first_memory")) { Text(stringResource(R.string.memory_delete)) } }
        )
    }
    if (deleting && time != null && session.parkedAndAllowed) {
        CompanionDialog(
            modifier = Modifier.companionDialogBorder(),
            onDismissRequest = { if (!session.memorySaving) deleting = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.memory_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.memory_delete_message))
                    session.memoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = session::hideWindMemory, enabled = !session.memorySaving,
                    modifier = Modifier.heightIn(min = 56.dp).testTag("confirm_delete_memory")) {
                    Text(stringResource(if (session.memorySaving) R.string.memory_deleting else R.string.memory_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }, enabled = !session.memorySaving, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.character_cancel)) }
            }
        )
    }
}
