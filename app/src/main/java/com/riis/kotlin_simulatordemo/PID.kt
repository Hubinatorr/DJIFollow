package com.riis.kotlin_simulatordemo

import android.util.Log
import dji.sdksharedlib.nhf.gfd.fdd.Ma
import kotlin.math.cos
import kotlin.math.sin

class PID {
    // Gains
    private var Kpx = 1.5
    private var Kpy = 1.5
    private var Kpz = 1.5

    private var Kdx = 1.5
    private var Kdy = 1.5
    private var Kdz = 1.5

    private var Kix = 0.0
    private var Kiy = 0.0
    private var Kiz = 0.0

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
    private var mLongitudeOffset = 3.0
    private var mAltitudeOffset = 0.0

    private var prevTimestamp: Long = 0

    private lateinit var prevDrone: DroneData

    private var learningRate = 0.1



    fun compute(drone: DroneData, target: DroneData) {
        val currentTimestamp = System.currentTimeMillis()
        val deltaT = (currentTimestamp - prevTimestamp) / 1000.0

        eX = target.x + mLatitudeOffset - drone.x
        eY = target.y + mLongitudeOffset - drone.y
        eZ = target.z + mAltitudeOffset - drone.Altitude

        veX = target.velocityX - drone.velocityX
        veY = target.velocityY - drone.velocityY
        veZ = target.velocityZ - drone.velocityZ
        if (prevTimestamp != 0L) {
            iX += ((eXprev + eX)/2 * deltaT)
            iY += ((eYprev + eY)/2 * deltaT)
            iZ += ((eZprev + eZ)/2 * deltaT)
        }

        Vx = Kpx * eX /**/ + Kix * iX /**/ + Kdx * veX
        Vy = Kpy * eY /**/ + Kiy * iY /**/ + Kdy * veY
        Vz = Kpz * eZ /**/ + Kiz * iZ /**/ + Kdz * veZ

        eXprev = eX
        eYprev = eY
        eZprev = eZ

        prevTimestamp = currentTimestamp
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