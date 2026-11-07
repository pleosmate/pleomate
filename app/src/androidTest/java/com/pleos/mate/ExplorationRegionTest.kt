package com.pleos.mate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.feature.exploration.data.ProvinceResolver
import com.pleos.mate.feature.exploration.data.ExplorationAreas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplorationRegionTest {

    @Test fun resolvesNeighborhoodAndLoadsItsHierarchy() {
        val areas = ExplorationAreas(InstrumentationRegistry.getInstrumentation().targetContext)
        val gangnam = areas.children("sgg", "11").first { it.name == "강남구" }
        val yeoksam = areas.children("emd", gangnam.id).first { it.name == "역삼1동" }
        assertEquals("1168064000", yeoksam.id)
        assertEquals(yeoksam.id, areas.resolve(37.499, 127.032)?.id)
        assertNull(areas.resolve(37.421997, -122.084))
    }

    @Test fun resolvesInlandAndIslandAndRejectsOutside() {
        val resolver = ProvinceResolver(InstrumentationRegistry.getInstrumentation().targetContext)
        assertEquals("서울특별시", resolver.resolve(37.5665, 126.9780))
        assertEquals("부산광역시", resolver.resolve(35.1796, 129.0756))
        assertEquals("제주특별자치도", resolver.resolve(33.38, 126.55))
        assertEquals("제주특별자치도", resolver.resolve(33.50, 126.52)) // coast
        assertEquals("제주특별자치도", resolver.resolve(33.50, 126.95)) // Udo
        assertNull(resolver.resolve(37.6313862865, 126.9750746667)) // Seoul/Gyeonggi border
        assertNull(resolver.resolve(37.421997, -122.084))
        assertNull(resolver.resolve(33.0, 126.0))
    }
}
