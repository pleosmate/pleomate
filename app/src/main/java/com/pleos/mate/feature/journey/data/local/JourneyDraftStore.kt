package com.pleos.mate.feature.journey.data.local

import android.content.Context
import com.pleos.mate.feature.journey.domain.JourneyEditor
import com.pleos.mate.feature.journey.domain.JourneyEditorMode

class JourneyDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("journey_editor", Context.MODE_PRIVATE)

    fun read(): JourneyEditor? = preferences.getString("id", null)?.let { id ->
        JourneyEditor(id, JourneyEditorMode.fromStorage(preferences.getString("mode", "manual")),
            preferences.getString("theme", "일상").orEmpty(), preferences.getString("memo", "").orEmpty())
    }

    fun save(value: JourneyEditor) {
        preferences.edit().putString("id", value.id).putString("mode", value.mode.storageValue)
            .putString("theme", value.theme).putString("memo", value.memo).apply()
    }

    fun clear() { preferences.edit().clear().apply() }
}
