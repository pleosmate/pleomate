package com.pleos.mate

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.feature.growth.data.local.GrowthRepository
import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.domain.GrowthProgress
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrowthRepositoryTest {
    @Test fun saveAndReadAreSerializedAndCallbacksReturnOnMainThread() {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "growth-repository-test-${System.nanoTime()}").apply { mkdirs() }
        try {
            val repository = GrowthRepository(GrowthStore(directory))
            val initial = CountDownLatch(1)
            val saved = CountDownLatch(1)
            val restored = CountDownLatch(1)
            val expected = GrowthProgress().completeWind(1234L)
            repository.read { result ->
                assertTrue(Looper.myLooper() == Looper.getMainLooper())
                assertEquals(GrowthProgress(), result.getOrThrow())
                initial.countDown()
            }
            assertTrue(initial.await(10, TimeUnit.SECONDS))
            repository.save(expected) { result ->
                assertTrue(Looper.myLooper() == Looper.getMainLooper())
                result.getOrThrow()
                saved.countDown()
            }
            assertTrue(saved.await(10, TimeUnit.SECONDS))
            repository.read { result ->
                assertTrue(Looper.myLooper() == Looper.getMainLooper())
                assertEquals(expected, result.getOrThrow())
                restored.countDown()
            }
            assertTrue(restored.await(10, TimeUnit.SECONDS))
        } finally {
            directory.deleteRecursively()
        }
    }
}
