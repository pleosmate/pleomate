package com.pleos.mate.feature.developer.presentation

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pleos.mate.R

/** Emulator-only read probe. Raw values never award mission completion or XP. */
class WindowValidationActivity : AppCompatActivity() {
    private var vehicle: Vehicle? = null
    private val area = WindowArea(WindowValue.ROW_1_LEFT)
    private lateinit var result: TextView
    private lateinit var check: Button
    private lateinit var read: Button
    private lateinit var subscribe: Button
    private lateinit var stop: Button
    private var active = false
    private var readable = false
    private var pending = false
    private var subscribed = false
    private var generation = 0
    private val entries = ArrayDeque<String>()

    private val listener = object : WindowPositionListener {
        override fun onWindowPositionUpdated(position: Int?) {
            runOnUiThread {
                if (active && subscribed) record("UPDATE ROW_1_LEFT raw=$position")
            }
        }

        override fun onFailed(e: Exception) {
            runOnUiThread {
                if (active && subscribed) error("SUBSCRIBE", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_validation)
        result = findViewById(R.id.window_result)
        check = findViewById(R.id.window_check)
        read = findViewById(R.id.window_read)
        subscribe = findViewById(R.id.window_subscribe)
        stop = findViewById(R.id.window_stop)
        try {
            vehicle = Vehicle(this)
            vehicle?.initialize()
            record("INITIALIZE called; service readiness not yet verified")
        } catch (e: Exception) {
            vehicle?.release()
            vehicle = null
            error("INITIALIZE", e)
        }
        check.setOnClickListener { checkCapabilities() }
        read.setOnClickListener { readPosition() }
        subscribe.setOnClickListener {
            try {
                subscribed = true
                vehicle?.getWindow()?.registerWindowPosition(area, listener)
                record("SUBSCRIBE requested; verify actual UPDATE callbacks")
            } catch (e: Exception) {
                subscribed = false
                error("SUBSCRIBE", e)
            }
            updateButtons()
        }
        stop.setOnClickListener { stopSubscription() }
    }

    override fun onStart() {
        super.onStart()
        active = true
        readable = false
        record("START: recheck capability before reading; emulator only")
        updateButtons()
    }

    private fun checkCapabilities() {
        val sdk = vehicle ?: return
        val token = generation
        pending = true
        readable = false
        updateButtons()
        record("CAPABILITY requested")
        try {
            sdk.getWindow().checkWindowPositionCapabilities(
                onComplete = { config: CapabilityConfig, areas: Map<WindowArea, AreaBounds<Int>> ->
                    finishRequest(token) {
                        val bounds = areas[area]
                        readable = bounds != null &&
                            (config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE)
                        record("CAPABILITY access=${config.accessType}, ROW_1_LEFT bounds=$bounds, readable=$readable")
                        areas.forEach { (key, value) -> record("AREA ${key.values.toList()} bounds=$value") }
                    }
                },
                onFailed = { e -> finishRequest(token) { error("CAPABILITY", e) } }
            )
        } catch (e: Exception) {
            finishRequest(token) { error("CAPABILITY", e) }
        }
    }

    private fun readPosition() {
        val sdk = vehicle ?: return
        val token = generation
        pending = true
        updateButtons()
        record("GET requested")
        try {
            sdk.getWindow().getCurrentWindowPosition(
                area = area,
                onComplete = { value -> finishRequest(token) { record("GET ROW_1_LEFT raw=$value") } },
                onFailed = { e -> finishRequest(token) { error("GET", e) } }
            )
        } catch (e: Exception) {
            finishRequest(token) { error("GET", e) }
        }
    }

    private fun finishRequest(token: Int, action: () -> Unit) {
        runOnUiThread {
            pending = false
            if (isDestroyed) {
                releaseVehicle()
            } else {
                if (active && token == generation) action()
                updateButtons()
            }
        }
    }

    private fun stopSubscription() {
        if (subscribed) {
            subscribed = false
            try {
                vehicle?.getWindow()?.unregisterWindowPosition(area, listener)
                record("UNSUBSCRIBE requested")
            } catch (e: Exception) {
                error("UNSUBSCRIBE", e)
            }
        }
        updateButtons()
    }

    private fun updateButtons() {
        check.isEnabled = active && vehicle != null && !pending && !subscribed
        read.isEnabled = active && readable && !pending
        subscribe.isEnabled = active && readable && !pending && !subscribed
        stop.isEnabled = active && subscribed
    }

    private fun record(message: String) {
        val entry = "${SystemClock.elapsedRealtime()}ms $message"
        Log.d("WindowProbe", entry)
        entries.addLast(entry)
        while (entries.size > 40) entries.removeFirst()
        result.text = entries.joinToString("\n")
    }

    private fun error(stage: String, e: Exception) {
        Log.e("WindowProbe", stage, e)
        record("$stage FAILED ${e.javaClass.simpleName}: ${e.message}")
    }

    override fun onStop() {
        active = false
        generation++
        readable = false
        stopSubscription()
        super.onStop()
    }

    override fun onDestroy() {
        if (!pending) releaseVehicle()
        super.onDestroy()
    }

    private fun releaseVehicle() {
        try {
            vehicle?.release()
        } catch (e: Exception) {
            Log.e("WindowProbe", "RELEASE", e)
        } finally {
            vehicle = null
        }
    }
}
