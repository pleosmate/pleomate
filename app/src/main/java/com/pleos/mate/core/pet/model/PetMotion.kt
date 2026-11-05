package com.pleos.mate.core.pet.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

enum class PetMotion(val unlockLevel: Int, val label: String) {
    SMILE(1, "기본 웃음"),
    WAVE(2, "손 흔들기"),
    CURIOUS(3, "고개 갸웃하기"),
    JUMP(4, "기쁜 점프"),
    CELEBRATE(5, "특별 축하")
}

internal data class PetMotionFrame(
    val smile: Float = 0f,
    val headTilt: Float = 0f,
    val pawLift: Float = 0f,
    val pawSide: Float = 0f,
    val jump: Float = 0f,
    val rotation: Float = 0f,
    val sparkles: Float = 0f,
    val earSway: Float = 0f,
    val squash: Float = 0f
)

internal object PetMotionRules {
    fun unlocked(level: Int) = PetMotion.entries.filter { it != PetMotion.SMILE && level >= it.unlockLevel }
    fun canPlay(level: Int, motion: PetMotion) = level >= motion.unlockLevel
    fun newlyUnlocked(previousLevel: Int, level: Int) = unlocked(level).filter { it.unlockLevel > previousLevel }
    fun greeting(level: Int, count: Int) = when {
        level >= PetMotion.CURIOUS.unlockLevel && count % 2 == 0 -> PetMotion.CURIOUS
        level >= PetMotion.WAVE.unlockLevel -> PetMotion.WAVE
        else -> PetMotion.SMILE
    }
    fun celebration(level: Int) = when {
        level >= PetMotion.CELEBRATE.unlockLevel -> PetMotion.CELEBRATE
        level >= PetMotion.JUMP.unlockLevel -> PetMotion.JUMP
        else -> PetMotion.SMILE
    }
    fun levelUpMessage(previousLevel: Int, level: Int): String {
        val names = newlyUnlocked(previousLevel, level).joinToString(" · ") { it.label }
        return if (names.isEmpty()) "Lv.${level}로 성장했어요. 함께한 경험이 계속 쌓이고 있어요."
        else "Lv.${level}로 성장했어요!\n새 모션: $names"
    }
    fun duration(character: PetCharacter, motion: PetMotion): Long = when (character) {
        PetCharacter.BEAR -> when (motion) {
            PetMotion.SMILE -> 1400L
            PetMotion.WAVE -> 2200L
            PetMotion.CURIOUS -> 2400L
            PetMotion.JUMP -> 1900L
            PetMotion.CELEBRATE -> 3000L
        }
        else -> when (motion) {
            PetMotion.SMILE -> 1000L
            PetMotion.WAVE -> 1400L
            PetMotion.CURIOUS -> 1700L
            PetMotion.JUMP -> 1350L
            PetMotion.CELEBRATE -> 2300L
        }
    }

    fun frame(motion: PetMotion?, progress: Float, character: PetCharacter = PetCharacter.BEAR): PetMotionFrame {
        if (motion == null || progress <= 0f || progress >= 1f) return PetMotionFrame()
        val t = progress.toDouble()
        val envelope = sin(t * PI).toFloat()
        val bear = character == PetCharacter.BEAR
        val wave = sin(t * (if (bear) 4 else 8) * PI).toFloat()
        val jumpHeight = if (bear) 14f else 22f
        val frame = when (motion) {
            PetMotion.SMILE -> PetMotionFrame(smile = envelope)
            PetMotion.WAVE -> PetMotionFrame(smile = envelope * 0.65f,
                pawLift = envelope * (0.75f + 0.25f * wave),
                pawSide = envelope * wave * if (bear) 0.7f else 1f)
            PetMotion.CURIOUS -> PetMotionFrame(headTilt = (if (bear) 8f else 12f) * envelope * sin(t * 2 * PI).toFloat())
            PetMotion.JUMP -> {
                val takeoff = if (bear) 0.12 else 0.08
                val landing = if (bear) 0.84 else 0.9
                val flight = ((t - takeoff) / (landing - takeoff)).coerceIn(0.0, 1.0)
                val squash = when {
                    t < takeoff -> sin(t / takeoff * PI)
                    t > landing -> sin((t - landing) / (1 - landing) * PI)
                    else -> 0.0
                }.toFloat() * if (bear) 1f else 0.45f
                PetMotionFrame(smile = envelope, jump = jumpHeight * sin(flight * PI).pow(if (bear) 1.5 else 0.85).toFloat(), squash = squash)
            }
            PetMotion.CELEBRATE -> PetMotionFrame(smile = envelope, pawLift = envelope,
                pawSide = sin(t * (if (bear) 6 else 10) * PI).toFloat() * envelope * if (bear) 0.7f else 1f,
                jump = jumpHeight * abs(sin(t * 2 * PI)).toFloat() * envelope,
                rotation = (if (bear) 2f else 3f) * sin(t * 4 * PI).toFloat() * envelope, sparkles = envelope)
        }
        return if (!bear && motion != PetMotion.SMILE)
            frame.copy(earSway = envelope * sin(t * 4 * PI - PI / 3).toFloat())
        else frame
    }
}
