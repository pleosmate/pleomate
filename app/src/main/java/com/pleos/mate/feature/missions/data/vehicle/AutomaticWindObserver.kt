package com.pleos.mate.feature.missions.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.missions.domain.WindMissionRules

/** Read-only app-session observer. It reports an open front-left window; it never operates the window. */
internal class AutomaticWindObserver(context: Context, private val onOpen: () -> Unit) {
    private val appContext = context.applicationContext
    private val area = WindowArea(WindowValue.ROW_1_LEFT)
    private val main = Handler(Looper.getMainLooper())
    private var sdk: Vehicle? = null
    private var listener: WindowPositionListener? = null
    private var generation = 0

    fun start() {
        stop()
        val token = generation
        try {
            val vehicle = Vehicle(appContext).also { sdk = it; it.initialize() }
            vehicle.getWindow().checkWindowPositionCapabilities(
                onComplete = { config, areas -> main.post {
                    if (token == generation) {
                        val bounds = areas[area]
                        if (config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) &&
                            WindMissionRules.supported(bounds?.minValue, bounds?.maxValue)) {
                            val updates = object : WindowPositionListener {
                                override fun onWindowPositionUpdated(position: Int?) {
                                    main.post { if (token == generation && WindMissionRules.isOpen(position)) onOpen() }
                                }
                                override fun onFailed(e: Exception) = Unit
                            }
                            listener = updates
                            try {
                                vehicle.getWindow().registerWindowPosition(area, updates)
                                vehicle.getWindow().getCurrentWindowPosition(area,
                                    onComplete = { position -> main.post { if (token == generation && WindMissionRules.isOpen(position)) onOpen() } },
                                    onFailed = { })
                            } catch (_: Exception) { stop() }
                        }
                    }
                } },
                onFailed = { }
            )
        } catch (_: Exception) { stop() }
    }

    fun stop() {
        generation++
        try { listener?.let { sdk?.getWindow()?.unregisterWindowPosition(area, it) } } catch (_: Exception) { }
        listener = null
        try { sdk?.release() } catch (_: Exception) { }
        sdk = null
    }
}
