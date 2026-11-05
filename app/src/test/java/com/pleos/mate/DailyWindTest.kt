package com.pleos.mate

import com.pleos.mate.feature.growth.domain.GrowthProgress
import com.pleos.mate.feature.growth.data.local.GrowthStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId

class DailyWindTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun dailyRewardsPersistWithoutReplacingFirstMemory() {
        val day = "2026-11-05"
        val now = LocalDate.parse(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val first = GrowthProgress().completeDailyWind(now, day)
        assertEquals(10, first.totalXp)
        assertEquals(first, first.completeDailyWind(now + 1, day))
        val repeat = first.completeDailyWind(now + 86400000, "2026-11-06")
        assertEquals(12, repeat.totalXp)
        assertEquals(first.windCompletedAt, repeat.windCompletedAt)
        assertEquals(repeat, repeat.completeDailyWind(now + 86400001, "2026-11-06"))
        val store = GrowthStore(folder.newFolder())
        store.save(repeat.hideWindMemory())
        val restored = store.read()
        assertEquals(12, restored.totalXp)
        assertNull(restored.windMemoryAt)
        assertTrue(restored.hasDailyWind("2026-11-06"))
    }
}
