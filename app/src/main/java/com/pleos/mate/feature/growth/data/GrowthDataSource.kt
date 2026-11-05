package com.pleos.mate.feature.growth.data

import com.pleos.mate.feature.growth.domain.GrowthProgress

/** Blocking storage boundary; repositories execute operations off the main thread. */
interface GrowthDataSource {
    fun read(): GrowthProgress
    fun save(progress: GrowthProgress)
}
