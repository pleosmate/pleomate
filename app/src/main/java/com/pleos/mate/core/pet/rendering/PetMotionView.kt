package com.pleos.mate.core.pet.rendering

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.model.PetRunDirection

/** Fixed plush body parts animated by an attached two-arm/two-foot rig. */
class PetMotionView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val renderer = PetRigRenderer(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparkle = Path().apply {
        moveTo(0f, -7f); lineTo(2f, -2f); lineTo(7f, 0f); lineTo(2f, 2f)
        lineTo(0f, 7f); lineTo(-2f, 2f); lineTo(-7f, 0f); lineTo(-2f, -2f); close()
    }
    private val sparklePositions = floatArrayOf(65f, 128f, 300f, 98f, 286f, 195f)
    private var ready = false
    private var cycle = 0f
    private var runPhase = 0f
    private var motionProgress = 0f
    private var motion: PetMotion? = null
    private var greetingCount = 0
    private var hostActive = false
    private var restricted = false

    var outfit: String = "none"
        set(value) { if (field != value) { field = value; invalidate() } }
    var room: String = "none"
        set(value) { if (field != value) { field = value; invalidate() } }

    var walking: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            runAnimator.cancel()
            runAnimator.duration = if (value) 1100L else if (character.isPixel) 640L else if (character == PetCharacter.BEAR) 960L else 820L
            runPhase = 0f
            updateMotion()
        }

    var runningDirection: PetRunDirection? = null
        set(value) {
            if (field == value) return
            field = value
            motionAnimator.cancel()
            motion = null
            motionProgress = 0f
            updateMotion()
            invalidate()
        }

    var character: PetCharacter = PetCharacter.BEAR
        set(value) {
            if (field == value) return
            field = value
            motionAnimator.cancel()
            motion = null
            motionProgress = 0f
            greetingCount = 0
            runAnimator.cancel()
            runAnimator.duration = if (walking) 1100L else if (value.isPixel) 640L else if (value == PetCharacter.BEAR) 960L else 820L
            runPhase = 0f
            updateMotion()
            invalidate()
        }

    var level: Int = 1
        set(value) {
            field = value.coerceAtLeast(1)
            if (motion?.let { !PetMotionRules.canPlay(field, it) } == true) {
                motionAnimator.cancel()
                motion = null
                motionProgress = 0f
                invalidate()
            }
        }

    private val idleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4400
        repeatCount = ValueAnimator.INFINITE
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { cycle = it.animatedValue as Float; invalidate() }
    }
    private val motionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { motionProgress = it.animatedValue as Float; invalidate() }
    }
    private val runAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 960L
        repeatCount = ValueAnimator.INFINITE
        interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { runPhase = it.animatedValue as Float; invalidate() }
    }

    init { ready = true }

    fun setHostActive(value: Boolean) {
        hostActive = value
        updateMotion()
    }

    fun setRestricted(value: Boolean) {
        restricted = value
        updateMotion()
    }

    fun greet() {
        if (canAnimate()) {
            greetingCount++
            playMotion(PetMotionRules.greeting(level, greetingCount))
        }
    }

    fun celebrate(): Boolean = playMotion(PetMotionRules.celebration(level))

    fun playMotion(value: PetMotion): Boolean {
        if (!canAnimate() || runningDirection != null || !PetMotionRules.canPlay(level, value)) return false
        motionAnimator.cancel()
        motion = value
        motionProgress = 0f
        motionAnimator.duration = PetMotionRules.duration(character, value)
        motionAnimator.start()
        return true
    }

    private fun canAnimate() = hostActive && !restricted && isAttachedToWindow &&
        visibility == VISIBLE && windowVisibility == VISIBLE && ValueAnimator.areAnimatorsEnabled()

    private fun updateMotion() {
        if (!ready) return
        if (canAnimate()) {
            if (runningDirection != null) {
                idleAnimator.cancel()
                cycle = 0f
                if (!runAnimator.isStarted) runAnimator.start()
            } else {
                runAnimator.cancel()
                runPhase = 0f
                if (!idleAnimator.isStarted) idleAnimator.start()
            }
        } else {
            idleAnimator.cancel()
            runAnimator.cancel()
            motionAnimator.cancel()
            motion = null
            motionProgress = 0f
            cycle = 0f
            runPhase = 0f
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMotion()
    }

    override fun onDetachedFromWindow() {
        idleAnimator.cancel()
        runAnimator.cancel()
        motionAnimator.cancel()
        motion = null
        motionProgress = 0f
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateMotion()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateMotion()
    }

    var showRoomBackground: Boolean = false
        set(value) {
            if (field != value) { field = value; invalidate() }
        }
    private val roomBackground = PetRoomBackground(context)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showRoomBackground) roomBackground.draw(canvas, character, width, height)
        val scale = minOf(width / 360f, height / 300f)
        canvas.save()
        val stageTop = if (showRoomBackground) height * .82f - 258f * scale
            else (height - 300f * scale) / 2
        canvas.translate((width - 360f * scale) / 2, stageTop)
        canvas.scale(scale, scale)
        val frame = PetMotionRules.frame(motion, motionProgress, character)
        renderer.draw(canvas, character, runningDirection, cycle, runPhase, frame, motion, level, outfit, room, walking, motionProgress)
        if (frame.sparkles > 0f) {
            paint.color = Color.rgb(235, 219, 168)
            paint.alpha = (frame.sparkles * 220).toInt()
            for (index in sparklePositions.indices step 2) {
                canvas.save()
                canvas.translate(sparklePositions[index], sparklePositions[index + 1] - frame.sparkles * 12f)
                canvas.scale(frame.sparkles, frame.sparkles)
                canvas.drawPath(sparkle, paint)
                canvas.restore()
            }
            paint.alpha = 255
        }
        canvas.restore()
    }
}
