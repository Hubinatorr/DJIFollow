package com.riis.kotlin_simulatordemo

import kotlin.math.cos
import kotlin.math.sin

class PID {
    private var Kp = 1.5
    private var Kd = 1.5
    private var Ki = 0.5

    private var eX = 0.0
    private var eY = 0.0
    private var eZ = 0.0

    private var veX = 0.0
    private var veY = 0.0
    private var veZ = 0.0

    private var Ix = 0.0
    private var Iy = 0.0
    private var Iz = 0.0

    var Vx = 0.0
    var Vy = 0.0
    var Vz = 0.0

    private var mLatitudeOffset = 0.0
    private var mLongitudeOffset = 3.0
    private var mAltitudeOffset = 0.0

    private var prevTimestamp: Long = 0

    fun compute(drone: DroneData, target: DroneData) {
        val deltaT = 0.2

        eX = target.x + mLatitudeOffset - drone.x
        eY = target.y + mLongitudeOffset - drone.y
        eZ = target.z + mAltitudeOffset - drone.Altitude

        veX = target.velocityX - drone.velocityX
        veY = target.velocityY - drone.velocityY
        veZ = target.velocityZ - drone.velocityZ

        val Px = Kp * eX
        val Py = Kp * eY
        val Pz = Kp * eZ

        val Dx = Kd * veX
        val Dy = Kd * veY
        val Dz = Kd * veZ

        if (prevTimestamp != 0L) {
            Ix += eX * deltaT * Ki
            Iy += eY * deltaT * Ki
            Iz += eZ * deltaT * Ki
        }

        Vx = Px + Ix + Dx
        Vy = Py + Iy + Dy
        Vz = Pz + Iz + Dz

        prevTimestamp = System.currentTimeMillis()
    }

    fun computeWithCommand(drone: DroneData, target: DroneData) {
        var targetHeading = Angle(target.Yaw).value
        var targetCommandSpeedX = ((target.RightV/660.0)*10)
        var targetCommandSpeedY = ((target.RightH/660.0)*10)

        var xX = targetCommandSpeedX * cos(Math.toRadians(targetHeading))
        var yX = targetCommandSpeedX * sin(Math.toRadians(targetHeading))

        var xY = targetCommandSpeedY * cos(Math.toRadians(targetHeading + 90))
        var yY = targetCommandSpeedY * sin(Math.toRadians(targetHeading + 90))

        var targetCommandX = xX + xY
        var targetCommandY = yX + yY
    }
}