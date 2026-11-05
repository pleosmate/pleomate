package com.pleos.mate.core.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.pleos.mate.core.pet.model.VehicleMotionRules
import java.util.concurrent.Executor

/** Requires both actual Park selection and unrestricted platform UX. Unknown means unavailable. */
class ParkedAccess(
    private val context: Context,
    private val worker: Executor,
    private val onChanged: (Boolean, String) -> Unit,
    private val onMotionChanged: (Boolean) -> Unit = {}
) {
    private val main = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var restrictions: CarUxRestrictionsManager? = null
    private var active = false
    private var parked = false
    private var unrestricted = false
    private var moving = false
    private var generation = 0
    private var revision = 0
    val allowed: Boolean get() = active && parked && unrestricted

    private var gearListener: CarPropertyManager.CarPropertyEventCallback? = null
    private var speedListener: CarPropertyManager.CarPropertyEventCallback? = null
    private var connection = 0

    fun start() {
        if (active) return
        active = true
        setMoving(false)
        val token = ++generation
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            onChanged(false, "차량 플랫폼에서 미션을 사용할 수 있어요.")
            return
        }
        onChanged(false, "주차 상태와 사용 가능 여부를 확인하고 있어요.")
        try {
            car = Car.createCar(context, main, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (!active || token != generation) return@createCar
                val connectionToken = ++connection
                revision++
                if (!ready) {
                    properties = null
                    restrictions = null
                    parked = false
                    unrestricted = false
                    setMoving(false)
                    onChanged(false, "차량 연결이 끊겼어요. 다시 연결해 주세요.")
                } else {
                    setMoving(false)
                    try {
                        try { gearListener?.let { properties?.unregisterCallback(it) } } catch (_: Exception) { }
                        try { speedListener?.let { properties?.unregisterCallback(it) } } catch (_: Exception) { }
                        try { restrictions?.unregisterListener() } catch (_: Exception) { }
                        properties = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                        restrictions = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                        restrictions?.registerListener { ux ->
                            if (active && token == generation && connectionToken == connection) {
                                revision++
                                unrestricted = !ux.isRequiresDistractionOptimization && ux.activeRestrictions == 0
                                publish()
                            }
                        }
                        val updates = object : CarPropertyManager.CarPropertyEventCallback {
                            override fun onChangeEvent(value: CarPropertyValue<*>) {
                                if (!active || token != generation || connectionToken != connection || value.propertyId != VehiclePropertyIds.GEAR_SELECTION) return
                                revision++
                                parked = value.status == CarPropertyValue.STATUS_AVAILABLE && value.value == VehicleGear.GEAR_PARK
                                publish()
                            }
                            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                                if (active && token == generation && connectionToken == connection) { revision++; parked = false; publish() }
                            }
                        }
                        gearListener = updates
                        check(properties?.registerCallback(updates, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE) == true)
                        val speedUpdates = object : CarPropertyManager.CarPropertyEventCallback {
                            override fun onChangeEvent(value: CarPropertyValue<*>) {
                                if (!active || token != generation || connectionToken != connection || value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
                                val speed = if (value.status == CarPropertyValue.STATUS_AVAILABLE) (value.value as? Number)?.toFloat() else null
                                if (speed == null) setMoving(false) else setMoving(VehicleMotionRules.isMoving(speed, moving))
                            }
                            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                                if (active && token == generation && connectionToken == connection && propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED) setMoving(false)
                            }
                        }
                        speedListener = speedUpdates
                        try {
                            if (properties?.registerCallback(speedUpdates, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI) == true) {
                                worker.execute {
                                    val speed = try { properties?.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)?.value } catch (_: Exception) { null }
                                    main.post {
                                        if (active && token == generation && connectionToken == connection && speed != null) {
                                            setMoving(VehicleMotionRules.isMoving(speed, moving))
                                        }
                                    }
                                }
                            } else setMoving(false)
                        } catch (_: Exception) {
                            speedListener = null
                            setMoving(false)
                        }
                        verify { publish() }
                    } catch (e: Exception) {
                        parked = false
                        unrestricted = false
                        setMoving(false)
                        onChanged(false, "주차 상태를 읽을 수 없어요. 차량 정보 권한과 연결을 확인해 주세요.")
                    }
                }
            }
        } catch (e: Exception) {
            setMoving(false)
            onChanged(false, "차량 상태를 확인할 수 없어요. 연결 후 다시 시도해 주세요.")
        }
    }

    /** Re-read both values before starting or awarding; never accept a manual parked toggle. */
    fun verify(done: (Boolean) -> Unit) {
        val token = generation
        val observedRevision = revision
        val manager = properties
        val uxManager = restrictions
        worker.execute {
            val snapshot = try {
                val gear = manager?.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION, 0)
                val ux = uxManager?.currentCarUxRestrictions
                (gear?.status == CarPropertyValue.STATUS_AVAILABLE && gear.value == VehicleGear.GEAR_PARK) to
                    (ux != null && !ux.isRequiresDistractionOptimization && ux.activeRestrictions == 0)
            } catch (e: Exception) { false to false }
            main.post {
                if (active && token == generation) {
                    if (observedRevision != revision) { verify(done); return@post }
                    parked = snapshot.first
                    unrestricted = snapshot.second
                    publish()
                    done(allowed)
                } else done(false)
            }
        }
    }

    private fun publish() {
        onChanged(allowed, if (allowed) "주차 중 · 미션을 시작할 수 있어요." else "주차(P) 후 사용 가능 상태에서 함께해요.")
    }

    private fun setMoving(value: Boolean) {
        if (moving == value) return
        moving = value
        onMotionChanged(value)
    }

    fun stop() {
        active = false
        generation++
        parked = false
        unrestricted = false
        setMoving(false)
        try { gearListener?.let { properties?.unregisterCallback(it) } } catch (_: Exception) { }
        try { speedListener?.let { properties?.unregisterCallback(it) } } catch (_: Exception) { }
        try { restrictions?.unregisterListener() } catch (_: Exception) { }
        try { car?.disconnect() } catch (_: Exception) { }
        gearListener = null
        speedListener = null
        properties = null
        restrictions = null
        car = null
    }
}
