package com.pleos.mate.feature.companion.presentation.session

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pleos.mate.feature.missions.data.location.ExperienceLocationReader
import com.pleos.mate.feature.missions.data.voice.ExperienceVoice
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules

internal class ExperienceSession(private val context: Context, private val session: CompanionSession) {
    var uiState by mutableStateOf(ExperienceUiState())
        private set
    var selected: ExperienceKind?
        get() = uiState.selected
        private set(value) { uiState = uiState.copy(selected = value) }
    var text: String
        get() = uiState.text
        private set(value) { uiState = uiState.copy(text = value) }
    var status: String
        get() = uiState.status
        private set(value) { uiState = uiState.copy(status = value) }
    var busy: Boolean
        get() = uiState.busy
        private set(value) { uiState = uiState.copy(busy = value) }
    private var ready = false
    private var evidenceAt = 0L
    private var generation = 0
    private var location: ExperienceLocationReader? = null
    private var voice: ExperienceVoice? = null

    fun open(kind: ExperienceKind) {
        if (!session.loaded || !session.parkedAndAllowed || session.growthBusy()) return
        cancel()
        session.cancelVehicleMission()
        selected = kind
        status = when (kind) {
            ExperienceKind.PLACE -> "현재 위치를 확인한 뒤 장소 이름을 남겨요. 좌표는 저장하지 않아요."
            ExperienceKind.DESTINATION -> "내비게이션 목적지 150m 이내에서 주차 후 확인해요."
            ExperienceKind.NOTE -> "오늘의 한마디를 말하거나 직접 입력해 주세요."
            ExperienceKind.GREETING -> "동료의 인사를 듣고 마이크로 인사해 주세요."
        }
    }

    fun edit(value: String) {
        if (session.experienceSaving || busy || selected !in listOf(ExperienceKind.NOTE, ExperienceKind.PLACE)) return
        text = value.take(ExperienceRules.MAX_TEXT)
    }

    fun problem(message: String) { status = message }

    fun queryLocation() {
        val kind = selected ?: return
        if (kind !in listOf(ExperienceKind.PLACE, ExperienceKind.DESTINATION)) return
        if (!session.parkedAndAllowed || busy || session.experienceSaving) return
        val token = ++generation
        ready = false
        busy = true
        status = "새 위치와 연결 상태를 확인하고 있어요."
        location?.cancel()
        val reader = ExperienceLocationReader(context)
        location = reader
        reader.read(kind == ExperienceKind.DESTINATION) { result, error ->
            if (token != generation || !session.parkedAndAllowed) return@read
            busy = false
            if (error != null) status = error
            else {
                ready = true
                evidenceAt = SystemClock.elapsedRealtime()
                if (kind == ExperienceKind.DESTINATION) text = result.orEmpty()
                status = if (kind == ExperienceKind.PLACE) result.orEmpty() else "목적지 근처의 현재 위치를 확인했어요. 이름을 기억으로 남겨요."
            }
        }
    }

    fun startVoice() {
        val kind = selected ?: return
        if (kind !in listOf(ExperienceKind.NOTE, ExperienceKind.GREETING)) return
        if (!session.parkedAndAllowed || busy || session.experienceSaving) return
        val token = ++generation
        busy = true
        ready = false
        voice?.cancel()
        val speaker = ExperienceVoice(context)
        voice = speaker
        speaker.start(kind == ExperienceKind.GREETING,
            onStatus = { if (token == generation) status = it },
            onResult = { result, error ->
                if (token == generation && session.parkedAndAllowed) {
                    busy = false
                    if (error != null) status = error
                    else {
                        text = result.orEmpty()
                        ready = true
                        evidenceAt = SystemClock.elapsedRealtime()
                        status = "인식한 내용을 확인하고 기억으로 남겨 주세요."
                    }
                }
            })
    }

    val canSave: Boolean get() = selected != null && !busy && !session.growthBusy() && session.parkedAndAllowed &&
        text.isNotBlank() && (selected == ExperienceKind.NOTE || (ready && SystemClock.elapsedRealtime() - evidenceAt <= 60000))

    fun save() {
        val kind = selected ?: return
        if (!canSave) {
            status = "현재 위치 또는 음성 확인이 필요해요. 시간이 지났다면 다시 확인해 주세요."
            return
        }
        val token = generation
        session.saveExperience(kind, text, stillValid = {
            token == generation && (kind == ExperienceKind.NOTE || (ready && SystemClock.elapsedRealtime() - evidenceAt <= 60000))
        }) { result -> if (token == generation) status = result }
    }

    fun cancel() {
        generation++
        location?.cancel(); location = null
        voice?.cancel(); voice = null
        selected = null
        text = ""
        busy = false
        ready = false
    }
}
