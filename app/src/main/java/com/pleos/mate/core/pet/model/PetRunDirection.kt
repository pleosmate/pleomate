package com.pleos.mate.core.pet.model

/** Stable body-view order in each plush rig atlas. */
enum class PetRunDirection(val label: String) {
    FRONT("정면"), LEFT("왼쪽"), RIGHT("오른쪽"), BACK("뒷면"),
    FRONT_LEFT("앞 왼쪽"), FRONT_RIGHT("앞 오른쪽")
}
