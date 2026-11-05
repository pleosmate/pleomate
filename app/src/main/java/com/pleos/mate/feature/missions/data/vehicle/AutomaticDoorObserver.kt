package com.pleos.mate.feature.missions.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import android.content.Context
import android.os.Handler
import android.os.Looper

/** Watches door state only while a parked daily check needs action; it never controls doors. */
internal class AutomaticDoorObserver(context: Context, private val onChanged: () -> Unit) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)
    private var sdk: Vehicle? = null
    private val positionListeners = mutableMapOf<DoorArea, DoorPositionListener>()
    private var generation = 0
    private var debounce: Runnable? = null

    fun start() {
        stop()
        val token = generation
        try {
            val vehicle = Vehicle(appContext).also { sdk = it; it.initialize() }
            vehicle.getDoor().checkDoorPositionCapabilities(
                onComplete = { positionConfig, bounds -> main.post {
                    if (token != generation) return@post
                    if (positionConfig.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE) ||
                        bounds[driverDoor]?.minValue == null || bounds[driverDoor]?.maxValue == null) return@post
                    try {
                        val positionListener = object : DoorPositionListener {
                            override fun onPositionUpdated(pos: Int?) = changed(token)
                            override fun onFailed(e: Exception) = Unit
                        }
                        positionListeners[driverDoor] = positionListener
                        vehicle.getDoor().registerDoorPosition(driverDoor, positionListener)
                    } catch (_: Exception) { stop() }
                } },
                onFailed = { }
            )
        } catch (_: Exception) { stop() }
    }

    private fun changed(token: Int) {
        main.post {
            if (token != generation) return@post
            debounce?.let(main::removeCallbacks)
            debounce = Runnable { if (token == generation) onChanged() }.also { main.postDelayed(it, 500) }
        }
    }

    fun stop() {
        generation++
        debounce?.let(main::removeCallbacks)
        debounce = null
        val door = sdk?.getDoor()
        positionListeners.forEach { (area, listener) -> try { door?.unregisterDoorPosition(area, listener) } catch (_: Exception) { } }
        positionListeners.clear()
        try { sdk?.release() } catch (_: Exception) { }
        sdk = null
    }
}
