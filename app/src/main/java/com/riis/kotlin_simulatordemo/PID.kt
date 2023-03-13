package com.riis.kotlin_simulatordemo

import kotlin.math.cos
import kotlin.math.sin

class PID {
    // Gains
    private var Kp = 2.0

    private var Kd = 1.47

    private var Ki = 0.0

    // Position Error
    private var eX = 0.0
    private var eY = 0.0
    private var eZ = 0.0

    // Velocity Error
    private var veX = 0.0
    private var veY = 0.0
    private var veZ = 0.0

    // Internal Term
    private var iX = 0.0
    private var iY = 0.0
    private var iZ = 0.0

    // Previous Position Error
    private var eXprev = 0.0
    private var eYprev = 0.0
    private var eZprev = 0.0

    // Output Velocity
    var Vx = 0.0
    var Vy = 0.0
    var Vz = 0.0

    private var mLatitudeOffset = 0.0
    private var mLongitudeOffset = 0.0
    private var mAltitudeOffset = 0.0

    private var prevTimestamp: Long = 0

    private var learningRate = 0.1

    fun compute(drone: DroneData, target: DroneData) {
        val currentTimestamp = System.currentTimeMillis()
        val deltaT = (currentTimestamp - prevTimestamp) / 1000.0

        eX = target.x - drone.x + mLatitudeOffset
        eY = target.y - drone.y + mLongitudeOffset
        eZ = target.z - drone.z + mAltitudeOffset

        veX = target.vX - drone.vX
        veY = target.vY - drone.vY
        veZ = target.vZ - drone.vZ

        if (prevTimestamp != 0L) {
            iX += eX * deltaT
            iY += eY * deltaT
            iZ += eZ * deltaT
        }

        Vx = Kp * eX /**/ + Ki * iX /**/ + Kd * veX
        Vy = Kp * eY /**/ + Ki * iY /**/ + Kd * veY
        Vz = Kp * eZ /**/ + Ki * iZ /**/ + Kd * veZ

        val saturationX = (Vx > 10.0 || Vx < -10.0)
        val saturationY = (Vy > 10.0 || Vy < -10.0)
        val saturationZ = (Vz > 10.0 || Vz < -10.0)

        val signX =((Vx * eX) > 0)
        val signY =((Vy * eY) > 0)
        val signZ =((Vz * eZ) > 0)

        iX = if (saturationX && signX) 0.0 else iX
        iY = if (saturationY && signY) 0.0 else iY
        iZ = if (saturationZ && signZ) 0.0 else iZ

        eXprev = eX
        eYprev = eY
        eZprev = eZ

        prevTimestamp = currentTimestamp
    }

    fun computeWithCommand(drone: DroneData, target: DroneData) {
        var targetHeading = Angle(target.yaw).value
        var targetCommandSpeedX = ((target.rv/660.0)*10)
        var targetCommandSpeedY = ((target.rh/660.0)*10)

        var xX = targetCommandSpeedX * cos(Math.toRadians(targetHeading))
        var yX = targetCommandSpeedX * sin(Math.toRadians(targetHeading))

        var xY = targetCommandSpeedY * cos(Math.toRadians(targetHeading + 90))
        var yY = targetCommandSpeedY * sin(Math.toRadians(targetHeading + 90))

        var targetCommandX = xX + xY
        var targetCommandY = yX + yY
    }

    private var Kpx = 2.0
    private var Kpy = 2.0
    private var Kpz = 2.0

    private var Kdx = 1.0
    private var Kdy = 1.0
    private var Kdz = 1.0

    private var Kix = 0.0
    private var Kiy = 0.0
    private var Kiz = 0.0

    private lateinit var prevDrone: DroneData

    fun gradientDescendK(drone: DroneData) {

        val ux = (Kpx * eX + Kix * iX + Kdx * veX - Vx)
        val uy = (Kpy * eY + Kiy * iY + Kdy * veY - Vy)
        val uz = (Kpz * eZ + Kiz * iZ + Kdz * veZ - Vz)

        if (ux != 0.0) {
            val diffX = drone.x - prevDrone.x
            Kpx -= learningRate * -eX * (diffX / ux) * eX
            Kdx -= learningRate * -eX * (diffX / ux) * veX
            Kix -= learningRate * -eX * (diffX / ux) * iX
        }

        if (uy != 0.0) {
            val diffY = drone.y - prevDrone.y
            Kpy -= learningRate * -eY * (diffY / uy) * eY
            Kdy -= learningRate * -eY * (diffY / uy) * veY
            Kiy -= learningRate * -eY * (diffY / uy) * iY
        }

        if (uz != 0.0) {
            val diffZ = drone.z - prevDrone.z
            Kpz -= learningRate * -eZ * (diffZ / uz) * eZ
            Kdz -= learningRate * -eZ * (diffZ / uz) * veZ
            Kiz -= learningRate * -eZ * (diffZ / uz) * iZ
        }
    }
}