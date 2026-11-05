package com.pleos.mate.core.pet.model

/** Appearance milestones are independent of the earlier motion unlock levels. */
internal object PetAppearance {
    fun stageLevel(level: Int) = when {
        level >= 15 -> 15
        level >= 5 -> 5
        else -> 1
    }

    fun asset(character: PetCharacter, level: Int): String {
        val stage = stageLevel(level)
        return if (stage > 1) "pets/evolution/${character.id}-lv$stage.png"
        else when (character) {
            PetCharacter.BEAR, PetCharacter.RABBIT -> "pets/rig/${character.id}.png"
            PetCharacter.PIXEL -> "pets/pixel-traveler-v2.png"
            else -> "pets/${character.id}-v1.png"
        }
    }

    fun spriteRows(character: PetCharacter, level: Int) =
        if (character == PetCharacter.PIXEL && stageLevel(level) == 1) 2 else 4
}
