package com.pleos.mate.feature.journey.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.pleos.mate.core.designsystem.companionTextFieldColors
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.OdometerSource
import com.pleos.mate.feature.journey.domain.JourneyEditorMode
import java.text.DateFormat
import java.util.Date

@Composable
internal fun JourneyCard(model: JourneyViewModel, allowed: Boolean) {
    val draft = model.archive.pending
    val editor = model.editor
    var editorOpen by rememberSaveable { mutableStateOf(true) }
    var viewing by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(allowed, model.archive) {
        if (!allowed) { viewing = false; deletingId = null }
        if (model.archive.memories.none { it.id == deletingId }) deletingId = null
    }
    Column(Modifier.fillMaxWidth().testTag("journey_card").companionCardBackground().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.journey_section), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(model.message, modifier = Modifier.testTag("journey_status"))
            when {
                model.recording -> {
                    Text(stringResource(R.string.journey_observing_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = model::finishObservation, enabled = allowed,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(stringResource(R.string.journey_finish)) }
                }
                draft != null -> {
                    if (allowed) JourneyDetails(draft)
                    else Text(stringResource(R.string.journey_park_review))
                    Text(stringResource(R.string.journey_interval_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { editorOpen = true; model.beginEditor(draft.id) }, enabled = allowed && !model.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("save_journey_memory")) { Text(stringResource(R.string.journey_save)) }
                    TextButton(onClick = { model.skipMemory(draft.id) }, enabled = allowed && !model.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.journey_skip)) }
                }
                else -> {
                    Text(stringResource(R.string.journey_start_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = model::startObservation, enabled = model.loaded && allowed && !model.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("start_journey")) { Text(stringResource(R.string.journey_start)) }
                }
            }
            model.archive.memories.lastOrNull()?.let { latest ->
                Text(stringResource(R.string.journey_latest), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                if (allowed) JourneyDetails(latest)
                OutlinedButton(onClick = { viewing = true }, enabled = allowed && !model.busy && !model.recording,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("view_journey_memories")) { Text(stringResource(R.string.journey_view)) }
            }
    }
    if (!model.recording) OutlinedButton(onClick = { editorOpen = true; model.beginEditor() },
        enabled = allowed && model.loaded && !model.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("manual_journey")) {
        Text(if (editor != null) "작성 중인 기억 이어 쓰기" else "직접 기억 남기기")
    }
    if (editor != null && editorOpen && allowed && model.loaded) CompanionDialog(
        modifier = Modifier.companionDialogBorder(),
        onDismissRequest = { if (!model.busy) editorOpen = false },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (editor.mode == JourneyEditorMode.EDIT) "기억 수정" else "기억 남기기") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (editor.mode == JourneyEditorMode.MANUAL) "직접 남기는 기억이에요. 거리·시간과 이동 경험치는 기록하지 않아요." else "오늘의 이동은 어떤 기억인가요?")
                listOf("일상", "나들이", "함께한 시간").forEach { option ->
                    OutlinedButton(onClick = { model.updateEditor(option, editor.memo) }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (editor.theme == option) "✓ $option" else option)
                    }
                }
                OutlinedTextField(shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                                colors = companionTextFieldColors(), value = editor.memo, onValueChange = { model.updateEditor(editor.theme, it) }, enabled = !model.busy,
                    placeholder = { Text("메모 (선택)") }, supportingText = { Text("${editor.memo.length}/200") })
            }
        },
        confirmButton = { Button(onClick = model::saveEditor, enabled = !model.busy && !model.recording,
            modifier = Modifier.testTag("confirm_journey_editor")) { Text(if (model.busy) "저장 중" else "저장") } },
        dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { editorOpen = false }, enabled = !model.busy) { Text("나중에") }
            TextButton(onClick = model::cancelEditor, enabled = !model.busy) { Text("작성 취소") }
        } }
    )
    if (viewing && allowed && editor == null) CompanionDialog(
        modifier = Modifier.companionDialogBorder(),
        onDismissRequest = { viewing = false }, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.journey_memories)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                model.archive.memories.asReversed().forEach { memory ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        JourneyDetails(memory)
                        TextButton(onClick = { editorOpen = true; model.beginEditor(memory.id) }, enabled = !model.busy && !model.recording) { Text("기억 수정") }
                        TextButton(onClick = { deletingId = memory.id }, enabled = !model.busy && !model.recording,
                            modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.memory_delete)) }
                    }
                }
                if (model.archive.memories.isEmpty()) Text(stringResource(R.string.journey_no_memories))
            }
        },
        confirmButton = { TextButton(onClick = { viewing = false }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.home_close)) } }
    )
    if (deletingId != null && allowed) {
        val id = requireNotNull(deletingId)
        CompanionDialog(
            modifier = Modifier.companionDialogBorder(),
            onDismissRequest = { if (!model.busy) deletingId = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.memory_delete_title)) },
            text = { Text(stringResource(R.string.journey_delete_notice)) },
            confirmButton = {
                Button(onClick = { model.deleteMemory(id) }, enabled = !model.busy && !model.recording,
                    modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.memory_delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }, enabled = !model.busy,
                modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.character_cancel)) } }
        )
    }
}

@Composable
private fun JourneyDetails(value: Journey) {
    if (value.theme.isNotBlank()) Text(value.theme, style = MaterialTheme.typography.titleMedium)
    if (value.memo.isNotBlank()) Text(value.memo)
    if (value.source == OdometerSource.TOTAL) Text("누적 주행거리의 증가분으로 확인한 구간", style = MaterialTheme.typography.bodyMedium)
    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value.startedAt)))
    Text(stringResource(R.string.journey_end_time, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value.endedAt))), style = MaterialTheme.typography.bodyMedium)
    if (value.source == OdometerSource.MANUAL) {
        Text("직접 남긴 기억 · 거리·시간 측정 없음", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val distance = if (value.meters < 1) "<1 m" else if (value.meters < 1000) String.format(locale, "%.0f m", value.meters)
        else String.format(locale, "%.1f km", value.meters / 1000)
    Text(if (value.minutes == null) "$distance · 주행 시간 확인 불가" else stringResource(R.string.journey_metrics, distance, value.minutes), style = MaterialTheme.typography.titleMedium)
}
