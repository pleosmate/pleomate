package com.pleos.mate.data

/** Account-bound fields; device guidance and voice settings stay local. */
interface PetProfileDataSource {
    val characterId: String?
    val petName: String
    val outfit: String
    val room: String
    fun saveProfile(characterId: String, name: String)
    fun saveDecoration(key: String, id: String)
}
