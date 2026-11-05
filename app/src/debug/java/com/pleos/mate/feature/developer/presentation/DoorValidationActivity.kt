package com.pleos.mate.feature.developer.presentation

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pleos.mate.R

class DoorValidationActivity : AppCompatActivity() {
    private var vehicle: Vehicle? = null
    private lateinit var resultText: TextView
    private lateinit var readButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById(R.id.result_text)
        readButton = findViewById(R.id.read_button)
        readButton.isEnabled = false

        try {
            vehicle = Vehicle(this)
            vehicle?.initialize()
            readButton.isEnabled = true
            resultText.setText(R.string.vehicle_ready)
        } catch (e: Exception) {
            showError(e)
            readButton.isEnabled = false
        }

        readButton.setOnClickListener { readDoorPosition() }

        // Debug-only activities resolve only when included in the installed build.
        listOf(
            "PetMotionPreviewActivity" to R.string.open_pet_motion,
            "WindowValidationActivity" to R.string.open_window_probe
        ).forEach { (activity, label) ->
            val intent = Intent().setClassName(this, "com.pleos.mate.$activity")
            if (intent.resolveActivity(packageManager) != null) {
                findViewById<LinearLayout>(R.id.main_container).addView(Button(this).apply {
                    setText(label)
                    minHeight = (64 * resources.displayMetrics.density).toInt()
                    setOnClickListener { startActivity(intent) }
                })
            }
        }
    }

    private fun readDoorPosition() {
        val sdk = vehicle ?: return
        readButton.isEnabled = false
        resultText.setText(R.string.door_loading)

        try {
            sdk.getDoor().getCurrentDoorPosition(
                area = DoorArea(DoorValue.ROW_1_LEFT),
                onComplete = { pos: Int? ->
                    Log.d("DoorTest", "ROW_1_LEFT position: $pos")
                    runOnUiThread {
                        if (!isDestroyed) {
                            resultText.text = getString(
                                R.string.door_result,
                                pos?.toString() ?: getString(R.string.door_no_value)
                            )
                            readButton.isEnabled = true
                        }
                    }
                },
                onFailed = { e: Exception -> showError(e) }
            )
        } catch (e: Exception) {
            showError(e)
        }
    }

    private fun showError(e: Exception) {
        Log.e("DoorTest", "Vehicle SDK error", e)
        runOnUiThread {
            if (!isDestroyed) {
                resultText.text = getString(
                    R.string.vehicle_error,
                    "${e.javaClass.simpleName}: ${e.message.orEmpty()}"
                )
                readButton.isEnabled = vehicle != null
            }
        }
    }

    override fun onDestroy() {
        try {
            vehicle?.release()
        } finally {
            vehicle = null
            super.onDestroy()
        }
    }
}
