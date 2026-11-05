package com.pleos.mate.core.pet.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pleos.mate.core.pet.model.PetAppearance
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionFrame
import com.pleos.mate.core.pet.model.PetRunDirection

/** A separate sprite renderer keeps pixel edges sharp and never stretches individual limbs. */
internal class PixelPetRenderer(private val context: Context) {
    private val sheets = mutableMapOf<Pair<PetCharacter, Int>, Bitmap>()
    private fun sheet(character: PetCharacter, level: Int): Bitmap = sheets.getOrPut(character to PetAppearance.stageLevel(level)) {
        val rows = PetAppearance.spriteRows(character, level)
        context.assets.open(PetAppearance.asset(character, level)).use { stream ->
            val original = requireNotNull(BitmapFactory.decodeStream(stream))
            // Render each sprite on a real 48×48 grid before enlarging it on screen.
            Bitmap.createBitmap(192, rows * 48, Bitmap.Config.ARGB_8888).also { pixels ->
                Canvas(pixels).drawBitmap(original, null, Rect(0, 0, 192, rows * 48), Paint().apply { isFilterBitmap = false })
                original.recycle()
            }
        }
    }
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }

    fun draw(canvas: Canvas, character: PetCharacter, direction: PetRunDirection?, idle: Float, run: Float,
        gesture: PetMotionFrame, motion: PetMotion?, room: String, walking: Boolean, progress: Float, level: Int = 1) {
        val sheet = sheet(character, level)
        if (room == "cushion") {
            paint.color = Color.rgb(135, 154, 143)
            paint.isAntiAlias = true
            paint.color = Color.argb(65, 35, 47, 48)
            canvas.drawOval(133f, 247f, 227f, 261f, paint)
            paint.isAntiAlias = false
        } else if (room == "plant") {
            paint.color = Color.rgb(165, 133, 103)
            canvas.drawRect(282f, 215f, 306f, 244f, paint)
            paint.color = Color.rgb(117, 145, 101)
            canvas.drawRect(288f, 185f, 300f, 218f, paint)
            canvas.drawRect(276f, 193f, 310f, 205f, paint)
        }
        val moving = direction != null
        val cell = PixelSpriteFrames.cell(character, moving, walking, run, motion, progress, level)
        val column = cell % 4
        val row = cell / 4
        val rows = PetAppearance.spriteRows(character, level)
        val source = Rect(column * sheet.width / 4, row * sheet.height / rows,
            (column + 1) * sheet.width / 4, (row + 1) * sheet.height / rows)
        val rise = if (moving) 0f else if (idle < .5f) 0f else 1f
        canvas.save()
        canvas.translate(0f, -gesture.jump - rise)
        canvas.rotate(gesture.headTilt + gesture.rotation, 180f, 235f)
        if (direction == PetRunDirection.LEFT || direction == PetRunDirection.FRONT_LEFT) canvas.scale(-1f, 1f, 180f, 150f)
        paint.color = Color.WHITE
        // Leave headroom for the airborne sprite plus the shared jump displacement.
        canvas.drawBitmap(sheet, source, RectF(66f, 30f, 294f, 258f), paint)
        canvas.restore()
    }
}

/** Cell selection is shared by real reactions and parked previews. */
internal object PixelSpriteFrames {
    fun cell(character: PetCharacter, moving: Boolean, walking: Boolean, phase: Float,
        motion: PetMotion?, progress: Float, level: Int = 1): Int {
        val frame = ((phase * 4).toInt() % 4).coerceIn(0, 3)
        if (PetAppearance.spriteRows(character, level) == 2) return if (moving) 4 + frame else when (motion) {
            PetMotion.CELEBRATE -> 3
            PetMotion.WAVE -> 2
            PetMotion.SMILE -> 1
            else -> 0
        }
        if (moving) return (if (walking) 8 else 12) + frame
        if (progress <= 0f || progress >= 1f) return 0
        return when (motion) {
            PetMotion.SMILE -> 1
            PetMotion.WAVE -> 2
            PetMotion.CURIOUS -> 3
            PetMotion.JUMP -> when {
                progress < .12f -> 4
                progress < .88f -> 5
                else -> 6
            }
            PetMotion.CELEBRATE -> 7
            null -> 0
        }
    }
}
