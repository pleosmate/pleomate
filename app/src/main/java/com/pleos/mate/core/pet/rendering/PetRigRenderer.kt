package com.pleos.mate.core.pet.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.pleos.mate.core.pet.model.PetAppearance
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionFrame
import com.pleos.mate.core.pet.model.PetRunDirection
import kotlin.math.PI
import kotlin.math.sin
import org.json.JSONObject

/** Draws six fixed body views with the same two arm and two foot textures. */
internal class PetRigRenderer(private val context: Context) {
    private data class Atlas(val bitmap: Bitmap, val parts: List<Rect>, val eyeBand: FloatArray,
        val limbsBitmap: Bitmap, val limbs: List<Rect>)
    private val pixel = PixelPetRenderer(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val destination = RectF()
    private val bodyDestination = RectF()
    private val jointMask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    companion object {
        private val atlases = mutableMapOf<Pair<PetCharacter, Int>, Atlas>()
    }

    private fun atlas(character: PetCharacter, level: Int): Atlas = atlases.getOrPut(character to PetAppearance.stageLevel(level)) {
        val root = "pets/rig/${character.id}"
        val definition = context.assets.open("$root.json").bufferedReader().use { JSONObject(it.readText()) }
        val bounds = definition.getJSONArray("parts")
        val parts = (0 until bounds.length()).map { index ->
            val rect = bounds.getJSONArray(index)
            Rect(rect.getInt(0), rect.getInt(1), rect.getInt(2), rect.getInt(3))
        }
        val band = definition.getJSONArray("eyeBand")
        if (PetAppearance.stageLevel(level) > 1) {
            val bitmap = context.assets.open(PetAppearance.asset(character, level)).use { requireNotNull(BitmapFactory.decodeStream(it)) }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            // Locate the transparent row gutters so tall ears are not cut at an exact third.
            val rowEdges = listOf(0) + (1..2).map { row ->
                val center = bitmap.height * row / 3
                (center - bitmap.height / 12..center + bitmap.height / 12).minWith(
                    compareBy<Int> { y -> (0 until bitmap.width).count { x -> Color.alpha(pixels[y * bitmap.width + x]) > 128 } }
                        .thenBy { kotlin.math.abs(it - center) })
            } + listOf(bitmap.height)
            // Trim transparent margins to retain the attached rig's proportions.
            val cells = (0 until 9).map { index ->
                val cellLeft = index % 3 * bitmap.width / 3
                val cellTop = rowEdges[index / 3]
                val cellRight = (index % 3 + 1) * bitmap.width / 3
                val cellBottom = rowEdges[index / 3 + 1]
                var left = cellRight; var top = cellBottom; var right = cellLeft; var bottom = cellTop
                for (y in cellTop until cellBottom) for (x in cellLeft until cellRight) {
                    if (Color.alpha(pixels[y * bitmap.width + x]) > 128) {
                        left = minOf(left, x); top = minOf(top, y)
                        right = maxOf(right, x + 1); bottom = maxOf(bottom, y + 1)
                    }
                }
                require(right > left && bottom > top) { "Empty evolution atlas cell: ${character.id} $level $index" }
                Rect(left, top, right, bottom)
            }
            return@getOrPut Atlas(bitmap, cells.take(7), FloatArray(4) { band.getDouble(it).toFloat() }, bitmap, cells.takeLast(2))
        }
        val bitmap = context.assets.open("$root.png").use { requireNotNull(BitmapFactory.decodeStream(it)) }
        val limbsBounds = definition.getJSONArray("limbs")
        val limbs = (0 until limbsBounds.length()).map { index ->
            val rect = limbsBounds.getJSONArray(index)
            Rect(rect.getInt(0), rect.getInt(1), rect.getInt(2), rect.getInt(3))
        }
        val limbsBitmap = context.assets.open("pets/rig/${definition.getString("limbsImage")}").use {
            requireNotNull(BitmapFactory.decodeStream(it))
        }
        Atlas(bitmap, parts, FloatArray(4) { band.getDouble(it).toFloat() }, limbsBitmap, limbs)
    }

    fun draw(canvas: Canvas, character: PetCharacter, direction: PetRunDirection?, idlePhase: Float,
        runPhase: Float, gesture: PetMotionFrame, motion: PetMotion?, level: Int = 1, outfit: String = "none", room: String = "none", walking: Boolean = false, progress: Float = 0f) {
        if (character.isPixel) {
            pixel.draw(canvas, character, direction, idlePhase, runPhase, gesture, motion, room, walking, progress, level)
            return
        }
        val atlas = atlas(character, level)
        val running = direction != null
        val view = direction ?: PetRunDirection.FRONT
        val pose = PetRigMotion.pose(runPhase, running, gesture, motion)
        val bear = character == PetCharacter.BEAR
        val side = view == PetRunDirection.LEFT || view == PetRunDirection.RIGHT
        val diagonal = view == PetRunDirection.FRONT_LEFT || view == PetRunDirection.FRONT_RIGHT
        val leftFacing = view == PetRunDirection.LEFT || view == PetRunDirection.FRONT_LEFT
        val facing = if (leftFacing) -1f else 1f
        val jump = if (running) 0f else gesture.jump

        paint.shader = RadialGradient(180f, 251f, 66f,
            Color.argb((36f - jump * 0.6f).toInt(), 20, 25, 20), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val shadowWidth = 66f - jump * 0.55f
        canvas.drawOval(180f - shadowWidth, 243f, 180f + shadowWidth, 260f, paint)
        paint.shader = null
        paint.color = Color.WHITE
        paint.alpha = 255

        if (room == "cushion") {
            paint.color = Color.rgb(151, 138, 117)
            canvas.drawOval(91f, 237f, 269f, 267f, paint)
            paint.color = Color.rgb(191, 177, 153)
            canvas.drawOval(94f, 234f, 266f, 257f, paint)
        } else if (room == "plant") {
            paint.color = Color.rgb(181, 153, 128)
            canvas.drawRoundRect(281f, 212f, 308f, 250f, 6f, 6f, paint)
            paint.color = Color.rgb(120, 145, 103)
            canvas.drawOval(267f, 182f, 295f, 216f, paint)
            canvas.drawOval(290f, 169f, 314f, 206f, paint)
        }
        paint.color = Color.WHITE
        // Separate ground contact from the returning foot's smooth swing arc.
        canvas.save()
        canvas.translate(0f, -jump)
        for (index in 0..1) {
            val left = index == 0
            val foot = if (left) pose.leftFoot else pose.rightFoot
            val spread = if (side) 7f else if (diagonal) 20f else 26f
            val offset = if (left) -spread else spread
            val travelX = if (side) facing * foot.travel * 13f else if (diagonal) facing * foot.travel * 7f else 0f
            val depth = if (side) 0f else foot.travel * 2f
            val footWidth = if (bear) 34f else 31f
            val footHeight = if (bear) 29f else 28f
            val x = 180f + offset + travelX
            val bottom = 253f - foot.lift + depth
            canvas.save()
            if (side) canvas.rotate(-facing * foot.travel * foot.lift * 0.8f, x, bottom - footHeight / 2)
            destination.set(x - footWidth / 2, bottom - footHeight, x + footWidth / 2, bottom)
            canvas.drawBitmap(atlas.limbsBitmap, atlas.limbs[1], destination, paint)
            canvas.restore()
        }
        canvas.restore()

        val breath = if (running) 0f else sin(idlePhase * 2 * PI).toFloat()
        canvas.save()
        canvas.translate(0f, -jump - pose.rise)
        canvas.scale(1f + breath * 0.002f + gesture.squash * 0.025f,
            1f + breath * 0.005f - gesture.squash * 0.035f, 180f, 238f)
        canvas.rotate(gesture.headTilt * 0.38f + gesture.rotation + gesture.earSway * 0.6f +
            if (running && side) facing * 3f else 0f, 180f, 220f)

        val source = atlas.parts[view.ordinal]
        val bodyScale = (if (bear) 188f else 202f) / atlas.parts[0].height()
        val bodyWidth = source.width() * bodyScale
        val bodyHeight = source.height() * bodyScale
        bodyDestination.set(180f - bodyWidth / 2, 238f - bodyHeight, 180f + bodyWidth / 2, 238f)
        val shoulderSpread = if (side) 16f else if (diagonal) (if (bear) 53f else 46f)
            else if (view == PetRunDirection.BACK) (if (bear) 78f else 66f) else if (bear) 61f else 53f
        val shoulderY = if (bear) 191f else 196f
        val nearLeft = leftFacing

        fun arm(left: Boolean) {
            val arm = PetRigMotion.arm(pose, left, view, running)
            val x = 180f + if (left) -shoulderSpread else shoulderSpread
            val width = (if (bear) 31f else 28f) * (1f + (arm.lengthScale - 1f) * 0.45f)
            val length = (if (bear) 24f else 22f) * arm.lengthScale
            canvas.save()
            canvas.rotate(arm.angle, x, shoulderY)
            destination.set(x - width / 2, shoulderY - 6f, x + width / 2, shoulderY + length)
            val layer = canvas.saveLayer(destination, null)
            canvas.drawBitmap(atlas.limbsBitmap, atlas.limbs[0], destination, paint)
            // Blend the short shoulder root into the belly instead of showing a separate capsule.
            jointMask.shader = LinearGradient(0f, destination.top, 0f, destination.top + destination.height() * 0.22f,
                Color.TRANSPARENT, Color.WHITE, Shader.TileMode.CLAMP)
            canvas.drawRect(destination, jointMask)
            canvas.restoreToCount(layer)
            canvas.restore()
        }

        val front = view == PetRunDirection.FRONT
        val back = view == PetRunDirection.BACK
        if (!front) {
            if (back) { arm(true); arm(false) } else arm(!nearLeft)
        }
        val evolvedRabbit = character == PetCharacter.RABBIT && PetAppearance.stageLevel(level) == 15
        val blink = if (front) maxOf(PetRigMotion.blink(idlePhase), (gesture.smile * 3f).coerceAtMost(1f)) else 0f
        // This evolution has different face proportions in its smile cell. Select one whole
        // expression instead of alpha-blending eye patches over the open-eyed artwork.
        val bodySource = if (evolvedRabbit && blink >= .5f) atlas.parts[6] else source
        canvas.drawBitmap(atlas.bitmap, bodySource, bodyDestination, paint)
        if (front) {
            if (!evolvedRabbit && blink > 0f) {
                val band = atlas.eyeBand
                paint.alpha = (blink * 255).toInt()
                for (eye in 0..1) {
                    canvas.save()
                    val left = if (eye == 0) band[0] else 0.56f
                    val right = if (eye == 0) 0.44f else band[2]
                    canvas.clipRect(bodyDestination.left + bodyWidth * left, bodyDestination.top + bodyHeight * band[1],
                        bodyDestination.left + bodyWidth * right, bodyDestination.top + bodyHeight * band[3])
                    canvas.drawBitmap(atlas.bitmap, atlas.parts[6], bodyDestination, paint)
                    canvas.restore()
                }
                paint.alpha = 255
            }
            arm(true); arm(false)
        } else if (!back) arm(nearLeft)
        // Accessories follow the body's proportions and viewing direction.
        val neckY = bodyDestination.top + bodyHeight * (if (bear) .72f else .79f)
        val accessoryX = 180f + if (side || diagonal) facing * bodyWidth * .10f else 0f
        if (outfit == "breeze") {
            val halfWidth = bodyWidth * (if (side) .22f else .29f)
            paint.shader = LinearGradient(0f, neckY, 0f, neckY + 10f,
                Color.rgb(157, 183, 168), Color.rgb(87, 124, 115), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(accessoryX - halfWidth, neckY, accessoryX + halfWidth, neckY + 10f, 5f, 5f, paint)
            paint.shader = null
            if (!back) {
                paint.color = Color.rgb(137, 169, 158)
                val tail = android.graphics.Path().apply {
                    moveTo(accessoryX + halfWidth * .35f, neckY + 6f)
                    quadTo(accessoryX + halfWidth * .65f, neckY + 18f, accessoryX + halfWidth * .40f, neckY + 28f)
                    lineTo(accessoryX + halfWidth * .05f, neckY + 25f)
                    lineTo(accessoryX, neckY + 8f)
                    close()
                }
                canvas.drawPath(tail, paint)
            }
        } else if (outfit == "bow" && !back) {
            val bowWidth = if (side) 12f else 18f
            paint.shader = LinearGradient(0f, neckY - 2f, 0f, neckY + 10f,
                Color.rgb(205, 166, 165), Color.rgb(157, 112, 121), Shader.TileMode.CLAMP)
            canvas.drawOval(accessoryX - bowWidth, neckY - 2f, accessoryX + 1f, neckY + 10f, paint)
            canvas.drawOval(accessoryX - 1f, neckY - 2f, accessoryX + bowWidth, neckY + 10f, paint)
            paint.shader = null
            paint.color = Color.rgb(199, 155, 156)
            canvas.drawRoundRect(accessoryX - 4f, neckY, accessoryX + 4f, neckY + 8f, 3f, 3f, paint)
        }
        paint.color = Color.WHITE
        canvas.restore()
    }
}
