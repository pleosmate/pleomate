package com.pleos.mate

import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionFrame
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.model.VehicleMotionRules
import com.pleos.mate.core.pet.rendering.PetRigMotion
import com.pleos.mate.core.pet.rendering.PetRigPose
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class PetRigMotionTest {
    private fun run(phase: Float) = PetRigMotion.pose(phase, true, PetMotionFrame(), null)

    @Test fun vehicleMotionUsesSpeedThresholdWithStopHysteresis() {
        assertFalse(VehicleMotionRules.isMoving(0.49f, false))
        assertTrue(VehicleMotionRules.isMoving(0.5f, false))
        assertTrue(VehicleMotionRules.isMoving(0.3f, true))
        assertFalse(VehicleMotionRules.isMoving(0.24f, true))
        assertFalse(VehicleMotionRules.isMoving(Float.NaN, true))
    }

    @Test fun gaitLoopsWithoutPositionJumpAndKeepsFeetWithinReach() {
        assertEquals(run(0f), run(1f))
        var previous = run(0f)
        for (sample in 1..1000) {
            val pose = run(sample / 1000f)
            for (foot in listOf(pose.leftFoot, pose.rightFoot)) {
                assertTrue(foot.travel in -1f..1f)
                assertTrue(foot.lift in 0f..8.001f)
            }
            assertTrue(pose.rise in 0f..2.401f)
            assertTrue(abs(pose.leftFoot.travel - previous.leftFoot.travel) < 0.02f)
            assertTrue(abs(pose.leftFoot.lift - previous.leftFoot.lift) < 0.1f)
            assertTrue(abs(pose.leftArm - previous.leftArm) < 0.2f)
            previous = pose
        }
    }

    @Test fun feetAlternateContactAndArmsSwingInOpposition() {
        assertEquals(0f, run(0.25f).leftFoot.lift, 0f)
        assertTrue(run(0.25f).rightFoot.lift > 7f)
        assertEquals(0f, run(0.75f).rightFoot.lift, 0f)
        assertTrue(run(0.75f).leftFoot.lift > 7f)
        for (sample in 0..100) {
            val pose = run(sample / 100f)
            assertEquals(0f, pose.leftArm + pose.rightArm, 0.001f)
        }
    }

    @Test fun restingAndFinishedGesturesReturnToAttachedNeutralPose() {
        for (motion in PetMotion.entries) {
            assertEquals(PetRigPose(), PetRigMotion.pose(0f, false, PetMotionRules.frame(motion, 0f), motion))
            assertEquals(PetRigPose(), PetRigMotion.pose(0f, false, PetMotionRules.frame(motion, 1f), motion))
        }
        val wave = PetRigMotion.pose(0f, false, PetMotionRules.frame(PetMotion.WAVE, 0.5f), PetMotion.WAVE)
        assertTrue(wave.leftArm > 50f)
        assertEquals(-14f, wave.rightArm, 0f)
    }

    @Test fun frontViewArmDepthOpposesTheSameSideFootInsteadOfFlappingBothArms() {
        val pose = run(0.25f)
        val left = PetRigMotion.arm(pose, true, PetRunDirection.FRONT, true)
        val right = PetRigMotion.arm(pose, false, PetRunDirection.FRONT, true)
        assertTrue(left.lengthScale > 1f)
        assertTrue(right.lengthScale < 1f)
        for (direction in PetRunDirection.entries) {
            for (sample in 0..100) {
                val p = run(sample / 100f)
                for (side in listOf(true, false)) {
                    val arm = PetRigMotion.arm(p, side, direction, true)
                    assertTrue(arm.lengthScale in 0.919f..1.081f)
                    assertTrue(arm.angle.isFinite())
                }
            }
        }
    }

    @Test fun blinkIsShortAndSmoothWithNoFullBodyPoseSwitch() {
        assertEquals(0f, PetRigMotion.blink(0.5f), 0f)
        assertEquals(0f, PetRigMotion.blink(0.68f), 0.0001f)
        assertEquals(1f, PetRigMotion.blink(0.70f), 0.0001f)
        assertEquals(0f, PetRigMotion.blink(0.72f), 0.0001f)
    }
}
