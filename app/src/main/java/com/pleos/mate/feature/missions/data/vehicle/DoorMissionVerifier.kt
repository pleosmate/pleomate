package com.pleos.mate.feature.missions.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

/** Read-only daily check for the driver's door position supported by the Pleos AVD. */
internal class DoorMissionVerifier(context: Context) {
    sealed interface Result {
        data object Closed : Result
        data object NeedsAction : Result
        data class Unavailable(val message: String) : Result
    }

    private val appContext = context.applicationContext
    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)

    fun verify(isStillAllowed: () -> Boolean, onResult: (Result) -> Unit) {
        if (!isStillAllowed()) { onResult(Result.Unavailable("주차 후 다시 확인해 주세요.")); return }
        val sdk = try { Vehicle(context = appContext).also { it.initialize() } }
        catch (_: Exception) { onResult(Result.Unavailable("운전석 문 상태에 연결할 수 없어요. 권한과 차량 연결을 확인해 주세요.")); return }
        val finished = AtomicBoolean(false)
        fun finish(result: Result) {
            if (finished.compareAndSet(false, true)) {
                try { sdk.release() } catch (_: Exception) { }
                onResult(result)
            }
        }
        try {
            sdk.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, bounds ->
                    if (!isStillAllowed()) finish(Result.Unavailable("차량 사용 상태가 바뀌었어요. 주차 후 다시 확인해 주세요."))
                    else {
                        val closedValue = bounds[driverDoor]?.minValue
                        if (config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE) || closedValue == null) {
                            finish(Result.Unavailable("이 차량에서는 운전석 문 닫힘 상태를 확인할 수 없어요."))
                        } else try {
                            sdk.getDoor().getCurrentDoorPosition(driverDoor,
                                onComplete = { position ->
                                    if (!isStillAllowed()) finish(Result.Unavailable("차량 사용 상태가 바뀌었어요. 주차 후 다시 확인해 주세요."))
                                    else if (position == null) finish(Result.Unavailable("운전석 문 닫힘 상태가 확인되지 않았어요. 다시 시도해 주세요."))
                                    else finish(if (position == closedValue) Result.Closed else Result.NeedsAction)
                                },
                                onFailed = { finish(Result.Unavailable("운전석 문 닫힘 상태를 읽지 못했어요. 다시 시도해 주세요.")) }
                            )
                        } catch (_: Exception) {
                            finish(Result.Unavailable("운전석 문 상태를 읽지 못했어요. 차량 연결을 확인해 주세요."))
                        }
                    }
                },
                onFailed = { finish(Result.Unavailable("운전석 문 닫힘 범위를 확인하지 못했어요. 다시 시도해 주세요.")) }
            )
        } catch (_: Exception) {
            finish(Result.Unavailable("운전석 문 상태를 읽지 못했어요. 차량 연결을 확인해 주세요."))
        }
    }
}
