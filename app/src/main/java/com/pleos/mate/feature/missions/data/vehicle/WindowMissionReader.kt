package com.pleos.mate.feature.missions.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.missions.domain.WindMissionRules

/** Owns the manual Window subscription; rewards and parking decisions belong to the session. */
internal class WindowMissionReader(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val area = WindowArea(WindowValue.ROW_1_LEFT)
    private var vehicle: Vehicle? = null
    private var listener: WindowPositionListener? = null
    private var generation = 0

    fun start(onReady: () -> Unit, onPosition: (Int?) -> Unit, onFailure: (String) -> Unit) {
        cancel()
        val token = generation
        val emulator = Build.HARDWARE in listOf("ranchu", "goldfish") ||
            Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator")
        if (!emulator) {
            onFailure("이 미션의 창문 기준은 현재 Pleos 에뮬레이터에서만 검증했어요.")
            return
        }
        try {
            val sdk = Vehicle(context = context)
            vehicle = sdk
            sdk.initialize()
            sdk.getWindow().checkWindowPositionCapabilities(
                onComplete = { config, areas -> main.post {
                    if (token != generation) return@post
                    val bounds = areas[area]
                    if (config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE) ||
                        !WindMissionRules.supported(bounds?.minValue, bounds?.maxValue)) {
                        onFailure("이 차량의 창문 조회 범위는 아직 미션에서 지원하지 않아요.")
                    } else {
                        onReady()
                        if (token == generation) watch(sdk, token, onPosition, onFailure)
                    }
                } },
                onFailed = { main.post {
                    if (token == generation) onFailure("창문 연결을 확인하지 못했어요. 다시 시도해 주세요.")
                } }
            )
        } catch (_: Exception) { onFailure("창문에 연결할 수 없어요. 권한과 차량 연결을 확인해 주세요.") }
    }

    private fun watch(sdk: Vehicle, token: Int, onPosition: (Int?) -> Unit, onFailure: (String) -> Unit) {
        val updates = object : WindowPositionListener {
            override fun onWindowPositionUpdated(position: Int?) { main.post {
                if (token == generation) onPosition(position)
            } }
            override fun onFailed(e: Exception) { main.post {
                if (token == generation) onFailure("창문 상태 수신이 끊겼어요. 다시 시작해 주세요.")
            } }
        }
        listener = updates
        try {
            sdk.getWindow().registerWindowPosition(area, updates)
            readCurrent(onPosition) { onFailure("현재 창문 상태를 읽지 못했어요. 다시 시도해 주세요.") }
        } catch (_: Exception) { onFailure("창문 상태를 읽을 수 없어요. 다시 시도해 주세요.") }
    }

    fun readCurrent(onPosition: (Int?) -> Unit, onFailure: () -> Unit) {
        val token = generation
        val sdk = vehicle ?: return onFailure()
        try {
            sdk.getWindow().getCurrentWindowPosition(area,
                onComplete = { value -> main.post { if (token == generation) onPosition(value) } },
                onFailed = { main.post { if (token == generation) onFailure() } })
        } catch (_: Exception) { onFailure() }
    }

    fun cancel() {
        generation++
        try { listener?.let { vehicle?.getWindow()?.unregisterWindowPosition(area, it) } } catch (_: Exception) { }
        listener = null
        try { vehicle?.release() } catch (_: Exception) { }
        vehicle = null
    }
}
