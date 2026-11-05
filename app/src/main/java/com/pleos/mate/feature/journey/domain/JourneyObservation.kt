package com.pleos.mate.feature.journey.domain

/** Vehicle observation port: the state holder does not construct the Pleos SDK. */
interface JourneyObservation {
    fun start()
    fun requestSample()
    fun stop()
}

fun interface JourneyObservationFactory {
    fun create(onSample: (OdometerSample) -> Unit, onProblem: (String) -> Unit): JourneyObservation
}
