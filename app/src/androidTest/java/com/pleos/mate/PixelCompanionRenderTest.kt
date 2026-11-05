package com.pleos.mate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.rendering.PixelPetRenderer
import org.junit.Assert.*
import org.junit.Test

class PixelCompanionRenderTest {
    @Test fun newCompanionsRenderEveryReactionAndBothGaitsFromPackagedAssets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val renderer = PixelPetRenderer(instrumentation.targetContext)
            val bitmap = Bitmap.createBitmap(360, 300, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(360 * 300)
            fun render(character: PetCharacter, direction: PetRunDirection?, walking: Boolean,
                phase: Float, motion: PetMotion?, progress: Float): Int {
                bitmap.eraseColor(Color.TRANSPARENT)
                renderer.draw(Canvas(bitmap), character, direction, 0f, phase,
                    PetMotionRules.frame(motion, progress, character), motion, "none", walking, progress)
                bitmap.getPixels(pixels, 0, 360, 0, 0, 360, 300)
                assertTrue("$character $motion $phase must be visible", pixels.count { Color.alpha(it) > 0 } > 500)
                return pixels.contentHashCode()
            }
            for (character in listOf(PetCharacter.CLOUD, PetCharacter.SEED, PetCharacter.ROVER)) {
                val idle = render(character, null, false, 0f, null, 0f)
                for (motion in PetMotion.entries) {
                    assertNotEquals(idle, render(character, null, false, 0f, motion, .5f))
                }
                val jump = listOf(.05f, .5f, .95f).map { render(character, null, false, 0f, PetMotion.JUMP, it) }
                assertEquals(3, jump.toSet().size)
                for (phase in listOf(0f, .25f, .5f, .75f)) {
                    val walk = render(character, PetRunDirection.RIGHT, true, phase, null, 0f)
                    assertNotEquals(walk, render(character, PetRunDirection.RIGHT, false, phase, null, 0f))
                    assertNotEquals(walk, render(character, PetRunDirection.LEFT, true, phase, null, 0f))
                }
            }
            bitmap.recycle()
        }
    }
}
