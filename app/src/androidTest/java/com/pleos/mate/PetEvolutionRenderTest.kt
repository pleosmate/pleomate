package com.pleos.mate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.rendering.PetRigRenderer
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PetEvolutionRenderTest {
    @Test fun allEighteenAppearancesRenderReactionsMovementAndDecorations() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val renderer = PetRigRenderer(context)
            val bitmap = Bitmap.createBitmap(360, 300, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val pixels = IntArray(360 * 300)
            val comparison = Bitmap.createBitmap(1080, 1980, Bitmap.Config.ARGB_8888)
            val comparisonCanvas = Canvas(comparison).apply { drawColor(Color.rgb(76, 91, 82)) }
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 18f }
            fun draw(character: PetCharacter, level: Int, direction: PetRunDirection? = null,
                phase: Float = 0f, walking: Boolean = false, motion: PetMotion? = null,
                progress: Float = 0f, outfit: String = "none"): Int {
                bitmap.eraseColor(Color.TRANSPARENT)
                renderer.draw(canvas, character, direction, 0f, phase,
                    PetMotionRules.frame(motion, progress, character), motion, level, outfit, "none", walking, progress)
                bitmap.getPixels(pixels, 0, 360, 0, 0, 360, 300)
                assertTrue("$character Lv.$level $direction $motion should be visible", pixels.count { Color.alpha(it) > 128 } > 400)
                // No pose should escape the stage's edges.
                val description = "$character Lv.$level $direction $motion progress=$progress phase=$phase"
                assertTrue("Top/bottom clipping: $description", (0 until 360).all { Color.alpha(pixels[it]) == 0 && Color.alpha(pixels[299 * 360 + it]) == 0 })
                assertTrue("Side clipping: $description", (0 until 300).all { Color.alpha(pixels[it * 360]) == 0 && Color.alpha(pixels[it * 360 + 359]) == 0 })
                return pixels.contentHashCode()
            }
            PetCharacter.entries.forEachIndexed { row, character ->
                val appearances = mutableSetOf<Int>()
                listOf(1, 5, 15).forEachIndexed { column, level ->
                    appearances += draw(character, level)
                    comparisonCanvas.drawBitmap(bitmap, column * 360f, row * 330f + 30f, null)
                    comparisonCanvas.drawText("${character.id} · Lv.$level", column * 360f + 12, row * 330f + 24, label)
                    for (motion in PetMotion.entries) for (progress in listOf(.05f, .5f, .95f))
                        draw(character, level, motion = motion, progress = progress)
                    for (direction in PetRunDirection.entries) for (walking in listOf(false, true))
                        for (phase in listOf(0f, .25f, .5f, .75f)) draw(character, level, direction, phase, walking)
                    for (outfit in listOf("breeze", "bow")) draw(character, level, outfit = outfit)
                }
                assertEquals("$character needs three different appearance images", 3, appearances.size)
            }
            File(context.cacheDir, "evolution-comparison.png").outputStream().use {
                comparison.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle(); comparison.recycle()
        }
    }
}
