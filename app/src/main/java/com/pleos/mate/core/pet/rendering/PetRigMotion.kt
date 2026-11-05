package com.pleos.mate.core.pet.rendering

import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionFrame
import com.pleos.mate.core.pet.model.PetRunDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal data class PetRigFoot(val travel: Float = 0f, val lift: Float = 0f)
internal data class PetRigArm(val angle: Float, val lengthScale: Float = 1f)

internal data class PetRigPose(
    val rise: Float = 0f,
    val leftArm: Float = 14f,
    val rightArm: Float = -14f,
    val leftFoot: PetRigFoot = PetRigFoot(),
    val rightFoot: PetRigFoot = PetRigFoot()
)

/** A single attached pair of arms/feet; no pose images are swapped during a stride. */
internal object PetRigMotion {
    fun arm(pose: PetRigPose, left: Boolean, view: PetRunDirection, running: Boolean): PetRigArm {
        val angle = if (left) pose.leftArm else pose.rightArm
        if (!running) return PetRigArm(angle)
        val foot = if (left) pose.leftFoot else pose.rightFoot
        val side = view == PetRunDirection.LEFT || view == PetRunDirection.RIGHT
        if (side) return PetRigArm(angle)
        val diagonal = view == PetRunDirection.FRONT_LEFT || view == PetRunDirection.FRONT_RIGHT
        val base = if (left) 14f else -14f
        val projection = if (diagonal) 0.7f else if (left) 0.28f else -0.28f
        return PetRigArm(base + (angle - base) * projection,
            1f - foot.travel * if (diagonal) 0.05f else 0.08f)
    }

    fun pose(phase: Float, running: Boolean, gesture: PetMotionFrame, motion: PetMotion?): PetRigPose {
        if (!running) {
            val wave = gesture.pawLift * 72f + gesture.pawSide * 8f
            return PetRigPose(leftArm = 14f + wave,
                rightArm = -14f - if (motion == PetMotion.CELEBRATE) wave else 0f)
        }
        val cycle = phase - floor(phase)
        val swing = sin(cycle * 2 * PI).toFloat() * 12f
        return PetRigPose(
            rise = (1f - cos(cycle * 4 * PI).toFloat()) * 1.2f,
            leftArm = 14f + swing,
            rightArm = -14f - swing,
            leftFoot = foot(cycle),
            rightFoot = foot((cycle + 0.5f) % 1f)
        )
    }

    private fun foot(phase: Float): PetRigFoot {
        val stance = 0.48f
        return if (phase < stance) {
            val t = phase / stance
            val eased = t * t * (3f - 2f * t)
            PetRigFoot(travel = 1f - 2f * eased)
        } else {
            val t = (phase - stance) / (1f - stance)
            PetRigFoot(travel = -cos(t * PI).toFloat(), lift = sin(t * PI).toFloat().coerceAtLeast(0f) * 8f)
        }
    }

    fun blink(idlePhase: Float): Float {
        if (idlePhase !in 0.68f..0.72f) return 0f
        val closing = sin((idlePhase - 0.68f) / 0.04f * PI).toFloat()
        return closing * closing
    }
}
