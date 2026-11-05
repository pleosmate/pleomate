package com.pleos.mate.feature.journey.data.vehicle

import android.content.Context
import com.pleos.mate.feature.journey.domain.JourneyObservation
import com.pleos.mate.feature.journey.domain.JourneyObservationFactory
import com.pleos.mate.feature.journey.domain.OdometerSample
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PleosJourneyObservationFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : JourneyObservationFactory {
    override fun create(
        onSample: (OdometerSample) -> Unit,
        onProblem: (String) -> Unit
    ): JourneyObservation = OdometerObserver(context, onSample, onProblem)
}
