package com.pleos.mate.feature.growth.data.local

import com.pleos.mate.feature.growth.data.GrowthDataSource
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.growth.domain.GrowthProgress
import java.util.concurrent.Executors

/** Serializes growth-record I/O and returns results on the main thread. */
class GrowthRepository(private val store: GrowthDataSource) {
    private val main = Handler(Looper.getMainLooper())

    fun read(onResult: (Result<GrowthProgress>) -> Unit) {
        io.execute {
            val result = runCatching(store::read)
            main.post { onResult(result) }
        }
    }

    fun save(progress: GrowthProgress, onResult: (Result<Unit>) -> Unit) {
        io.execute {
            val result = runCatching { store.save(progress) }
            main.post { onResult(result) }
        }
    }

    private companion object {
        // Keep writes serialized across ViewModel recreation.
        val io = Executors.newSingleThreadExecutor()
    }
}
