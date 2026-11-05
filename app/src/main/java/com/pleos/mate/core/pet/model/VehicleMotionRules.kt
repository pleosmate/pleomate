package com.pleos.mate.core.pet.model

internal object VehicleMotionRules {
    fun isMoving(speedMetersPerSecond: Float, currentlyMoving: Boolean): Boolean {
        if (!speedMetersPerSecond.isFinite()) return false
        return speedMetersPerSecond >= if (currentlyMoving) 0.25f else 0.5f
    }
}
