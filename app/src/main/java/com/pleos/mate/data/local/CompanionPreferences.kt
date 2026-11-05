package com.pleos.mate.data.local

import android.content.Context
import com.pleos.mate.data.PetProfileDataSource

/** Keeps existing preference names and keys so installed user data remains compatible. */
class CompanionPreferences(context: Context) : PetProfileDataSource {
    private val companion = context.applicationContext.getSharedPreferences("companion", Context.MODE_PRIVATE)
    private val guidance = context.applicationContext.getSharedPreferences("mission_guidance", Context.MODE_PRIVATE)

    override val characterId: String? get() = companion.getString("character_id", null)
    override val petName: String get() = companion.getString("pet_name", "").orEmpty()
    override val outfit: String get() = companion.getString("outfit", "none").orEmpty()
    override val room: String get() = companion.getString("room", "none").orEmpty()
    val reactionVoiceEnabled: Boolean get() = companion.getBoolean("reaction_voice", false)
    val seenGuides: Set<String> get() = guidance.getStringSet("seen", emptySet()).orEmpty().toSet()

    override fun saveProfile(characterId: String, name: String) {
        companion.edit().putString("character_id", characterId).putString("pet_name", name).apply()
    }

    override fun saveDecoration(key: String, id: String) { companion.edit().putString(key, id).apply() }
    fun saveReactionVoice(enabled: Boolean) { companion.edit().putBoolean("reaction_voice", enabled).apply() }
    fun saveSeenGuides(ids: Set<String>) { guidance.edit().putStringSet("seen", ids).apply() }
}
