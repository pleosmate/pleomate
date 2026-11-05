package com.pleos.mate.feature.journey.data

import com.pleos.mate.feature.journey.domain.JourneyArchive

/** Blocking storage boundary; repositories execute operations off the main thread. */
interface JourneyDataSource {
    fun read(): JourneyArchive
    fun save(archive: JourneyArchive)
}
