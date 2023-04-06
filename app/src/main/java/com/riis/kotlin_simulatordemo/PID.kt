package com.riis.kotlin_simulatordemo

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

class PID {
    // Gains
    var Kp = 1.5

    var Kd = 0.5

    var Ki = 0.0

    private var Kpx = 2.0
    private var Kpy = 2.0
    private var Kpz = 2.0

    private var Kdx = 1.47
    private var Kdy = 1.47
    private var Kdz = 1.47

    private var Kix = 0.2
    private var Kiy = 0.2
    private var Kiz = 0.2

    // Position Error
    private var eX = 0.0
    private var eY = 0.0
    private var eZ = 0.0

    private var eXfull = 0.0
    private var eYfull = 0.0
    private var eZfull = 0.0


    // Velocity Error
    private var veX = 0.0
    private var veY = 0.0
    private var veZ = 0.0

    private var veXfull = 0.0
    private var veYfull = 0.0
    private var veZfull = 0.0

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

    var offsetX = 0.0
    var offsetY = 0.0
    var offsetZ = 0.0

    private var prevTimestamp: Long = 0

    private var i = 0
    fun compute(drone: DroneData, target: DroneData) {
        val currentTimestamp = System.currentTimeMillis()
        eX = target.x - (drone.x + offsetX)
        eY = target.y - (drone.y + offsetY)
        eZ = target.z - (drone.z + offsetZ)

        veX = target.vX - drone.vX
        veY = target.vY - drone.vY
        veZ = target.vZ - drone.vZ

        if (i > 1) {
            val deltaT = (currentTimestamp - prevTimestamp) / 1000.0
            iX += ((eX + eXprev)/2) * deltaT
            iY += ((eY + eYprev)/2) * deltaT
            iZ += ((eZ + eZprev)/2) * deltaT
        }

        Vx = Kp * eX /**/ + Ki * iX /**/ + Kd * veX
        Vy = Kp * eY /**/ + Ki * iY /**/ + Kd * veY
        Vz = Kp * eZ /**/ + Ki * iZ /**/ + Kd * veZ

        val saturationX = (Vx > 15.0 || Vx < -15.0)
        val saturationY = (Vy > 15.0 || Vy < -15.0)
        val saturationZ = (Vz > 15.0 || Vz < -15.0)

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
        i++
    }

//    fun tuneGains() {
//        val gpX = ((eXfull + eX.pow(2)) / i - (eXfull / (i - 1))) / 0.01
//        val gpY = ((eYfull + eY.pow(2)) / i - (eYfull / (i - 1))) / 0.01
//        val gpZ = ((eZfull + eZ.pow(2)) / i - (eZfull / (i - 1))) / 0.01
//
//        val gdX = ((veXfull + veX.pow(2)) / i - (veXfull / (i - 1))) / 0.01
//        val gdY = ((veYfull + veY.pow(2)) / i - (veYfull / (i - 1))) / 0.01
//        val gdZ = ((veZfull + veZ.pow(2)) / i - (veZfull / (i - 1))) / 0.01
//
//        eXfull += eX.pow(2)
//        eYfull += eY.pow(2)
//        eZfull += eZ.pow(2)
//
//        veXfull += veXfull.pow(2)
//        veYfull += veYfull.pow(2)
//        veZfull += veZfull.pow(2)
//
//        Kpx = max(Kpx - (gamma * gpX), 0.0)
//        Kpy = max(Kpy - (gamma * gpY), 0.0)
//        Kpz = max(Kpz - (gamma * gpZ), 0.0)
//
//        Kdx = max(Kdx - (gamma * gdX), 0.0)
//        Kdy = max(Kdy - (gamma * gdY), 0.0)
//        Kdz = max(Kdz - (gamma * gdZ), 0.0)
//    }

    var targetCommandX = 0.0
    var targetCommandY = 0.0

    fun computeWithCommand(drone: DroneData, target: DroneData) {
        var targetHeading = Angle(target.yaw).value
        var targetCommandSpeedX = ((target.rv/660.0)*15)
        var targetCommandSpeedY = ((target.rh/660.0)*15)

        var xX = targetCommandSpeedX * cos(Math.toRadians(targetHeading))
        var yX = targetCommandSpeedX * sin(Math.toRadians(targetHeading))

        var xY = targetCommandSpeedY * cos(Math.toRadians(targetHeading + 90))
        var yY = targetCommandSpeedY * sin(Math.toRadians(targetHeading + 90))

        targetCommandX = xX + xY
        targetCommandY = yX + yY
    }

    private lateinit var prevDrone: DroneData
}