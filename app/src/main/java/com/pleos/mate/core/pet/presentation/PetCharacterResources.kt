package com.pleos.mate.core.pet.presentation

import com.pleos.mate.R
import com.pleos.mate.core.pet.model.PetCharacter

internal val PetCharacter.labelResource: Int
    get() = when (this) {
        PetCharacter.BEAR -> R.string.character_bear
        PetCharacter.RABBIT -> R.string.character_rabbit
        PetCharacter.PIXEL -> R.string.character_pixel
        PetCharacter.CLOUD -> R.string.character_cloud
        PetCharacter.SEED -> R.string.character_seed
        PetCharacter.ROVER -> R.string.character_rover
    }

internal val PetCharacter.descriptionResource: Int
    get() = when (this) {
        PetCharacter.BEAR -> R.string.character_bear_description
        PetCharacter.RABBIT -> R.string.character_rabbit_description
        PetCharacter.PIXEL -> R.string.character_pixel_description
        PetCharacter.CLOUD -> R.string.character_cloud_description
        PetCharacter.SEED -> R.string.character_seed_description
        PetCharacter.ROVER -> R.string.character_rover_description
    }
