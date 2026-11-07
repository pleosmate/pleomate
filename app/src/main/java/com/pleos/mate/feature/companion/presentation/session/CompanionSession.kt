package com.pleos.mate.feature.companion.presentation.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.vehicle.ParkedAccess
import com.pleos.mate.data.local.CompanionPreferences
import com.pleos.mate.data.PetProfileDataSource
import com.pleos.mate.feature.collection.domain.growthStories
import com.pleos.mate.feature.growth.data.local.GrowthRepository
import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.missions.data.vehicle.AutomaticDoorObserver
import com.pleos.mate.feature.missions.data.vehicle.AutomaticWindObserver
import com.pleos.mate.feature.missions.data.vehicle.DoorMissionVerifier
import com.pleos.mate.feature.missions.data.vehicle.VehicleMissionReader
import com.pleos.mate.feature.missions.data.vehicle.WindowMissionReader
import com.pleos.mate.feature.missions.domain.ExperienceKind
import com.pleos.mate.feature.missions.domain.ExperienceRules
import com.pleos.mate.feature.missions.domain.DoorMissionRules
import com.pleos.mate.feature.missions.domain.MissionSchedule
import com.pleos.mate.feature.missions.domain.VehicleMissionRules
import com.pleos.mate.feature.missions.domain.WindMissionRules
import com.pleos.mate.feature.missions.domain.vehicleMissions
import java.util.concurrent.Executors

class CompanionSession(
    context: Context,
    private val growthRepository: GrowthRepository,
    private val preferences: CompanionPreferences,
    private val profile: PetProfileDataSource = preferences
) {
    enum class Mission { IDLE, CHECKING, WAITING, VERIFYING, SAVING, COMPLETE, ERROR }
    enum class DailyDoorCheck { READY, CHECKING, NEEDS_ACTION, COMPLETE, ERROR }
    private val context = context.applicationContext
    var uiState by mutableStateOf(CompanionUiState(
        seenVehicleGuides = preferences.seenGuides,
        reactionVoiceEnabled = preferences.reactionVoiceEnabled,
        outfit = profile.outfit,
        room = profile.room
    ))
        private set
    val character: PetCharacter get() = PetCharacter.fromId(profile.characterId) ?: PetCharacter.BEAR
    val outfit: String get() = uiState.outfit
    val room: String get() = uiState.room
    var seenVehicleGuides: Set<String>
        get() = uiState.seenVehicleGuides
        private set(value) { uiState = uiState.copy(seenVehicleGuides = value) }
    fun hasSeenGuide(id: String) = id in seenVehicleGuides
    private fun markGuideSeen(id: String) {
        seenVehicleGuides = seenVehicleGuides + id
        preferences.saveSeenGuides(seenVehicleGuides)
    }

    private val doorVerifier = DoorMissionVerifier(context)
    private val main = Handler(Looper.getMainLooper())
    private val vehicleWorker = Executors.newSingleThreadExecutor()
    private var windowReader: WindowMissionReader? = null
    private var doorGeneration = 0
    private var active = false
    private var generation = 0
    private var timeout: Runnable? = null
    private var automaticWindObserver: AutomaticWindObserver? = null
    private var automaticWindPending = false
    private var automaticWindSaving = false
    private var automaticDoorObserver: AutomaticDoorObserver? = null
    private val parking = ParkedAccess(context, vehicleWorker, { allowed, text ->
        val returnedToAllowedPark = allowed && !parkedAndAllowed
        parkedAndAllowed = allowed
        parkingMessage = text
        if (!allowed) { cancelVehicleMission(); experiences.cancel() }
        if (!allowed) {
            automaticDoorObserver?.stop()
            automaticDoorObserver = null
        }
        if (!allowed && mission in setOf(Mission.CHECKING, Mission.WAITING, Mission.VERIFYING)) {
            cancelMission("차량 사용 상태가 바뀌었어요. 주차 후 다시 시작해 주세요.")
        }
        if (!allowed && dailyDoorCheck == DailyDoorCheck.CHECKING) {
            cancelDailyDoorCheck("차량 사용 상태가 바뀌었어요. 주차 후 다시 확인해 주세요.")
        }
        if (returnedToAllowedPark) maybeAutoCheckDailyDoor()
        if (allowed) maybeCompleteAutomaticWind()
    }, { moving -> vehicleMoving = moving })

    var decorationRevision: Int
        get() = uiState.decorationRevision
        private set(value) { uiState = uiState.copy(decorationRevision = value) }
    fun selectDecoration(key: String, id: String) {
        if (!parkedAndAllowed || key !in listOf("outfit", "room")) return
        profile.saveDecoration(key, id)
        uiState = if (key == "outfit") uiState.copy(outfit = id) else uiState.copy(room = id)
        decorationRevision++
    }

    var petReaction: String
        get() = uiState.petReaction
        private set(value) { uiState = uiState.copy(petReaction = value) }
    var petReactionId: Int
        get() = uiState.petReactionId
        private set(value) { uiState = uiState.copy(petReactionId = value) }
    var reactionVoiceEnabled: Boolean
        get() = uiState.reactionVoiceEnabled
        private set(value) { uiState = uiState.copy(reactionVoiceEnabled = value) }

    fun updateReactionVoiceEnabled(enabled: Boolean) {
        reactionVoiceEnabled = enabled
        preferences.saveReactionVoice(enabled)
    }

    private fun react(text: String) { petReaction = text; petReactionId++ }
    fun previewPetReaction() {
        if (parkedAndAllowed) react("오늘도 함께라서 좋아. 잠깐 쉬었다 갈까?")
    }

    var progress: GrowthProgress
        get() = uiState.progress
        private set(value) { uiState = uiState.copy(progress = value) }
    var loaded: Boolean
        get() = uiState.loaded
        private set(value) { uiState = uiState.copy(loaded = value) }
    var parkedAndAllowed: Boolean
        get() = uiState.parkedAndAllowed
        private set(value) { uiState = uiState.copy(parkedAndAllowed = value) }
    var vehicleMoving: Boolean
        get() = uiState.vehicleMoving
        private set(value) { uiState = uiState.copy(vehicleMoving = value) }
    var parkingMessage: String
        get() = uiState.parkingMessage
        private set(value) { uiState = uiState.copy(parkingMessage = value) }
    var mission: CompanionSession.Mission
        get() = uiState.mission
        private set(value) { uiState = uiState.copy(mission = value) }
    var message: String
        get() = uiState.message
        private set(value) { uiState = uiState.copy(message = value) }
    var rawPosition: Int?
        get() = uiState.rawPosition
        private set(value) { uiState = uiState.copy(rawPosition = value) }
    var celebration: Int
        get() = uiState.celebration
        private set(value) { uiState = uiState.copy(celebration = value) }
    private var playedCelebration by mutableStateOf(0)
    val hasPendingCelebration: Boolean get() = celebration > playedCelebration
    fun markCelebrationPlayed(counter: Int) { playedCelebration = maxOf(playedCelebration, counter.coerceAtMost(celebration)) }

    var memorySaving: Boolean
        get() = uiState.memorySaving
        private set(value) { uiState = uiState.copy(memorySaving = value) }
    var memoryError: String?
        get() = uiState.memoryError
        private set(value) { uiState = uiState.copy(memoryError = value) }
    var dailyDoorCheck: CompanionSession.DailyDoorCheck
        get() = uiState.dailyDoorCheck
        private set(value) { uiState = uiState.copy(dailyDoorCheck = value) }
    var dailyDoorMessage: String
        get() = uiState.dailyDoorMessage
        private set(value) { uiState = uiState.copy(dailyDoorMessage = value) }

    init {
        growthRepository.read { result ->
            result.onSuccess { saved ->
                val previous = context.getSharedPreferences("exploration", 0).getStringSet("regions", emptySet()).orEmpty()
                progress = saved
                loaded = true
                if (previous.isNotEmpty()) migrateExploredRegions(previous)
                if (active) startAutomaticWindObservation()
                maybeAutoCheckDailyDoor()
                maybeCompleteAutomaticWind()
            }.onFailure {
                message = "성장 기록을 읽을 수 없어요. 저장 공간을 확인한 뒤 앱을 다시 열어주세요."
                mission = Mission.ERROR
            }
        }
    }

    private var explorationSaving by mutableStateOf(false)

    private fun migrateExploredRegions(regions: Set<String>) {
        if (explorationSaving) return
        explorationSaving = true
        val next = progress.copy(exploredRegions = progress.exploredRegions + regions)
        growthRepository.save(next) { result ->
            result.onSuccess {
                applyProgress(next)
                context.getSharedPreferences("exploration", 0).edit().remove("regions").apply()
            }
            explorationSaving = false
        }
    }

    internal fun saveExploration(region: String, stillValid: () -> Boolean, done: (String) -> Unit) {
        if (!active || !loaded || !parkedAndAllowed || growthBusy()) {
            done("주차 상태와 저장 작업을 확인한 뒤 다시 시도해 주세요.")
            return
        }
        if (region in progress.exploredRegions) {
            done("이미 발견한 지역이에요. 첫 발견 보상은 한 번만 받아요.")
            return
        }
        explorationSaving = true
        parking.verify { allowed ->
            if (!allowed || !active || !stillValid()) {
                explorationSaving = false
                done("주차 상태가 바뀌었어요. 주차 후 다시 확인해 주세요.")
                return@verify
            }
            val next = progress.copy(exploredRegions = progress.exploredRegions + region)
            growthRepository.save(next) { result ->
                result.onSuccess {
                    applyProgress(next)
                    done("${region} 발견! 기념 조각과 +5 XP를 받았어요.")
                }.onFailure { done("저장하지 못했어요. 다시 시도해 주세요.") }
                explorationSaving = false
            }
        }
    }

    internal fun collectExploration(areaId: String, province: String, stillValid: () -> Boolean, done: (String) -> Unit) {
        if (!active || !loaded || !parkedAndAllowed || growthBusy()) {
            done("주차 상태와 저장 작업을 확인한 뒤 다시 시도해 주세요.")
            return
        }
        explorationSaving = true
        parking.verify { allowed ->
            val today = java.time.LocalDate.now().toString()
            val slot = progress.nextExplorationSouvenir(areaId, today)
            if (!allowed || !active || !stillValid() || slot == null) {
                explorationSaving = false
                done(if (slot == null) "오늘은 이미 조각을 모았거나 모든 조각을 모았어요." else "위치나 주차 상태를 다시 확인해 주세요.")
                return@verify
            }
            val newProvince = province !in progress.exploredRegions
            val next = progress.copy(exploredRegions = progress.exploredRegions + province,
                explorationSouvenirs = progress.explorationSouvenirs + "$areaId|$slot|$today")
            growthRepository.save(next) { result ->
                result.onSuccess {
                    applyProgress(next)
                    done("기념 조각 $slot 발견! +${if (newProvince) 10 else 5} XP를 받았어요.")
                }.onFailure { done("저장하지 못했어요. 다시 시도해 주세요.") }
                explorationSaving = false
            }
        }
    }

    internal val experiences = ExperienceSession(context, this)
    var experienceSaving: Boolean
        get() = uiState.experienceSaving
        private set(value) { uiState = uiState.copy(experienceSaving = value) }

    internal fun saveExperience(kind: ExperienceKind, text: String, stillValid: () -> Boolean, done: (String) -> Unit) {
        if (!active || !loaded || !parkedAndAllowed || growthBusy() || !stillValid()) return
        experienceSaving = true
        parking.verify { allowed ->
            if (!allowed || !active || !stillValid()) {
                experienceSaving = false
                done("사용 상태가 바뀌었어요. 주차 후 다시 확인해 주세요.")
                return@verify
            }
            val next = progress.completeExperience(kind, MissionSchedule.today(), text)
            if (next == progress) {
                experienceSaving = false
                done("이미 보상을 받은 미션이에요.")
                return@verify
            }
            val weeklyBonus = ExperienceRules.bonusWeeks(next.companionMemories).size > ExperienceRules.bonusWeeks(progress.companionMemories).size
            growthRepository.save(next) { result ->
                result.onSuccess {
                    applyProgress(next)
                    experienceSaving = false
                    if (active) celebration++
                    done("기억 저장 완료 · +${kind.xp} XP" + if (weeklyBonus) " · 이번 주 추억 3일 +15 XP!" else "")
                }.onFailure {
                    experienceSaving = false
                    done("저장하지 못했어요. 경험치는 아직 지급되지 않았어요. 다시 시도해 주세요.")
                }
            }
        }
    }

    var levelUpMessage: String?
        get() = uiState.levelUpMessage
        private set(value) { uiState = uiState.copy(levelUpMessage = value) }

    fun dismissLevelUp() { levelUpMessage = null }

    private fun applyProgress(next: GrowthProgress) {
        val previousLevel = progress.level
        val previousStories = growthStories(progress).filter { it.completed }.map { it.id }.toSet()
        progress = next
        growthStories(next).firstOrNull { it.completed && it.id !in previousStories }?.let {
            react(it.lines.last() + " 도감에 우리 이야기를 남겼어.")
        }
        if (next.hasDailyWind()) {
            automaticWindPending = false
            automaticWindObserver?.stop()
            automaticWindObserver = null
        } else if (automaticWindPending) main.post(::maybeCompleteAutomaticWind)
        if (next.level > previousLevel) levelUpMessage = PetMotionRules.levelUpMessage(previousLevel, next.level)
    }

    fun resume() {
        active = true
        parking.start()
        if (loaded) startAutomaticWindObservation()
    }

    fun pause() {
        active = false
        cancelVehicleMission()
        experiences.cancel()
        automaticWindObserver?.stop()
        automaticWindObserver = null
        automaticDoorObserver?.stop()
        automaticDoorObserver = null
        parkedAndAllowed = false
        parking.stop()
        if (mission != Mission.SAVING) {
            val running = mission in setOf(Mission.CHECKING, Mission.WAITING, Mission.VERIFYING)
            cancelMission(if (running) "미션이 잠시 멈췄어요. 주차 후 다시 시작할 수 있어요." else null)
        }
        if (dailyDoorCheck == DailyDoorCheck.CHECKING) cancelDailyDoorCheck("확인이 멈췄어요. 주차 후 다시 시작할 수 있어요.")
    }

    fun retryParking() {
        parking.stop()
        parkedAndAllowed = false
        if (active) parking.start()
    }

    private var missionReader: VehicleMissionReader? = null
    private var vehicleMissionGeneration = 0
    private var vehicleReadAt = 0L
    private var vehicleAwardVerifying = false
    var vehicleMissionId: String?
        get() = uiState.vehicleMissionId
        private set(value) { uiState = uiState.copy(vehicleMissionId = value) }
    var vehicleMissionReading: Boolean
        get() = uiState.vehicleMissionReading
        private set(value) { uiState = uiState.copy(vehicleMissionReading = value) }
    var vehicleMissionSaving: Boolean
        get() = uiState.vehicleMissionSaving
        private set(value) { uiState = uiState.copy(vehicleMissionSaving = value) }
    var vehicleMissionValue: String?
        get() = uiState.vehicleMissionValue
        private set(value) { uiState = uiState.copy(vehicleMissionValue = value) }
    var vehicleMissionMessage: String
        get() = uiState.vehicleMissionMessage
        private set(value) { uiState = uiState.copy(vehicleMissionMessage = value) }

    internal fun growthBusy() = explorationSaving || experienceSaving || vehicleMissionSaving || memorySaving || dailyDoorCheck == DailyDoorCheck.CHECKING || automaticWindSaving ||
        mission in setOf(Mission.CHECKING, Mission.WAITING, Mission.VERIFYING, Mission.SAVING)

    private fun maybeAutoCheckDailyDoor() {
        if (!active || !loaded || !parkedAndAllowed || progress.hasDailyDoorCheck()) return
        if (dailyDoorCheck == DailyDoorCheck.NEEDS_ACTION) {
            startAutomaticDoorObservation()
            return
        }
        if (growthBusy() || vehicleMissionId != null || experiences.selected != null) return
        startDailyDoorCheck()
    }

    private fun startAutomaticDoorObservation() {
        if (!active || !parkedAndAllowed || progress.hasDailyDoorCheck() || automaticDoorObserver != null) return
        automaticDoorObserver = AutomaticDoorObserver(context) {
            if (active && parkedAndAllowed && dailyDoorCheck == DailyDoorCheck.NEEDS_ACTION) startDailyDoorCheck()
        }.also { it.start() }
    }

    private fun startAutomaticWindObservation() {
        if (!active || !loaded || progress.hasDailyWind() || automaticWindObserver != null) return
        automaticWindObserver = AutomaticWindObserver(context) {
            main.post {
                if (!active || progress.hasDailyWind()) return@post
                automaticWindPending = true
                maybeCompleteAutomaticWind()
            }
        }.also { it.start() }
    }

    private fun maybeCompleteAutomaticWind() {
        if (!automaticWindPending || automaticWindSaving || !active || !loaded || !parkedAndAllowed ||
            progress.hasDailyWind() || growthBusy()) return
        automaticWindSaving = true
        parking.verify { allowed ->
            if (!active || !automaticWindPending || progress.hasDailyWind()) {
                automaticWindSaving = false
                return@verify
            }
            if (!allowed) {
                automaticWindSaving = false
                return@verify
            }
            val next = progress.completeDailyWind(System.currentTimeMillis())
            if (next == progress) {
                automaticWindPending = false
                automaticWindSaving = false
                return@verify
            }
            growthRepository.save(next) { result ->
                result.onSuccess {
                    automaticWindPending = false
                    applyProgress(next)
                    automaticWindSaving = false
                    mission = Mission.COMPLETE
                    message = "오늘의 창문 경험을 확인했어요 · 첫 달성 +10 XP / 이후 하루 +2 XP"
                    if (active) celebration++
                }.onFailure {
                    automaticWindSaving = false
                    message = "창문 경험을 저장하지 못했어요. 주차 상태에서 다시 확인할게요."
                }
            }
        }
    }

    fun startVehicleMission(id: String) {
        if (!VehicleMissionRules.canStart(active, loaded, parkedAndAllowed, growthBusy(), vehicleMissions.any { it.id == id })) return
        experiences.cancel()
        cancelVehicleMission()
        val token = vehicleMissionGeneration
        vehicleMissionId = id
        vehicleMissionReading = true
        vehicleMissionMessage = "차량 지원과 현재 값을 확인하고 있어요."
        parking.verify { allowed ->
            if (token != vehicleMissionGeneration || !active) return@verify
            if (!allowed) { cancelVehicleMission(); return@verify }
            val reader = VehicleMissionReader(context)
            missionReader = reader
            reader.read(id) { result ->
                if (token != vehicleMissionGeneration || !active || !parkedAndAllowed) return@read
                vehicleMissionReading = false
                when (result) {
                    is VehicleMissionReader.Result.Available -> {
                        markGuideSeen(id)
                        vehicleMissionValue = result.text
                        vehicleReadAt = android.os.SystemClock.elapsedRealtime()
                        vehicleMissionMessage = if (id in progress.completedVehicleMissions)
                            "현재 차량 정보예요. 이미 보상을 받은 미션이라 XP는 추가되지 않아요."
                        else "차량에서 읽은 현재 정보예요. 확인하면 최초 1회 +5 XP를 받아요."
                    }
                    is VehicleMissionReader.Result.Unavailable -> vehicleMissionMessage = result.text
                }
            }
        }
    }

    fun cancelVehicleMission() {
        if (vehicleAwardVerifying) {
            vehicleAwardVerifying = false
            vehicleMissionSaving = false
        }
        vehicleMissionGeneration++
        missionReader?.cancel()
        missionReader = null
        vehicleMissionReading = false
        vehicleMissionId = null
        vehicleMissionValue = null
    }

    fun completeVehicleMission() {
        val id = vehicleMissionId ?: return
        if (!active || !loaded || !parkedAndAllowed || growthBusy() || vehicleMissionReading || vehicleMissionValue == null) return
        if (!VehicleMissionRules.readingIsFresh(vehicleReadAt, android.os.SystemClock.elapsedRealtime())) {
            vehicleMissionValue = null
            vehicleMissionMessage = "조회한 지 시간이 지났어요. 다시 조회한 뒤 확인해 주세요."
            return
        }
        val token = vehicleMissionGeneration
        vehicleMissionSaving = true
        vehicleAwardVerifying = true
        parking.verify { allowed ->
            if (token != vehicleMissionGeneration) return@verify
            vehicleAwardVerifying = false
            if (!active || !allowed) {
                vehicleMissionSaving = false
                return@verify
            }
            val next = progress.completeVehicleMission(id)
            if (next == progress) {
                vehicleMissionSaving = false
                vehicleMissionMessage = "이미 보상을 받은 미션이에요. 현재 정보는 다시 확인할 수 있어요."
                return@verify
            }
            growthRepository.save(next) { result ->
                result.onSuccess {
                    applyProgress(next)
                    vehicleMissionSaving = false
                    if (token == vehicleMissionGeneration) vehicleMissionMessage = "미션 완료 · +5 XP"
                    if (active) celebration++
                }.onFailure {
                    vehicleMissionSaving = false
                    if (token == vehicleMissionGeneration) vehicleMissionMessage = "저장하지 못했어요. 보상은 아직 반영되지 않았어요. 다시 시도해 주세요."
                }
            }
        }
    }

    fun startMission() {
        if (!WindMissionRules.canStart(active, loaded, progress.hasDailyWind(), growthBusy())) return
        experiences.cancel()
        cancelVehicleMission()
        cancelMission()
        val token = generation
        mission = Mission.CHECKING
        message = "주차 상태와 창문 연결을 확인하고 있어요."
        armTimeout(token)
        parking.verify { allowed ->
            if (!current(token)) return@verify
            if (!allowed) { fail(token, "주차(P)와 차량 사용 가능 상태를 확인한 뒤 다시 시도해 주세요."); return@verify }
            val reader = WindowMissionReader(context)
            windowReader = reader
            reader.start(
                onReady = {
                    if (current(token)) {
                        mission = Mission.WAITING
                        message = "기존 차량 조작부로 앞좌석 왼쪽 창문을 조금 열어주세요. 이미 열려 있으면 그대로 확인해요."
                    }
                },
                onPosition = { receive(token, it) },
                onFailure = { fail(token, it) }
            )
        }
    }

    fun startDailyDoorCheck() {
        val otherMissionRunning = mission in setOf(Mission.CHECKING, Mission.WAITING, Mission.VERIFYING, Mission.SAVING)
        val busy = experienceSaving || vehicleMissionSaving || memorySaving || automaticWindSaving || otherMissionRunning
        if (!DoorMissionRules.canStart(active, loaded, parkedAndAllowed, dailyDoorCheck == DailyDoorCheck.CHECKING, busy)) return
        experiences.cancel()
        cancelVehicleMission()
        val dateKey = MissionSchedule.today()
        if (progress.hasDailyDoorCheck(dateKey)) {
            dailyDoorCheck = DailyDoorCheck.COMPLETE
            dailyDoorMessage = "오늘 확인은 완료했어요. 내일 다시 함께 살펴봐요."
            return
        }
        val token = ++doorGeneration
        dailyDoorCheck = DailyDoorCheck.CHECKING
        dailyDoorMessage = "운전석 문의 닫힘 상태를 확인하고 있어요."
        parking.verify { allowed ->
            if (!doorCurrent(token)) return@verify
            if (!allowed) {
                dailyDoorCheck = DailyDoorCheck.ERROR
                dailyDoorMessage = "주차(P)와 차량 사용 가능 상태를 확인한 뒤 다시 시도해 주세요."
                return@verify
            }
            doorVerifier.verify(isStillAllowed = { active && parkedAndAllowed }) { result -> main.post {
                if (!doorCurrent(token)) return@post
                when (result) {
                    DoorMissionVerifier.Result.NeedsAction -> {
                        dailyDoorCheck = DailyDoorCheck.NEEDS_ACTION
                        dailyDoorMessage = "차량 조작부에서 운전석 문을 닫으면 자동으로 다시 확인해요. 앱은 차량을 직접 조작하지 않아요."
                        startAutomaticDoorObservation()
                        maybeCompleteAutomaticWind()
                    }
                    is DoorMissionVerifier.Result.Unavailable -> {
                        dailyDoorCheck = DailyDoorCheck.ERROR
                        dailyDoorMessage = result.message
                        maybeCompleteAutomaticWind()
                    }
                    DoorMissionVerifier.Result.Closed -> finishDailyDoorReward(token)
                }
            } }
        }
    }

    private fun finishDailyDoorReward(token: Int) {
        if (!doorCurrent(token)) return
        parking.verify { allowed ->
            if (!doorCurrent(token)) return@verify
            if (!allowed) {
                dailyDoorCheck = DailyDoorCheck.ERROR
                dailyDoorMessage = "주차 상태가 바뀌어 확인을 멈췄어요."
                return@verify
            }
            val dateKey = MissionSchedule.today()
            val weekKey = MissionSchedule.week(dateKey)
            val gainsWeeklyBonus = DoorMissionRules.earnsWeeklyBonus(
                hasBonus = progress.hasWeeklyDoorBonus(weekKey),
                completedDaysThisWeek = progress.doorChecksThisWeek(weekKey)
            )
            val next = progress.completeDailyDoorCheck(dateKey)
            if (next == progress) {
                dailyDoorCheck = DailyDoorCheck.COMPLETE
                dailyDoorMessage = "오늘 확인은 이미 완료했어요."
                automaticDoorObserver?.stop()
                automaticDoorObserver = null
                return@verify
            }
            dailyDoorMessage = "오늘의 확인을 저장하고 있어요."
            growthRepository.save(next) { result ->
                result.onSuccess {
                    applyProgress(next)
                    dailyDoorCheck = DailyDoorCheck.COMPLETE
                    react("운전석 문이 닫힌 것도 확인했어. 오늘도 함께해 줘서 고마워!")
                    automaticDoorObserver?.stop()
                    automaticDoorObserver = null
                    dailyDoorMessage = if (gainsWeeklyBonus) "운전석 문 닫힘 확인 · +5 XP, 주간 3일 달성 +15 XP!" else "운전석 문 닫힘 확인 · +5 XP"
                    if (active) celebration++
                    maybeCompleteAutomaticWind()
                }.onFailure {
                    dailyDoorCheck = DailyDoorCheck.ERROR
                    dailyDoorMessage = "저장하지 못했어요. 확인 보상은 아직 반영되지 않았어요."
                }
            }
        }
    }

    private fun cancelDailyDoorCheck(reason: String) {
        ++doorGeneration
        dailyDoorCheck = DailyDoorCheck.READY
        dailyDoorMessage = reason
    }

    private fun doorCurrent(token: Int) = active && token == doorGeneration

    private fun receive(token: Int, raw: Int?) {
        if (!current(token) || mission != Mission.WAITING) return
        rawPosition = raw
        if (raw == null || raw !in 0..10) {
            fail(token, "창문 상태가 확인되지 않았어요. 연결 후 다시 시도해 주세요.")
        } else if (WindMissionRules.canComplete(true, parkedAndAllowed, raw)) {
            mission = Mission.VERIFYING
            message = "함께한 순간을 확인하고 있어요."
            parking.verify { allowed ->
                if (!current(token)) return@verify
                if (!allowed) { fail(token, "주차 상태가 바뀌어 미션을 멈췄어요."); return@verify }
                windowReader?.readCurrent(
                    onPosition = { value ->
                        if (current(token)) {
                            if (WindMissionRules.canComplete(true, parkedAndAllowed, value)) award(token)
                            else {
                                mission = Mission.WAITING
                                receive(token, value)
                            }
                        }
                    },
                    onFailure = { fail(token, "완료 상태를 확인하지 못했어요. 다시 시도해 주세요.") }
                )
            }
        } else {
            // A valid closed reading establishes the connection; allow time for the user's action.
            clearTimeout()
            armTimeout(token, 120_000)
        }
    }

    private fun award(token: Int) {
        if (!current(token) || mission != Mission.VERIFYING) return
        generation++
        mission = Mission.SAVING
        message = "동료의 첫 경험을 저장하고 있어요."
        clearTimeout()
        stopWindow()
        val next = progress.completeDailyWind(System.currentTimeMillis())
        growthRepository.save(next) { result ->
            result.onSuccess {
                applyProgress(next)
                mission = Mission.COMPLETE
                message = "오늘의 창문 경험 완료 · 첫 달성 +10 XP / 이후 하루 +2 XP"
                if (active) celebration++
            }.onFailure {
                mission = Mission.ERROR
                message = "저장하지 못했어요. 공간을 확인하고 다시 시도해 주세요. 경험치는 아직 지급하지 않았어요."
            }
        }
    }

    fun hideWindMemory() {
        if (experienceSaving || vehicleMissionSaving || automaticWindSaving || mission in setOf(Mission.CHECKING, Mission.WAITING, Mission.VERIFYING, Mission.SAVING) || dailyDoorCheck == DailyDoorCheck.CHECKING || !active || !parkedAndAllowed || !loaded || memorySaving || progress.windMemoryAt == null) return
        cancelVehicleMission()
        memorySaving = true
        memoryError = null
        val next = progress.hideWindMemory()
        growthRepository.save(next) { result ->
            result.onSuccess {
                applyProgress(next)
                memorySaving = false
                if (mission == Mission.COMPLETE) mission = Mission.IDLE
            }.onFailure {
                memorySaving = false
                memoryError = "기억을 지우지 못했어요. 잠시 후 다시 시도해 주세요."
            }
        }
    }

    fun cancelMission(reason: String? = null) {
        if (mission == Mission.SAVING) return
        generation++
        clearTimeout()
        stopWindow()
        rawPosition = null
        mission = Mission.IDLE
        if (reason != null) message = reason
    }

    private fun fail(token: Int, reason: String) {
        if (!current(token) || mission == Mission.SAVING) return
        cancelMission()
        mission = Mission.ERROR
        message = reason
    }

    private fun current(token: Int) = active && token == generation

    private fun armTimeout(token: Int, delay: Long = 15_000) {
        clearTimeout()
        timeout = Runnable { fail(token, "연결 또는 상태 확인 시간이 지났어요. 다시 시작할 수 있어요.") }.also { main.postDelayed(it, delay) }
    }
    private fun clearTimeout() { timeout?.let(main::removeCallbacks); timeout = null }
    private fun stopWindow() {
        windowReader?.cancel()
        windowReader = null
    }

    fun close() { pause(); vehicleWorker.shutdown() }
}
