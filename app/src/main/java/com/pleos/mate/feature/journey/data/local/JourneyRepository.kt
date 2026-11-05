package com.pleos.mate.feature.journey.data.local

import com.pleos.mate.feature.journey.data.JourneyDataSource
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.journey.domain.JourneyArchive
import java.util.concurrent.Executors

/** Applies archive changes serially and delivers storage results on the main thread. */
class JourneyRepository(private val store: JourneyDataSource) {
    private val main = Handler(Looper.getMainLooper())

    fun read(onResult: (Result<JourneyArchive>) -> Unit) {
        io.execute {
            val result = runCatching(store::read)
            main.post { onResult(result) }
        }
    }

    fun update(change: (JourneyArchive) -> JourneyArchive, onResult: (Result<JourneyArchive>) -> Unit) {
        io.execute {
            val result = runCatching {
                change(store.read()).also(store::save)
            }
            main.post { onResult(result) }
        }
    }

    private companion object {
        val io = Executors.newSingleThreadExecutor()
    }
}
