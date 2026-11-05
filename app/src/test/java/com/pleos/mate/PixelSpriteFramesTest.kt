package com.pleos.mate

import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.rendering.PixelSpriteFrames
import org.junit.Assert.*
import org.junit.Test

class PixelSpriteFramesTest {
    private val newcomers = listOf(PetCharacter.CLOUD, PetCharacter.SEED, PetCharacter.ROVER)

    @Test fun walkingAndRunningUseDistinctCompleteCycles() {
        newcomers.forEach { character ->
            val phases = listOf(0f, .25f, .5f, .75f)
            assertEquals(listOf(8, 9, 10, 11), phases.map { PixelSpriteFrames.cell(character, true, true, it, null, 0f) })
            assertEquals(listOf(12, 13, 14, 15), phases.map { PixelSpriteFrames.cell(character, true, false, it, null, 0f) })
            assertEquals(12, PixelSpriteFrames.cell(character, true, false, 1f, null, 0f))
        }
    }

    @Test fun jumpUsesTakeoffFlightAndLandingThenReturnsToIdle() {
        newcomers.forEach { character ->
            assertEquals(listOf(4, 5, 6), listOf(.05f, .5f, .95f).map {
                PixelSpriteFrames.cell(character, false, false, 0f, PetMotion.JUMP, it)
            })
            assertEquals(0, PixelSpriteFrames.cell(character, false, false, 0f, null, 0f))
            assertEquals(0, PixelSpriteFrames.cell(character, false, false, 0f, PetMotion.JUMP, 1f))
            assertEquals(1, PixelSpriteFrames.cell(character, false, false, 0f, PetMotion.SMILE, .5f))
            assertEquals(7, PixelSpriteFrames.cell(character, false, false, 0f, PetMotion.CELEBRATE, .5f))
        }
    }

    @Test fun travelerKeepsItsOriginalEightCellLayout() {
        PetMotion.entries.forEach {
            assertTrue(PixelSpriteFrames.cell(PetCharacter.PIXEL, false, false, 0f, it, .5f) in 0..3)
        }
        assertEquals(7, PixelSpriteFrames.cell(PetCharacter.PIXEL, true, false, .75f, null, 0f))
        assertEquals(PetCharacter.PIXEL, PetCharacter.fromId("pixel"))
        newcomers.forEach { assertEquals(it, PetCharacter.fromId(it.id)) }
    }
}
