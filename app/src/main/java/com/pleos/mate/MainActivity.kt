package com.pleos.mate

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.pleos.mate.app.CompanionApp
import com.pleos.mate.app.CompanionAppViewModel
import com.pleos.mate.feature.journey.presentation.JourneyViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Android host only: permission requests, lifecycle and the Compose app entry point. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val model: CompanionAppViewModel by viewModels()
    private val journeys: JourneyViewModel by viewModels()
    private val powertrainPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        model.session.retryParking()
    }

    private fun reconnectVehicle() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) &&
            checkSelfPermission("android.car.permission.CAR_POWERTRAIN") != PackageManager.PERMISSION_GRANTED) {
            powertrainPermission.launch("android.car.permission.CAR_POWERTRAIN")
        } else model.session.retryParking()
    }

    override fun onResume() { super.onResume(); model.session.resume() }
    override fun onPause() { model.session.pause(); super.onPause() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reconnectVehicle()
        setContent { CompanionApp(model, journeys, ::reconnectVehicle) }
    }
}
