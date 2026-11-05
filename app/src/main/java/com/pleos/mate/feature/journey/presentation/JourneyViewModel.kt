package com.pleos.mate.feature.journey.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.pleos.mate.feature.journey.data.local.JourneyDraftStore
import com.pleos.mate.feature.journey.data.local.JourneyRepository
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.JourneyEditor
import com.pleos.mate.feature.journey.domain.JourneyEditorMode
import com.pleos.mate.feature.journey.domain.JourneyObservationFactory
import com.pleos.mate.feature.journey.domain.JourneyTracker
import com.pleos.mate.feature.journey.domain.OdometerSample
import java.util.UUID
import javax.inject.Inject

/** Explicit, process-bound observation. UI may pause while readonly callbacks remain alive. */
@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val repository: JourneyRepository,
    private val draftStore: JourneyDraftStore,
    observationFactory: JourneyObservationFactory
) : ViewModel() {
    var uiState by mutableStateOf(JourneyUiState(editor = draftStore.read()))
        private set
    var editor: JourneyEditor?
        get() = uiState.editor
        private set(value) { uiState = uiState.copy(editor = value) }

    private var tracker: JourneyTracker? = null
    private var parked = false
    private var finishAfterSample = false
    private var pendingWrites = 0
    private var lastQueued: Journey? = null
    private val observer = observationFactory.create(::receive, ::problem)

    var archive: JourneyArchive
        get() = uiState.archive
        private set(value) { uiState = uiState.copy(archive = value) }
    var loaded: Boolean
        get() = uiState.loaded
        private set(value) { uiState = uiState.copy(loaded = value) }
    var recording: Boolean
        get() = uiState.recording
        private set(value) { uiState = uiState.copy(recording = value) }
    var busy: Boolean
        get() = uiState.busy
        private set(value) { uiState = uiState.copy(busy = value) }
    var message: String
        get() = uiState.message
        private set(value) { uiState = uiState.copy(message = value) }
    var memoryReaction: Int
        get() = uiState.memoryReaction
        private set(value) { uiState = uiState.copy(memoryReaction = value) }

    init {
        repository.read { result ->
            result.onSuccess { saved ->
                archive = saved
                loaded = true
                editor?.let { draft ->
                    if ((draft.mode == JourneyEditorMode.PENDING && saved.pending?.id != draft.id) ||
                        (draft.mode == JourneyEditorMode.EDIT && saved.memories.none { it.id == draft.id }) ||
                        (draft.mode == JourneyEditorMode.MANUAL && draft.id in saved.handledIds)) cancelEditor()
                }
                if (saved.pending != null) message = "마지막으로 확인한 이동 구간이 있어요. 주차 후 살펴보세요."
            }.onFailure {
                message = "이동 기록을 읽지 못했어요. 저장 공간을 확인한 뒤 앱을 다시 열어주세요."
            }
        }
    }

    fun setParkedAllowed(allowed: Boolean) {
        val returning = !parked && allowed
        parked = allowed
        if (returning && recording) finishObservation()
    }

    fun startObservation() {
        if (!loaded || !parked || recording || busy || archive.pending != null || editor != null) return
        tracker = JourneyTracker(UUID.randomUUID().toString())
        lastQueued = null
        finishAfterSample = false
        recording = true
        message = "차량에서 이동의 시작 값을 확인하고 있어요."
        observer.start()
    }

    fun finishObservation() {
        if (!recording || !parked) return
        finishAfterSample = true
        message = "주차 후 마지막 이동 값을 확인하고 있어요."
        observer.requestSample()
    }

    private fun receive(sample: OdometerSample) {
        if (!recording) return
        val current = tracker ?: return
        if (!current.accept(sample)) {
            problem("이동 값이 초기화되거나 확인에 공백이 생겼어요. 앞서 확인한 구간만 남겼어요.")
            return
        }
        val segment = current.segment
        if (segment != null && segment != lastQueued) {
            lastQueued = segment
            persist({ it.observe(segment) })
        }
        message = if (segment == null) "시작 값을 확인했어요. 이동 중 확인되는 구간을 남길게요." else "확인한 이동을 남기고 있어요. 주차 후 함께 살펴봐요."
        if (finishAfterSample) {
            observer.stop()
            recording = false
            finishAfterSample = false
            message = if (segment == null) "아직 확인된 이동이 없어요. 다음 이동에 다시 함께해요." else "앱이 확인한 이동 구간이에요. 기억으로 남겨볼까요?"
        }
    }

    private fun problem(reason: String) {
        observer.stop()
        recording = false
        finishAfterSample = false
        message = reason
    }

    fun beginEditor(id: String? = null) {
        if (!canEdit()) return
        if (editor != null) return
        val memory = archive.memories.find { it.id == id }
        val pending = archive.pending?.takeIf { it.id == id }
        if (id != null && memory == null && pending == null) return
        persistEditor(JourneyEditor(id ?: UUID.randomUUID().toString(),
            if (id == null) JourneyEditorMode.MANUAL else if (memory != null) JourneyEditorMode.EDIT else JourneyEditorMode.PENDING,
            (memory ?: pending)?.theme?.ifBlank { "일상" } ?: "일상", (memory ?: pending)?.memo.orEmpty()))
    }

    fun updateEditor(theme: String, memo: String) {
        if (!parked || recording || busy) return
        editor?.let { persistEditor(it.updateText(theme, memo)) }
    }

    private fun persistEditor(value: JourneyEditor) {
        editor = value
        draftStore.save(value)
    }

    fun cancelEditor() {
        if (busy) return
        editor = null
        draftStore.clear()
    }

    fun saveEditor() {
        if (!canEdit()) return
        val draft = editor ?: return
        when (draft.mode) {
            JourneyEditorMode.PENDING -> saveMemory(draft.id, draft.theme, draft.memo)
            JourneyEditorMode.EDIT -> editMemory(draft)
            JourneyEditorMode.MANUAL -> {
                val now = System.currentTimeMillis()
                persist({ it.saveManual(draft, now) }) {
                    cancelEditor()
                    message = "직접 남긴 기억을 저장했어요. 이동 경험치는 지급하지 않아요."
                    memoryReaction++
                }
            }
        }
    }

    fun saveMemory(id: String, theme: String = "", memo: String = "") {
        if (!canEdit() || archive.pending?.id != id) return
        persist({ it.copy(pending = it.pending?.copy(theme = theme.take(30), memo = memo.trim().take(200))).savePending(id) }) {
            cancelEditor()
            message = "함께한 이동을 기억으로 남겼어요."
            memoryReaction++
        }
    }

    private fun editMemory(editor: JourneyEditor) {
        if (!canEdit()) return
        persist({ it.editMemory(editor) }) { cancelEditor(); message = "기억을 수정했어요." }
    }

    fun skipMemory(id: String) {
        if (!canEdit() || archive.pending?.id != id) return
        persist({ it.skipPending(id) }) { if (editor?.id == id) cancelEditor(); message = "이번 구간은 건너뛰었어요. 다음 이동에 다시 함께해요." }
    }

    fun deleteMemory(id: String) {
        if (!canEdit() || archive.memories.none { it.id == id }) return
        persist({ it.deleteMemory(id) }) { if (editor?.id == id) cancelEditor(); message = "이동 기억을 삭제했어요." }
    }

    private fun canEdit() = loaded && parked && !recording && !busy

    private fun persist(change: (JourneyArchive) -> JourneyArchive, done: () -> Unit = {}) {
        pendingWrites++
        busy = true
        repository.update(change) { result ->
            result.onSuccess { next ->
                archive = next
                pendingWrites--
                busy = pendingWrites > 0
                done()
            }.onFailure {
                pendingWrites--
                busy = pendingWrites > 0
                problem("이동 기록을 저장하지 못했어요. 마지막으로 저장된 구간만 확인해 주세요.")
            }
        }
    }

    override fun onCleared() {
        observer.stop()
        super.onCleared()
    }
}
