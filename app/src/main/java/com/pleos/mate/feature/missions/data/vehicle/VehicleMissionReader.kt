package com.pleos.mate.feature.missions.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.missions.domain.VehicleMissionRules

/** One read-only request. All callbacks, timeout and cancellation are serialized on main. */
internal class VehicleMissionReader(context: Context) {
    sealed interface Result {
        data class Available(val text: String) : Result
        data class Unavailable(val text: String) : Result
    }
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var sdk: Vehicle? = null
    private var finished = true
    private var result: (Result) -> Unit = {}
    private val timeout = Runnable { finish(Result.Unavailable("차량 응답 시간이 초과됐어요. 연결 후 다시 확인해 주세요.")) }

    fun cancel() {
        finished = true
        main.removeCallbacks(timeout)
        try { sdk?.release() } catch (_: Exception) { }
        sdk = null
        result = {}
    }

    fun read(id: String, onResult: (Result) -> Unit) {
        cancel()
        finished = false
        result = onResult
        main.postDelayed(timeout, 12000)
        guarded {
            val vehicle = Vehicle(context).also { sdk = it; it.initialize() }
            when (id) {
                "odometer" -> global(vehicle.getOdometer()::checkCurrentDistanceDrivenCapabilities, vehicle.getOdometer()::getCurrentDistanceDriven) {
                    it.takeIf(VehicleMissionRules::validNonNegative)?.let { value -> "누적 주행거리: $value km" }
                }
                "tire" -> areas(vehicle.getTire()::checkLowTirePressureCapabilities, vehicle.getTire()::getLowTirePressure,
                    { areaLabel(it.values.map { area -> area.name }) }) {
                    it.takeIf { value -> value != null && VehicleMissionRules.validNonNegative(value) && value > 0f }?.let { value -> "저압 기준: $value kPa (현재 공기압이 아닙니다)" }
                }
                "battery" -> global(vehicle.getEvBattery()::checkEvBatteryStateOfChargeCapabilities, vehicle.getEvBattery()::getEvBatteryStateOfCharge) {
                    it.takeIf(VehicleMissionRules::validPercent)?.let { value -> "배터리 잔량: $value%" }
                }
                "light" -> global(
                    { ok, fail -> vehicle.getLight().checkCabinLightStateCapabilities({ config, _ -> ok(config) }, fail) },
                    vehicle.getLight()::getCurrentCabinLightState
                ) { it?.let { value -> "실내 조명: ${when (value.name) { "OFF" -> "꺼짐"; "ON" -> "켜짐"; else -> "주간 점등" }}" } }
                "hvac" -> areas(vehicle.getHVAC()::checkHvacTemperatureCurrentCapabilities, vehicle.getHVAC()::getHvacTemperatureCurrent,
                    { areaLabel(it.values.map { area -> area.name }) }) {
                    it.takeIf(VehicleMissionRules::validTemperature)?.let { value -> "현재 실내 온도: $value °C" }
                }
                "wiper" -> areas(
                    { ok, fail -> vehicle.getWiper().checkWiperStateCapabilities({ config, map -> ok(config, map.keys.toList()) }, fail) },
                    vehicle.getWiper()::getCurrentWiperState, { areaLabel(it.values.map { area -> area.name }) }
                ) { it?.let { value -> "와이퍼: ${when (value.name) { "OFF" -> "꺼짐"; "ON" -> "켜짐"; "SERVICE" -> "서비스 모드"; else -> "기타 상태" }}" } }
                "seat" -> areas(vehicle.getSeat()::checkSeatOccupancyCapabilities, vehicle.getSeat()::getCurrentSeatOccupancy,
                    { areaLabel(it.values.map { area -> area.name }) }) { it?.let { occupied -> "착석 감지: ${if (occupied) "있음" else "없음"}" } }
                "brake" -> global(vehicle.getBrake()::checkBrakePedalStatusCapabilities, vehicle.getBrake()::getCurrentBrakePedalStatus) {
                    it?.let { pressed -> "브레이크 페달: ${if (pressed) "밟힘" else "밟히지 않음"} · 제동 성능 진단이 아닙니다." }
                }
                "turn_signal" -> global(vehicle.getTurnSignal()::checkTurnSignalStateCapabilities, vehicle.getTurnSignal()::getCurrentTurnSignalState) {
                    it?.let { value -> "방향 지시등: ${when (value.name) { "OFF" -> "꺼짐"; "LEFT" -> "왼쪽"; else -> "오른쪽" }}" }
                }
                "mirror" -> areas(
                    { ok, fail -> vehicle.getSideMirror().checkHvacSideMirrorHeatCapabilities({ config, map -> ok(config, map.keys.toList()) }, fail) },
                    vehicle.getSideMirror()::getHvacSideMirrorHeatState, { areaLabel(it.values.map { area -> area.name }) }
                ) { it?.takeIf { value -> value >= 0 }?.let { value -> "미러 열선 단계: $value" } }
                else -> finish(Result.Unavailable("등록되지 않은 미션이에요."))
            }
        }
    }

    private fun areaLabel(names: List<String>): String = names.filter { it != "UNKNOWN" }.joinToString(" · ") {
        when (it) {
            "LEFT_FRONT", "ROW_1_LEFT" -> "앞쪽 왼쪽"
            "RIGHT_FRONT", "ROW_1_RIGHT" -> "앞쪽 오른쪽"
            "LEFT_REAR", "ROW_2_LEFT" -> "뒤쪽 왼쪽"
            "RIGHT_REAR", "ROW_2_RIGHT" -> "뒤쪽 오른쪽"
            "ROW_1_CENTER" -> "앞쪽 가운데"
            "ROW_2_CENTER" -> "뒤쪽 가운데"
            "ROW_3_LEFT" -> "3열 왼쪽"
            "ROW_3_CENTER" -> "3열 가운데"
            "ROW_3_RIGHT" -> "3열 오른쪽"
            "DRIVER_LEFT" -> "왼쪽 미러"
            "DRIVER_RIGHT" -> "오른쪽 미러"
            "DRIVER_CENTER" -> "가운데 미러"
            "FRONT_WINDSHIELD" -> "앞 유리"
            "REAR_WINDSHIELD" -> "뒤 유리"
            "LEFT" -> "왼쪽"
            "RIGHT" -> "오른쪽"
            else -> it
        }
    }.ifBlank { "공통 영역" }

    private fun guarded(block: () -> Unit) {
        if (finished) return
        try { block() } catch (e: Exception) { failed(e) }
    }

    private fun dispatch(block: () -> Unit) { main.post { guarded(block) } }

    private fun readable(config: CapabilityConfig): Boolean {
        if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE)) return true
        finish(Result.Unavailable("이 차량에서는 해당 정보를 읽을 수 없어요. 지원 여부와 권한을 확인해 주세요."))
        return false
    }

    private fun <T> global(
        check: ((CapabilityConfig) -> Unit, (Exception) -> Unit) -> Unit,
        get: ((T?) -> Unit, (Exception) -> Unit) -> Unit,
        format: (T?) -> String?
    ) {
        check({ config -> dispatch {
            if (readable(config)) get({ value -> dispatch { valueResult(format(value)) } }, ::failed)
        } }, ::failed)
    }

    private fun <A, T> areas(
        check: ((CapabilityConfig, List<A>) -> Unit, (Exception) -> Unit) -> Unit,
        get: (A, (T?) -> Unit, (Exception) -> Unit) -> Unit,
        label: (A) -> String,
        format: (T?) -> String?
    ) {
        check({ config, areas -> dispatch {
            if (readable(config)) {
                if (areas.isEmpty()) valueResult(null)
                else {
                    val values = mutableListOf<String>()
                    fun next(index: Int) {
                        if (index == areas.size) valueResult(values.joinToString("\n"))
                        else get(areas[index], { value -> dispatch {
                            val text = format(value)
                            if (text == null) valueResult(null)
                            else { values += "${label(areas[index])}: $text"; next(index + 1) }
                        } }, ::failed)
                    }
                    next(0)
                }
            }
        } }, ::failed)
    }

    private fun valueResult(text: String?) = finish(
        if (text.isNullOrBlank()) Result.Unavailable("유효한 차량 값이 아직 없어요. 차량 연결과 지원 상태를 확인하고 다시 시도해 주세요.")
        else Result.Available(text)
    )

    private fun failed(error: Exception) { main.post {
        if (!finished) finish(Result.Unavailable(
            if (error is SecurityException) "차량 정보 조회 권한이 없어요. 권한 승인 상태를 확인해 주세요."
            else if (error.message?.contains("ERROR_NOT_SUPPORTED_PROPERTY") == true)
                "이 차량에서는 해당 정보를 제공하지 않아요. 지원 차량에서 다시 확인할 수 있어요."
            else "차량 정보를 읽지 못했어요. 차량 연결과 권한 상태를 확인하고 다시 시도해 주세요."
        ))
    } }

    private fun finish(value: Result) {
        if (finished) return
        val callback = result
        cancel()
        callback(value)
    }
}
