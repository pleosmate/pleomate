package com.pleos.mate.core.pet.model

// Stable IDs are stored; labels and final character names may change later.
enum class PetCharacter(val id: String) {
    BEAR("bear"),
    RABBIT("rabbit"),
    PIXEL("pixel"),
    CLOUD("cloud"),
    SEED("seed"),
    ROVER("rover");

    val isPixel: Boolean get() = this != BEAR && this != RABBIT

    companion object {
        fun fromId(id: String?): PetCharacter? = entries.firstOrNull { it.id == id }
    }
}
