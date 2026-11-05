package com.pleos.mate.core.pet.rendering

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.pleos.mate.core.pet.model.PetCharacter

/** Full-bleed room artwork; pet animation remains an independent foreground layer. */
internal class PetRoomBackground(context: Context) {
    private val atlas by lazy {
        context.assets.open("pets/rooms/rooms-v1.png").use { requireNotNull(BitmapFactory.decodeStream(it)) }
    }
    private val roverRoom by lazy {
        context.assets.open("pets/rooms/rover-room-v2.png").use { requireNotNull(BitmapFactory.decodeStream(it)) }
    }
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val subdued = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(.75f) })
    fun draw(canvas: Canvas, character: PetCharacter, width: Int, height: Int) {
        val index = when (character) {
            PetCharacter.BEAR -> 0
            PetCharacter.RABBIT -> 1
            PetCharacter.PIXEL -> 2
            PetCharacter.CLOUD -> 3
            PetCharacter.SEED -> 4
            PetCharacter.ROVER -> 5
        }
        val bitmap = if (character == PetCharacter.ROVER) roverRoom else atlas
        val cellWidth = if (character == PetCharacter.ROVER) bitmap.width else atlas.width / 2
        val rows = intArrayOf(0, atlas.height * 432 / 1536, atlas.height * 960 / 1536, atlas.height)
        val top = if (character == PetCharacter.ROVER) 0 else rows[index / 2]
        val bottom = if (character == PetCharacter.ROVER) bitmap.height else rows[index / 2 + 1]
        val cellHeight = bottom - top
        val left = if (character == PetCharacter.ROVER) 0 else index % 2 * cellWidth
        val source = Rect(left, top, left + cellWidth, bottom)
        // Center crop rather than stretch the room's furniture and perspective.
        val targetAspect = width.toFloat() / height.coerceAtLeast(1)
        if (cellWidth.toFloat() / cellHeight > targetAspect) {
            val crop = (cellHeight * targetAspect).toInt().coerceAtLeast(1)
            source.inset((cellWidth - crop) / 2, 0)
        } else {
            val crop = (cellWidth / targetAspect).toInt().coerceAtLeast(1)
            source.inset(0, (cellHeight - crop) / 2)
        }
        paint.colorFilter = if (character == PetCharacter.ROVER) null else subdued
        paint.isFilterBitmap = !character.isPixel
        canvas.drawBitmap(bitmap, source, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
    }
}
