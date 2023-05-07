package com.riis.dji_follow

import android.util.Log

class PIDController(var Kp: Double, var Kd: Double, var Ki: Double) {
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

    // Output Velocity
    var Vx = 0.0
    var Vy = 0.0
    var Vz = 0.0

    var offsetX = 0.0
    var offsetY = 0.0
    var offsetZ = 0.0

    private var prevTimestamp = 0L
    fun compute(drone: DroneData, target: DroneData) : Triple<Double, Double, Double> {
        val currentTimestamp = System.currentTimeMillis()
        eX = target.x - (drone.x + offsetX)
        eY = target.y - (drone.y + offsetY)
        eZ = target.z - (drone.z + offsetZ)

        veX = target.vX - drone.vX
        veY = target.vY - drone.vY
        veZ = target.vZ - drone.vZ

        if (prevTimestamp > 0) {
            val dt = (currentTimestamp - prevTimestamp) / 1000.0
            iX += eX * dt
            iY += eY * dt
            iZ += eZ * dt
        }

        Vx = Kp * eX /**/ + Ki * iX /**/ + Kd * veX
        Vy = Kp * eY /**/ + Ki * iY /**/ + Kd * veY
        Vz = Kp * eZ /**/ + Ki * iZ /**/ + Kd * veZ

        Log.i(MainActivity.DEBUG, "${eX} ${eY}, ${eZ}")

        checkAntiWindup()
        prevTimestamp = currentTimestamp

        return Triple(Vx, Vy, Vz)
    }

    private fun checkAntiWindup()
    {
        val saturationX = (Vx > 10.0 || Vx < -10.0)
        val saturationY = (Vy > 10.0 || Vy < -10.0)
        val saturationZ = (Vz > 4.0 || Vz < -4.0)

        val signX =((Vx * eX) > 0)
        val signY =((Vy * eY) > 0)
        val signZ =((Vz * eZ) > 0)

        iX = if (saturationX && signX) 0.0 else iX
        iY = if (saturationY && signY) 0.0 else iY
        iZ = if (saturationZ && signZ) 0.0 else iZ
    }
}