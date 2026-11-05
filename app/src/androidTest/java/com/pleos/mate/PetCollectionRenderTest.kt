package com.pleos.mate

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.rendering.PetMotionView
import com.pleos.mate.feature.companion.domain.PetCollection
import org.junit.Assert.*
import org.junit.Test

class PetCollectionRenderTest {
    @Test fun bothCharactersRenderEveryDecorationAndGrowthStageInAllDirections() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bitmap = Bitmap.createBitmap(720, 600, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val view = PetMotionView(instrumentation.targetContext)
            view.layout(0, 0, 720, 600)
            for (character in PetCharacter.entries) for (direction in PetRunDirection.entries)
                for (outfit in PetCollection.outfits) for (room in PetCollection.rooms) for (level in listOf(1, 3, 5)) {
                    bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
                    view.character = character
                    view.runningDirection = direction
                    view.level = level
                    view.outfit = outfit.id
                    view.room = room.id
                    view.draw(canvas)
                    assertTrue(bitmap.getPixel(360, 400) != 0)
                }
            bitmap.recycle()
        }
    }
}
