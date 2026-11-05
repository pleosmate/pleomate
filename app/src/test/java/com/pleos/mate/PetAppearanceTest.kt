package com.pleos.mate

import com.pleos.mate.core.pet.model.PetAppearance
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.rendering.PixelSpriteFrames
import org.junit.Assert.*
import org.junit.Test

class PetAppearanceTest {
    @Test fun evolutionChangesOnlyAtFiveAndFifteenForEveryCharacter() {
        for (character in PetCharacter.entries) {
            assertEquals(PetAppearance.asset(character, 1), PetAppearance.asset(character, 4))
            assertNotEquals(PetAppearance.asset(character, 4), PetAppearance.asset(character, 5))
            assertEquals(PetAppearance.asset(character, 5), PetAppearance.asset(character, 14))
            assertNotEquals(PetAppearance.asset(character, 14), PetAppearance.asset(character, 15))
            assertEquals(PetAppearance.asset(character, 15), PetAppearance.asset(character, 100))
        }
    }

    @Test fun travelerEvolutionGainsCompleteWalkRunAndJumpSprites() {
        assertEquals(2, PetAppearance.spriteRows(PetCharacter.PIXEL, 4))
        assertEquals(4, PetAppearance.spriteRows(PetCharacter.PIXEL, 5))
        for (level in listOf(5, 15)) {
            assertEquals(8, PixelSpriteFrames.cell(PetCharacter.PIXEL, true, true, 0f, null, 0f, level))
            assertEquals(12, PixelSpriteFrames.cell(PetCharacter.PIXEL, true, false, 0f, null, 0f, level))
            assertEquals(listOf(4, 5, 6), listOf(.05f, .5f, .95f).map {
                PixelSpriteFrames.cell(PetCharacter.PIXEL, false, false, 0f, PetMotion.JUMP, it, level)
            })
        }
    }
}
